FROM elixir:latest

MAINTAINER Safta Catalin Mihai <saftacatalinmihai@gmail.com>

RUN apt-get update\
    && apt-get install -y inotify-tools

# Install hex
RUN mix local.hex --force

# Install rebar
RUN mix local.rebar --force

# Install the Phoenix framework itself
RUN mix archive.install https://github.com/phoenixframework/archives/raw/master/phoenix_new.ez --force

# Install NodeJS 6.x and the NPM
RUN curl -sL https://deb.nodesource.com/setup_6.x | bash -
RUN apt-get install -y -q nodejs

# Set /code as workdir
WORKDIR /code