#!/bin/bash
echo "Creating JAR"
mvn install > /dev/null
echo "Created JAR"

echo "Sending JAR"
sftp root@10.0.0.32:/home/christian/dev_papermc/data/plugins <<< $'put ./target/Medieval-Factions-4.7.0-SNAPSHOT.jar' > /dev/null
echo "Sent JAR"