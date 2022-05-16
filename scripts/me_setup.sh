#!/bin/bash
# Suppress warning related to sun.misc.Unsafe and sun.nio.ch.SelectorImpl.selectedKeys
mvn -pl MatchingEngine -am clean package && java --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens jdk.unsupported/sun.misc=ALL-UNNAMED -jar MatchingEngine/target/MatchingEngine-1.0-SNAPSHOT-jar-with-dependencies.jar $(hostname -I | awk '{print $2}') 1