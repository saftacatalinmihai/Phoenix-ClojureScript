#!/usr/bin/env bash

docker-compose up -d
docker-compose run --rm -p 3449:3449 frontend lein figwheel