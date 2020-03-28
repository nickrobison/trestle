#!/usr/bin/env sh

set -e

JAVA_CLASSES="-cp /app/resources:/app/classes:/app/libs/* com.nickrobison.trestle.server.TrestleServer"

echo ">> Migrating Server db"
eval "java ${JVM_FLAGS} ${JAVA_CLASSES} db migrate ${CONFIG_FILE}"

CMDLINE="java ${JVM_FLAGS} ${JAVA_CLASSES} server ${CONFIG_FILE}"

exec ${CMDLINE}
