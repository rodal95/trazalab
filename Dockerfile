# TRAZALAB - version web de demostracion (rama web).
# La consola del sistema corre en el navegador dentro de una terminal web (ttyd):
# cada visitante abre su propia sesion de la aplicacion, contra la misma base MySQL.

# ---------------------------------------------------------------- compilacion
FROM eclipse-temurin:21-jdk-noble AS compilacion
WORKDIR /fuente
COPY lib lib
COPY src src
RUN javac -encoding UTF-8 --release 17 -cp lib/mysql-connector-j-8.4.0.jar -d clases $(find src -name "*.java")

# ---------------------------------------------------------------- ejecucion
FROM eclipse-temurin:21-jre-noble
RUN apt-get update \
 && apt-get install -y --no-install-recommends ttyd mysql-client tzdata nginx \
 && rm -rf /var/lib/apt/lists/*
ENV TZ=America/Argentina/Cordoba \
    LANG=C.UTF-8
WORKDIR /app
COPY --from=compilacion /fuente/clases clases
COPY lib lib
COPY sql sql
COPY web/iniciar.sh web/cargar-base.sh web/nginx.conf web/
COPY web/sitio web/sitio
RUN chmod +x web/*.sh && mkdir -p config salidas \
 && useradd --system --home-dir /app trazalab && chown -R trazalab /app
USER trazalab
CMD ["/app/web/iniciar.sh"]
