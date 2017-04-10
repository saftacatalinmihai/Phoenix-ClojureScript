FROM phusion/baseimage

LABEL maintainer "saftacatalinmihai@gmail.com"

RUN \
  add-apt-repository ppa:openjdk-r/ppa \
  && apt-get update \
  && apt-get install -y -qq --no-install-recommends \
    openjdk-8-jdk git

RUN curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > /usr/bin/lein
RUN chmod 755 /usr/bin/lein
RUN \
    cd /usr/local/bin \
    && curl -fsSLo boot https://github.com/boot-clj/boot-bin/releases/download/latest/boot.sh \
    && chmod 755 boot
ENV BOOT_AS_ROOT=yes
RUN \
    mkdir -p ~/packages \
    && cd ~/packages \
    && git clone https://github.com/saftacatalinmihai/packages.git \
    && cd packages/pixi \
    && boot package install target

ENV LEIN_ROOT=1
# Actually install leiningen.
RUN lein --version

WORKDIR /code