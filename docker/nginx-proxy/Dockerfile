FROM nginx:1.13.8-alpine

LABEL name="nginx-proxy"
LABEL maintainer="Nick Robison <nick@nickrobison.com>"
LABEL description="Nginx image for proxying connections to Trestle Server"

RUN apk update && \
    apk add openssl

WORKDIR /opt

ADD entrypoint.sh .
ADD nginx.conf .

RUN chmod u+rx,g+rx,o+rx,a-w /opt/entrypoint.sh && \
#    adduser -u 10777 -h /opt nginx && \
#    addgroup -g 10777 nginx && \
#            mkdir -p /opt/www && \
    mkdir -p /opt/ssl && \
    chown -R nginx:nginx /opt/ && \
    mkdir -p /nginx/tmp/ && \
    chown -R nginx:nginx /nginx/

USER nginx

ENTRYPOINT ["/opt/entrypoint.sh"]
CMD ["nginx", "-c", "/opt/nginx.conf", "-g", "daemon off;"]

EXPOSE 4443
