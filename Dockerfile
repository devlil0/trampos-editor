FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# pom primeiro: enquanto as dependências não mudam, esta layer fica em cache entre builds.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Contexto da API filtrado pelo .dockerignore; testes não são copiados para a imagem.
COPY src/main ./src/main
RUN mvn -B clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S tramposeditor && adduser -S tramposeditor -G tramposeditor

COPY --from=build /build/target/*.jar app.jar
RUN chown tramposeditor:tramposeditor app.jar

USER tramposeditor
EXPOSE 8080

# MaxRAMPercentage: a JVM respeita o limite de memória do container em vez do host.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
