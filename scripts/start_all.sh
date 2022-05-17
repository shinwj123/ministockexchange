#!/bin/bash
NUM_ME=$1
#vagrant up

function trap_ctrlc ()
{
    # perform cleanup here
    echo "Ctrl-C caught...performing clean up"

    exit 2
}


if [[ $# -eq 0 ]] || [[ NUM_ME -eq 1 ]]
then
  echo "Using one matching engine..."
#  vagrant ssh matching-engine1 -c "nohup $(cd /vagrant && mvn -pl MatchingEngine -am clean package && java --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens jdk.unsupported/sun.misc=ALL-UNNAMED -jar MatchingEngine/target/MatchingEngine-1.0-SNAPSHOT-jar-with-dependencies.jar 192.168.0.51 1 > /dev/null 2>&1 &)"

elif [[ NUM_ME -eq 2 ]]
then
  echo "Using two matching engine..."
#  vagrant ssh matching-engine1 -c "cd /vagrant && mvn -pl MatchingEngine -am clean package && java --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens jdk.unsupported/sun.misc=ALL-UNNAMED -jar MatchingEngine/target/MatchingEngine-1.0-SNAPSHOT-jar-with-dependencies.jar 192.168.0.51 1"
#  vagrant ssh matching-engine2 -c "cd /vagrant && mvn -pl MatchingEngine -am clean package && java --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens jdk.unsupported/sun.misc=ALL-UNNAMED -jar MatchingEngine/target/MatchingEngine-1.0-SNAPSHOT-jar-with-dependencies.jar 192.168.0.52 2"
else
  echo "currently number of matching engine should be either 1 or 2"
  exit 0
fi

trap "trap_ctrlc" 2
read -r -d '' _ </dev/tty