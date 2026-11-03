# 第一步：在容器里编译（不需要本机装 Java 和 Maven）
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
# 先单独下载依赖：只要 pom.xml 不变，这一层会被缓存，之后重新构建很快
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

# 第二步：运行镜像只包含 Java 运行环境和打好的 jar，体积更小
FROM eclipse-temurin:21-jre
WORKDIR /app
# 不用 root 运行，降低被攻破后的影响
RUN useradd --system --uid 10001 app
COPY --from=build /src/target/ticket-triage.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
