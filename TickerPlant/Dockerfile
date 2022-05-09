FROM openjdk:11
WORKDIR /TickerPlant
COPY ../out/artifacts/TickerPlant_jar/TickerPlant.jar TickerPlant.jar

# Suppress warning related to sun.misc.Unsafe and sun.nio.ch.SelectorImpl.selectedKeys
ENTRYPOINT [ "java", "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED", "--add-opens", "jdk.unsupported/sun.misc=ALL-UNNAMED", "-jar", "TickerPlant.jar"]