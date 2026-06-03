# Estágio 1: Build (Compilação)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

# Estágio 2: Runtime (Execução)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV ANALYZER_SERVER_PORT=15600

EXPOSE ${ANALYZER_SERVER_PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]