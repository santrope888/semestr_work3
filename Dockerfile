FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre

RUN groupadd --system spring && useradd --system --gid spring spring

WORKDIR /app
COPY --from=build /app/target/semestr_work3-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/uploads && chown -R spring:spring /app

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]