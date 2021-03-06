#!/usr/bin/env bash
set -e

ENVIRONMENT=${ENVIRONMENT:-alpha}

function on_exit {
  echo "Stopping and removing containers..."
  docker-compose --file docker-compose.register.yml down
  docker-compose --file docker-compose.basic.yml down
  exit
}

function wait_for_http_on_port {
  while ! curl ":$1" --silent --fail --output /dev/null;
  do
    echo "Waiting for HTTP on :$1"
    sleep 1
  done
}

function do_nothing_forever {
  tail -f /dev/null
}

trap on_exit EXIT

if [ ! -e "./deploy/openregister-java.jar" ]
then
  docker run \
    --rm \
    --volume "$PWD":/usr/src/openregister-java \
    --workdir /usr/src/openregister-java \
    openjdk:8 \
      bash -c "./gradlew assemble"
fi

echo "Starting environment based off \"$ENVIRONMENT\""
echo "Starting basic registers..."
docker-compose --file docker-compose.basic.yml up -d
wait_for_http_on_port 8081

for register in "register" "datatype" "field"; do
  echo "Loading $register..."
  curl \
    --fail \
    --header "Content-Type: application/uk-gov-rsf" \
    --header "Host: $register" \
    --data-binary @<(curl "https://$register.$ENVIRONMENT.openregister.org/download-rsf") \
    --user foo:bar \
    "http://localhost:8081/load-rsf"
done

echo "Starting register..."
docker-compose --file docker-compose.register.yml up -d
wait_for_http_on_port 8080

echo "Loading school-eng data..."
curl \
  --fail \
  --header "Content-type: application/uk-gov-rsf" \
  --data-binary @<(curl "https://school-eng.alpha.openregister.org/download-rsf") \
  --user foo:bar \
  "http://localhost:8080/load-rsf"

echo "Register is ready on http://localhost:8080"
do_nothing_forever
