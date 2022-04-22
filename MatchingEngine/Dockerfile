FROM openjdk:11
WORKDIR /MatchingEngine
COPY ../out/artifacts/MatchingEngine_jar/MatchingEngine.jar MatchingEngine.jar

# Suppress warning related to sun.misc.Unsafe and sun.nio.ch.SelectorImpl.selectedKeys
ENTRYPOINT [ "java", "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED", "--add-opens", "jdk.unsupported/sun.misc=ALL-UNNAMED", "-jar", "MatchingEngine.jar"]