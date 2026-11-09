#!/bin/sh
# Crea la base desde cero con los datos ficticios de la demostracion:
# 01-esquema.sql (tablas y disparadores), 02-datos.sql y 06-web.sql.
# El usuario y la contrasenia de la demostracion salen de las variables
# DEMO_USUARIO y DEMO_CONTRASENIA del servicio (en Railway: Variables).
set -e
mysql_trazalab() {
    mysql --host="$MYSQLHOST" --port="${MYSQLPORT:-3306}" --user="$MYSQLUSER" \
          --password="$MYSQLPASSWORD" --init-command="SET time_zone='-03:00'" "$@" 2>/dev/null
}
for script in 01-esquema 02-datos 06-web; do
    mysql_trazalab < "/app/sql/$script.sql" > /dev/null
done
if [ -n "$DEMO_USUARIO" ] && [ -n "$DEMO_CONTRASENIA" ]; then
    usuario=$(printf '%s' "$DEMO_USUARIO" | sed "s/'/''/g")
    clave=$(printf '%s' "$DEMO_CONTRASENIA" | sed "s/'/''/g")
    mysql_trazalab trazalab -e "UPDATE usuarios SET username = '$usuario', password_hash = SHA2('$clave', 256) WHERE username = 'demo';"
    echo "[base] usuario de demostracion: $DEMO_USUARIO (contrasenia en DEMO_CONTRASENIA)"
fi
echo "[base] restablecida $(date '+%d/%m/%Y %H:%M')"
