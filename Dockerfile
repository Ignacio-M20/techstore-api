# Stage 1: Compilación de la aplicación usando Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Imagen base de ejecución ligera alpine
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/target/techstore-api-1.0.0.jar app.jar

# Configurar un usuario seguro no-root por motivos de seguridad cloud
RUN chown -R nobody:nobody /app
USER nobody

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]