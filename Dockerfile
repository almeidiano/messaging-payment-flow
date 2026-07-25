# ─── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:24-jdk AS builder

WORKDIR /app

# Copia apenas os arquivos de configuração do Gradle primeiro (melhor cache)
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Baixa as dependências sem compilar o código-fonte
RUN ./gradlew dependencies --no-daemon || true

# Copia o código-fonte e gera o fat-jar
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:24-jre

LABEL maintainer="almeidiano"
LABEL description="Messaging - Fluxo de Pagamento com RabbitMQ"

WORKDIR /app

# Cria usuário sem privilégios para rodar a aplicação
RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown appuser:appuser app.jar
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]
