#!/bin/bash

set -e

echo ">> DOCKER-ENTRYPOINT: GENERATING SSL CERT"

cd /opt/ssl/
openssl genrsa -des3 -passout pass:testP@sswor1d -out server.pass.key 2048
openssl rsa -passin pass:testP@sswor1d -in server.pass.key -out server.key
rm server.pass.key
openssl req -new -key server.key -out server.csr -subj "/C=US/ST=Washington/L=Seattle/O=evaluation.nickrobison.com/OU=evaluation.nickrobison.com/CN=evaluation.nickrobison.com"
openssl x509 -req -sha256 -days 300065 -in server.csr -signkey server.key -out server.crt
#cd /opt/www/

echo ">> DOCKER-ENTRYPOINT: GENERATING SSL CERT ... DONE"
echo ">> DOCKER-ENTRYPOINT: EXECUTING CMD"

exec "$@"