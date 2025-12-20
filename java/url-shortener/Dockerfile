# --- Build the JAR ---
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests -pl url-shortener -am

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/url-shortener/target/url-shortener-1.0-SNAPSHOT.jar app.jar
COPY --from=build /app/url-shortener/target/lib lib

# Run it
ENTRYPOINT ["java", "-cp", "app.jar:lib/*", "com.dsl.urlshortener.Application"]