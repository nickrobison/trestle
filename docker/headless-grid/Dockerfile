# Borrowed from https://github.com/Zenika/alpine-chrome

FROM openjdk:8-jdk-alpine

# Update apk repositories
RUN echo "http://dl-2.alpinelinux.org/alpine/edge/main" > /etc/apk/repositories
RUN echo "http://dl-2.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories
RUN echo "http://dl-2.alpinelinux.org/alpine/edge/testing" >> /etc/apk/repositories

# Install chromium
RUN apk -U --no-cache \
	--allow-untrusted add \
    zlib-dev \
    chromium \
    chromium-chromedriver \
    xvfb \
    wait4ports \
    xorg-server \
    dbus \
    ttf-freefont \
    mesa-dri-swrast \
    grep \
    udev \
    curl \
    firefox \

    && apk del --purge --force linux-headers binutils-gold gnupg zlib-dev libc-utils \
    && rm -rf /var/lib/apt/lists/* \
    /var/cache/apk/* \
    /usr/share/man \
    /tmp/* \
    /usr/lib/node_modules/npm/man \
    /usr/lib/node_modules/npm/doc \
    /usr/lib/node_modules/npm/html \
    /usr/lib/node_modules/npm/scripts

ENV CHROME_BIN=/usr/bin/chromium-browser
ENV CHROME_PATH=/usr/lib/chromium/

# Selenium
WORKDIR /opt

# Setup Chrome Driver (2.35)
#RUN curl -o chromedriver.zip -sSL "https://chromedriver.storage.googleapis.com/2.35/chromedriver_linux64.zip" && \
#    unzip chromedriver.zip && \
#    mv chromedriver /usr/local/bin

# Setup Firefox (58.0.2) and Geckodriver (0.19.1)
RUN curl -L "https://github.com/mozilla/geckodriver/releases/download/v0.19.1/geckodriver-v0.19.1-linux64.tar.gz" | tar zxf - -C /usr/local/bin/
#RUN curl -L "https://ftp.mozilla.org/pub/firefox/releases/58.0.2/linux-x86_64/en-US/firefox-58.0.2.tar.bz2" | tar jxf - && \
#    curl -L "https://github.com/mozilla/geckodriver/releases/download/v0.19.1/geckodriver-v0.19.1-linux64.tar.gz" | tar zxf - -C /usr/local/bin/

# Selenium (3.9.1)
RUN curl -o grid.jar -L "https://goo.gl/SP94ZB"

# Add Chrome as a user
#RUN groupadd -r chrome && useradd -r -g chrome -G audio,video chrome \
#    && mkdir -p /home/chrome && chown -R chrome:chrome /home/chrome
# Run Chrome non-privileged
#USER chrome

# Autorun chrome headless with no GPU
ENTRYPOINT ["java", "-jar", "/opt/grid.jar"]

EXPOSE 4444
