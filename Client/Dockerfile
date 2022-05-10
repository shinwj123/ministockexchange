FROM openjdk:11
WORKDIR /Gateway
COPY ../out/artifacts/Gateway_jar/Gateway.jar Gateway.jar

# Suppress warning related to sun.misc.Unsafe and sun.nio.ch.SelectorImpl.selectedKeys
ENTRYPOINT [ "java", "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED", "--add-opens", "jdk.unsupported/sun.misc=ALL-UNNAMED", "-jar", "Gateway.jar"]