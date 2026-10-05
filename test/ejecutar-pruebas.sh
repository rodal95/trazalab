#!/bin/sh
# Compila el prototipo junto con las pruebas y ejecuta:
#   - la bateria de componente e integracion (CPC01, CPF02, CPC03, CPF04, CPI05);
#   - los guiones de prueba de sistema de test/sistema/ (CPF02, CPS06, CPS07).
#
#   sh test/ejecutar-pruebas.sh               salidas en pantalla
#   sh test/ejecutar-pruebas.sh evidencias    salidas guardadas en esa carpeta
#
# Termina con error si alguna comprobacion de la bateria falla.
set -e
cd "$(dirname "$0")/.."

CLASES=build/pruebas
rm -rf "$CLASES"
javac -encoding UTF-8 -Xlint:all -d "$CLASES" $(find src test -name "*.java")

CP="$CLASES"
for jar in lib/*.jar; do
    [ -f "$jar" ] && CP="$CP:$jar"
done

ejecutar() {   # ejecutar <archivo-de-salida> <clase> [argumentos]
    salida=$1
    shift
    if [ -n "$DESTINO" ]; then
        java -cp "$CP" "$@" > "$DESTINO/$salida"
        echo "  $DESTINO/$salida"
    else
        java -cp "$CP" "$@"
    fi
}

DESTINO=$1
[ -n "$DESTINO" ] && mkdir -p "$DESTINO"

ejecutar casos-de-prueba-componente.txt trazalab.pruebas.CasosDePrueba
for guion in test/sistema/*.txt; do
    caso=$(basename "$guion" .txt)
    ejecutar "prueba-sistema-$caso.txt" trazalab.pruebas.EjecutorSistema "$guion"
done
