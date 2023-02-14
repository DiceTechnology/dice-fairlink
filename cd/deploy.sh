#!/usr/bin/env bash

set +x
echo $GPG_KEY | base64 --decode | gpg --batch --fast-import
set -x

./cd/version.sh && \
./cd/tag.sh && \
mvn deploy -P publish -DskipTests=true --settings cd/mvnsettings.xml
