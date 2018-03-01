#!/bin/sh

set -e

cd /opt/trestle
echo ">> Migrating Server db"
java -jar trestle.jar db migrate config.yml

exec "$@"
