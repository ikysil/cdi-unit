#!/usr/bin/env bash

set -ex

export MAVEN_ARGS="-V -B --settings .github/mvn-settings.xml --no-transfer-progress"

./mvnw -Dno-format dependency:go-offline
./mvnw -Dno-format -DskipTests package
