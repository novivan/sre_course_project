FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew

COPY src src
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system auction \
    && useradd --system --gid auction --home-dir /app auction

WORKDIR /app
COPY --from=build /workspace/build/libs/auction-*.jar /app/auction.jar

USER auction
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/auction.jar"]
