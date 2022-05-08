FROM openjdk:11
WORKDIR /aeron
COPY ../out/artifacts/MediaDriver_jar/MediaDriver.jar MediaDriver.jar

# Suppress warning related to sun.misc.Unsafe and sun.nio.ch.SelectorImpl.selectedKeys
ENTRYPOINT [ "java", "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED", "--add-opens", "jdk.unsupported/sun.misc=ALL-UNNAMED", "-jar", "MediaDriver.jar"]