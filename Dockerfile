FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# copy only what we need to leverage Docker layer caching
COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# copy the fat jar produced by the build stage
COPY --from=build /workspace/target/*.jar ./app.jar 

EXPOSE 8080

# JVM opts tuned for container environments
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

CMD ["java","-jar","/app/app.jar"]
