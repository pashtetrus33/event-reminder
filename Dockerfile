FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /workspace
COPY pom.xml .
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn clean package -DskipTests -Dspotless.skip=true -B -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=builder /workspace/target/*.jar app.jar
RUN mkdir -p /app/logs && chown -R app:app /app/logs
USER app
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
