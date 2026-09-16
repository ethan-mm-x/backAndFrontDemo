# 请用 scripts/pack-images.sh 构建（带 --platform linux/amd64）
# 避免 Apple Silicon 打出的 arm64 镜像在 x86 服务器上出现 exec format error
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY src src
RUN mvn -s .mvn/settings.xml -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
