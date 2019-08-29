#!/bin/bash

if (( $# < 5 )); then
    echo "usage: $0 host port username password iterations"
    exit 1;
fi

i=1
host=$1
port=$2
username=$3
password=$4
end=$5

while [ $i -le $end ]; do
    echo $(date) - iteration $i
    mysql -h $host -P $port -u $username -p$password -e "select server_id, if(session_id =    'MASTER_SESSION_ID','WRITER', 'READER') as role from information_schema.replica_host_status;"
    i=$(($i+1))
done




