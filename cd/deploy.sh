#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    gpg --batch --fast-import gpg.asc
    mvn deploy -P publish --settings cd/mvnsettings.xml
fi
