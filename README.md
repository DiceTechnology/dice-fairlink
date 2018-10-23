[![Build Status](https://travis-ci.com/DiceTechnology/dice-fairlink.svg?token=F6ktiNWbNbvGRbN5NmqA&branch=master)](https://travis-ci.com/DiceTechnology/dice-fairlink)[ ![Download](https://api.bintray.com/packages/dicetechnology/dice-fairlink/dice-fairlink/images/download.svg) ](https://bintray.com/dicetechnology/dice-fairlink/dice-fairlink/_latestVersion)

dice-fairlink is a JDBC driver designed to connect to the read replicas of an AWS Aurora cluster.
The driver will periodically obtain a description of the cluster and dispatch connections to each read replica
on a round-robin fashion.

dice-fairlink does **not** handle read/write connections

# Why do we need dice-fairlink (TL/DR version)?
Because in many cases Aurora will not evenly distribute the connections amongst all the available read replicas.
[image]

# How can I use dice-fairlink (TL/DR version)?
- Add dice-fairlink as a dependency to your JVM project
- Add `auroraro` as a jdbc sub-protocol to your connection string's schema
- Change your connection string's host to the name of your AWS Aurora cluster 

# Usage Examples

dice-fairlink implements a generic sub-protocol of any existing jdbc protocol (psql,mysql,etc). The host section
of the URL should be the cluster identifier and not the hostname of any cluster or instance endpoint.
The driver will accept urls in the form `jdbc:XXXX:auroraro` and delegate the actual handling of the connection
to the driver of the protocol `XXXX` (which needs to be loadable by the JVM classloader).

## Example:

In a cluster named `my-cluster` with three read replicas `my-cluster-r1`, `my-cluster-r2` and, `my-cluster-r3`, and 
the following connection string
```java
String connectionString = "jdbc:mysql:auroraro//my-cluster/my-schema";
```
dice-fairlink will return `my-cluster-r1` for the first connection request, `my-cluster-r2` to the second
and, `my-cluster-r3` to the third. The forth request for a connection will again return `my-cluster-r1`, and so forth.

Only replicas in the `available` state will be used. 

In this example dice-fairlink will use the available mysql driver to establish the connection to the read replica.

Dynamic changes to the cluster (node promotions, removals and additions) are automatically detected.

# Why do we need dice-fairlink ?
Using AWS Aurora clusters with database connection pools is a possible use case. A possible configuration is to point
a connection pool to the cluster's read-only endpoint. AWS claims ([here](https://aws.amazon.com/blogs/aws/new-reader-endpoint-for-amazon-aurora-load-balancing-higher-availability/),
[here](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.Overview.Endpoints.html), and
[here](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/CHAP_Aurora.html#Aurora.Overview.Endpoints)) 
that Aurora will send the new connections to different read replicas in a quasi-round-robin fashion. It is well documented on
the references above that Aurora does this based on the number of connections each of the replicas is holding at the time of receiving a new connection request. This is done via DNS, with a 1 second TTL. This means that, for a period of 1 
second, all new connection requests will be sent to the same read replica.

**Example**:
Consider an Aurora cluster with the read endpoint at `read-endpoint-url`, and read replicas `r1`, `r2`, `r3`, and `r4`.
Also consider an application using a fixed-sized connection pool of 10 connections, recycled every 30 minutes. Finally,
consider we have a cluster of 3 servers running this application. When we launch the servers for the first time, the 
following is a possible timeline (times in ms), starting from an idle cluster:
- **t0**: Server 1 comes online and pre-fills the connection pool, sending 10 connection requests to `read-endpoint-url`
- **t0**: Aurora directs 10 connections to `r1`
- **t500**: Server 2 comes online and pre-fills the connection pool, sending 10 connection requests to `read-endpoint-url`
- **t750**: Aurora directs 10 connections to `r1`
- **t1500**: Server 3 comes online and pre-fills the connection pool, sending 10 connection requests to `read-endpoint-url`
- **t1500**: Aurora directs 10 connections to `r2`

The ideal scenario would be 10 connection on each read-replica. Unfortunately, as Server 1 and Server 2 populated their 
connection pools with less than 1 seconds' difference, and Aurora has cached the name resolution of `read-endpoint-url` 
to `r1`for 1 second starting on **t0**, Server 2's requests will also be sent to `r1`. `r1` ends up serving 20 connections,
`r2` 10 connections and `r3` will be idle. 

The fact Aurora's uses DNS to distribute the connections amongst the available read replicas can also be problematic due to
other components of a solution. If any network agent (local server, router, etc) caches DNS resolutions, the results will
become harder to predict. On top of this, Java can also cache DNS resolutions. It does so by default **forever**, 
or for 30 seconds depending on the JVM version and vendor.

## What other options did we try before writing dice-fairlink ?
We tried the following, commutative, options

### Controlling DNS
In a controlled environment we disabled Java DNS cache (see [here](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-jvm-ttl.html),
or [here](https://docs.oracle.com/javase/7/docs/technotes/guides/net/properties.html)) and any other intermediate caches between the server
and the Aurora cluster. 

**result**: this allowed us to achieve the results described on the previous section.

### Tweaking pool parameters
We configured our connection pool to not be fixed-sized and to have a much lower connection maximum lifetime (2 minutes).
Additionally we had a random (maximum 2.5% of the maximum lifetime) variance on the maximum lifetime for each pool generation. 
Finally, each application server had a different maximum connection lifetime.
 
The rationale was to try to disperse connection requests to the Aurora cluster as much as possible. 

**result**: with the non-deterministic random variables did generate better distribution in some occasions. However, the 
random nature of this experiment also means that, in other occasions, a single read replica received all 30 connections.
It is not simple to reliably set all the variables mentioned above in such a way that each server will request a connection
to Aurora if and only if no other server has requested a connection in the previous second. 

## How does dice-fairlink solve this problem ?
dice-fairlink does not require using the Aurora cluster read only endpoint. Instead, it keeps a list of addresses
for every `available` read replica of a given cluster. When the client application (through a connection pool or otherwise)
requests a connection to the jdbc driver, dice-fairlink selects the next `available` read replica and delegates the
actual establishing of the connection to the underlying jdbc driver (see usage examples). 
The frequency with which this list is refreshed is configurable (see driver parameters). 
In the current version, dice-fairlink does not dynamically mark replicas as faulty, or try to despatch connections 
taking into account how busy each replica is. It simply returns the read replica that hasn't been returned for longer
(round-robin).

# Installation

Add the following repository to your `~/.m2/settings.xml` file or to your `pom.xml` file.
```xml
<repository>
    <snapshots>
	<enabled>false</enabled>
    </snapshots>
    <id>bintray-dicetechnology-dice-fairlink</id>
    <name>bintray</name>
    <url>https://dl.bintray.com/dicetechnology/dice-fairlink</url>
</repository>

```xml
Add the following dependency to your `pom.xml`
<dependency>
	<groupId>technology.dice.open</groupId>
	<artifactId>dice-fairlink</artifactId>
	<version>1.0.6</version>
</dependency>
```


# Driver properties
dice-fairlink uses the AWS RDS Java SDK to obtain information about the cluster, and needs a valid authentication
source to establish the connection. Two modes of authentication are supported: `environment` or `basic`. Depending
on the chosen mode, different driver properties are required. This is the full list of properties:
- `auroraClusterRegion`: the AWS region of the cluster to connect to. Mandatory.
- `auroraDiscoveryAuthMode`: `{'environment'|'basic'}`. default: `environment`
- `auroraDiscoveryKeyId`: the AWS key id to connect to the Aurora cluster. Mandatory if the authentication mode is `basic`. 
Ignored otherwise.
- `auroraDiscoverKeySecret`: the AWS key secret to connect to the Aurora cluster. Mandatory if the authentication mode is `basic`. 
Ignored otherwise.
- `replicaPollInterval`: the interval, in seconds, between each refresh of the list of read replicas. default: `30` 


all properties (including the list above) will be passed to the underlying driver.

# Logging
To limit the dependencies of dice-fairlink, the `java.util.logging` package is used for logging. 
Client applications may make use of the popular `slf4j` library, in which case the following block of 
bootstrap code is necessary to connect the two logging systems:
```java
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
```
additionally, the following dependency must be added to the project:
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>jul-to-slf4j</artifactId>
    <version>x.y.z</version>
</dependency>
```
This will direct the `java.util.logging` logging statements to SLF4J, and make them available to any
logging backend as `logback` or `log4j`.
