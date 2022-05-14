#!/bin/bash
PROJECT_ROOT=/home/vagrant/dev/group_03_project
mvn -pl Gateway -am clean package && java -jar Gateway/target/Gateway-1.0-SNAPSHOT-jar-with-dependencies.jar $(hostname -I | awk '{print $2}') 1