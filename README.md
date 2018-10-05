[![Build Status](https://travis-ci.com/DiceTechnology/dice-fairlink.svg?token=F6ktiNWbNbvGRbN5NmqA&branch=master)](https://travis-ci.com/DiceTechnology/dice-fairlink)

dice-fairlink is a JDBC driver designed to connect to the read replicas of an AWS Aurora cluster.
The driver will periodically obtain a description of the cluster and dispatch connections to each read replica
on a pure round-robin fashion.

# Installation

WIP

# Usage Examples

dice-fairlink implements a generic sub-protocol of any existing jdbc protocol (psql,mysql,etc). The host section
of the URL should be the cluster identifier and not the hostname of any cluster or instance endpoint.
The driver will accept urls in the form `jdbc:XXXX:auroraro` and delegate the actual handling of the connection
to the driver of the protocol `XXXX`

## Example:

In a cluster named `my-cluster` with three read replicas `my-cluster-r1`, `my-cluster-r2` and, `my-cluster-r3`, and 
the following connection string
```java
String connectionString = "jdbc:mysql:auroraro://my-cluster/my-schema";
```
dice-fairlink will return `my-cluster-r1` for the first connection request, `my-cluster-r2` to the second
and, `my-cluster-r3` to the third. The forth request for a connection will again return `my-cluster-r1`, and so forth.

Only replicas in the `available` state will be used. 

In this example dice-fairlink will use the available mysql driver to establish the connection to the read replica.

Dynamic changes to the cluster (node promotions, removals and additions) are automatically detected.


#Driver properties
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