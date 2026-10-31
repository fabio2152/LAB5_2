# ---------- Etapa 1: compilar con Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Descargar dependencias primero (se cachea mientras no cambie el pom.xml)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: imagen de ejecución ----------
FROM eclipse-temurin:21-jre-noble

# FFmpeg arma el video; las fuentes DejaVu se usan para escribir los titulares en las fotos
# (los repositorios se usan por HTTPS para evitar proxies que alteren las descargas HTTP)
RUN sed -i 's|http://|https://|g' /etc/apt/sources.list.d/ubuntu.sources \
    && apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg fonts-dejavu-core fontconfig \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --create-home app \
    && mkdir -p /app/data/media \
    && chown -R app:app /app
WORKDIR /app

COPY --from=build /build/target/marketing-ai.jar app.jar

USER app
ENV STORAGE_DIR=/app/data/media \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.awt.headless=true"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
