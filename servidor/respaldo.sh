#!/bin/bash
# Respaldo diario de la base trazalab (RNF10): copia comprimida y
# conservacion de los ultimos 30 dias. Se programa en el servidor con cron:
#   0 22 * * * /opt/trazalab/respaldo.sh
#
# Las credenciales estan en un archivo de opciones de MySQL con permisos 600,
# no en este script. Variables opcionales: DESTINO, OPCIONES, DIAS.
set -euo pipefail

DESTINO=${DESTINO:-/var/backups/trazalab}
OPCIONES=${OPCIONES:-/etc/trazalab/respaldo.cnf}
DIAS=${DIAS:-30}

# Las copias tienen datos personales y clinicos (Ley 25.326): solo las lee root.
umask 077
mkdir -p "$DESTINO"
ARCHIVO="$DESTINO/trazalab-$(date +%Y%m%d-%H%M).sql.gz"
trap 'rm -f "$ARCHIVO.tmp"' EXIT

# --single-transaction: copia consistente sin bloquear a los puestos (InnoDB).
# --triggers: incluye los disparadores de inalterabilidad de la bitacora.
# Se escribe en un temporal y se renombra solo si mysqldump termino bien.
mysqldump --defaults-extra-file="$OPCIONES" --single-transaction --triggers \
    --no-tablespaces --databases trazalab | gzip > "$ARCHIVO.tmp"
mv "$ARCHIVO.tmp" "$ARCHIVO"

# Borra las copias con mas de DIAS dias.
find "$DESTINO" -name 'trazalab-*.sql.gz' -mtime +$((DIAS - 1)) -delete
echo "Respaldo generado: $ARCHIVO"
