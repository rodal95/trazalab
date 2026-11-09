#!/bin/sh
# Arranque del contenedor: configura la conexion, carga la base, programa el
# restablecimiento nocturno y publica la consola en el navegador con ttyd.
set -e
: "${MYSQLHOST:?Falta MYSQLHOST (variable del servicio MySQL de Railway)}"
: "${MYSQLUSER:?Falta MYSQLUSER}"
: "${MYSQLPASSWORD:?Falta MYSQLPASSWORD}"
PUERTO="${PORT:-8080}"

cat > /app/config/trazalab.properties <<FIN
db.url=jdbc:mysql://$MYSQLHOST:${MYSQLPORT:-3306}/trazalab?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000
db.usuario=$MYSQLUSER
db.clave=$MYSQLPASSWORD
db.zona=-03:00
FIN

# Espera a que MySQL acepte conexiones (en Railway puede arrancar despues).
intentos=0
until mysqladmin ping --host="$MYSQLHOST" --port="${MYSQLPORT:-3306}" --user="$MYSQLUSER" \
        --password="$MYSQLPASSWORD" --silent 2>/dev/null; do
    intentos=$((intentos + 1))
    [ "$intentos" -ge 60 ] && { echo "[base] MySQL no responde"; exit 1; }
    sleep 2
done

if [ "${RESTABLECER_AL_INICIAR:-si}" = "si" ]; then
    /app/web/cargar-base.sh
fi

# Todas las noches a las 4 la base vuelve a los datos ficticios iniciales.
(
    while true; do
        objetivo=$(date -d '04:00' +%s)
        [ "$(date +%s)" -ge "$objetivo" ] && objetivo=$(date -d 'tomorrow 04:00' +%s)
        sleep $((objetivo - $(date +%s)))
        /app/web/cargar-base.sh || echo "[base] no se pudo restablecer"
    done
) &

# Cada conexion abre su propia sesion de TRAZALAB. ttyd no ofrece una shell:
# solo ejecuta la aplicacion, y al salir de ella la sesion termina. Escucha en un
# socket local; nginx lo publica en /consola/ junto con la portada y el manual.
(
    while true; do
        rm -f /tmp/ttyd.sock
        ttyd --interface /tmp/ttyd.sock --base-path /consola --writable --max-clients "${MAX_SESIONES:-10}" \
            --client-option titleFixed="TRAZALAB - Laboratorio Bioquimico San Rafael" \
            --client-option fontSize=15 \
            --client-option disableLeaveAlert=true \
            --client-option 'theme={"background":"#0b1210","foreground":"#dbe0df","cursor":"#2db29a"}' \
            java -Xmx96m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 \
                 -Duser.language=es -Duser.country=AR \
                 -Dtrazalab.web=true -Dtrazalab.config=/app/config/trazalab.properties \
                 -cp "/app/clases:/app/lib/mysql-connector-j-8.4.0.jar" trazalab.app.MenuPrincipal
        echo "[consola] ttyd termino; se reinicia"
        sleep 1
    done
) &

sed "s/PUERTO/$PUERTO/" /app/web/nginx.conf > /tmp/nginx.conf
exec nginx -c /tmp/nginx.conf -g 'daemon off;'
