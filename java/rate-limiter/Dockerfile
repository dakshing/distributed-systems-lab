FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests -pl rate-limiter -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/rate-limiter/target/rate-limiter-1.0-SNAPSHOT.jar app.jar
COPY --from=build /app/rate-limiter/target/lib lib


ENTRYPOINT ["java", "-cp", "app.jar:lib/*", "com.dsl.ratelimiter.Application"]