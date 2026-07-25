# ─── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:24-jdk AS builder

WORKDIR /app

# Instala o Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copia apenas o pom.xml primeiro para aproveitar o cache do Docker
COPY pom.xml ./

# Baixa as dependências sem compilar o código-fonte
RUN mvn dependency:go-offline -q

# Copia o código-fonte e gera o fat-jar
COPY src ./src
RUN mvn package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:24-jre

LABEL maintainer="almeidiano"
LABEL description="Messaging - Fluxo de Pagamento com RabbitMQ"

WORKDIR /app

# Cria usuário sem privilégios para rodar a aplicação
RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=builder /app/target/*.jar app.jar

RUN chown appuser:appuser app.jar
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]
