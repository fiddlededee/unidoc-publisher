FROM alpine:3.21.0
RUN apk add --no-cache \
    openjdk11

RUN set -xe \
    && apk add --no-cache --purge -uU \
        libreoffice \
        ttf-freefont ttf-opensans ttf-inconsolata \
	    ttf-liberation ttf-dejavu \
        libstdc++ dbus-x11 \
        font-noto-cjk\
    && echo "http://dl-cdn.alpinelinux.org/alpine/edge/main" >> /etc/apk/repositories \
    && echo "http://dl-cdn.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories \
    && echo "http://dl-cdn.alpinelinux.org/alpine/edge/testing" >> /etc/apk/repositories \
    && apk add --no-cache -U \
	ttf-font-awesome ttf-mononoki ttf-hack \
    && rm -rf /var/cache/apk/* /tmp/*

WORKDIR /documents
VOLUME /documents