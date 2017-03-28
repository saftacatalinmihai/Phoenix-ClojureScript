#!/usr/bin/env bash

docker-compose up -d
docker-compose exec frontend lein figwheel