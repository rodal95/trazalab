#!/bin/sh
# Construye la carpeta que se instala en cada puesto del laboratorio:
#
#   dist/trazalab/
#     trazalab.jar                       aplicacion (clase principal y classpath en el manifiesto)
#     lib/mysql-connector-j-8.4.0.jar    conector JDBC
#     config/trazalab.properties.ejemplo configuracion de la conexion
#     sql/                               scripts de la base (los usa el servidor)
#
#   sh construir.sh
#
# Se compila para Java 17 (--release 17) aunque se desarrolle con el JDK 21:
# los puestos tienen el JRE 17 (RNF01) y no podrian ejecutar clases de Java 21.
set -e
cd "$(dirname "$0")"

CONECTOR=lib/mysql-connector-j-8.4.0.jar
VERSION=$(git describe --tags --always --dirty 2>/dev/null || echo sin-version)

rm -rf build dist
javac -encoding UTF-8 -Xlint:all --release 17 -cp "$CONECTOR" -d build/clases $(find src -name "*.java")

cat > build/manifiesto.txt <<FIN
Main-Class: trazalab.app.MenuPrincipal
Class-Path: $CONECTOR
Implementation-Title: TRAZALAB
Implementation-Version: $VERSION
FIN

mkdir -p dist/trazalab/lib dist/trazalab/config
jar --create --file dist/trazalab/trazalab.jar --manifest build/manifiesto.txt -C build/clases .
cp "$CONECTOR" dist/trazalab/lib/
cp config/trazalab.properties.ejemplo dist/trazalab/config/
cp -r sql dist/trazalab/
echo "Construido dist/trazalab/trazalab.jar (version $VERSION)"
