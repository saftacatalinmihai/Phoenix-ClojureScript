#!/usr/bin/env bash

docker-compose up -d
docker-compose run backend mix ecto.create
docker-compose restart backend

docker-compose run --rm -p 3449:3449 frontend lein figwheel