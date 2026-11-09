#!/bin/sh
# Crea la base desde cero con los datos ficticios de la demostracion:
# 01-esquema.sql (tablas y disparadores), 02-datos.sql y 06-web.sql.
set -e
for script in 01-esquema 02-datos 06-web; do
    mysql --host="$MYSQLHOST" --port="${MYSQLPORT:-3306}" --user="$MYSQLUSER" \
          --password="$MYSQLPASSWORD" --init-command="SET time_zone='-03:00'" \
          < "/app/sql/$script.sql" > /dev/null
done
echo "[base] restablecida $(date '+%d/%m/%Y %H:%M')"
