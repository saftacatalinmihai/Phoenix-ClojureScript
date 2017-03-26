FROM phusion/baseimage

LABEL maintainer "saftacatalinmihai@gmail.com"

RUN \
  add-apt-repository ppa:openjdk-r/ppa \
  && apt-get update \
  && apt-get install -y -qq --no-install-recommends \
    openjdk-8-jdk

RUN curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > /usr/bin/lein
RUN chmod 755 /usr/bin/lein

ENV LEIN_ROOT=1
# Actually install leiningen.
RUN lein --version

WORKDIR /code