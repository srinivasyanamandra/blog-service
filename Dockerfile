FROM maven:3.9.6-eclipse-temurin-21 AS base
WORKDIR /app
COPY . .
RUN mvn clean install -DskipTests=true

FROM eclipse-temurin:21-jdk-jammy
COPY --from=base /app/target/pranuBlog-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
