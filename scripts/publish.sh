#!/bin/bash

set -ex

./gradlew --no-daemon publishAndReleaseToMavenCentral --no-configuration-cache