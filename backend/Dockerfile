# Build em multi-stage: a imagem final nao carrega Maven nem codigo-fonte.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S devmetrics && adduser -S devmetrics -G devmetrics
COPY --from=build /build/target/devmetrics-api-*.jar app.jar
RUN chown -R devmetrics:devmetrics /app
USER devmetrics
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
