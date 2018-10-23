#!/bin/bash
# Script to tag the GIT repository with a specific version taken from the POM file

set -x

function slack {
  local SLACK_URL="https://hooks.slack.com/services/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";
  local PAYLOAD="payload={\"channel\": \"dice-opensource\", \"text\":\" $1 \", \"username\": \"Travis\", \"icon_url\": \"https://fst.slack-edge.com/66f9/img/services/travis_36.png\"}"
  curl -X POST --data-urlencode "$PAYLOAD" $SLACK_URL
}

# Get VERSION from top level POM
VERSION_POM=$( mvn help:evaluate -Dexpression=project.version | grep -v '\[.*' | tail -n1 )

# Get ARTIFACT_ID from top level POM
ARTIFACT_ID_POM=$( mvn help:evaluate -Dexpression=project.artifactId | grep -v '\[.*' | tail -n1 )

# Setup Git Configuration
git config --global user.email "build@travis-ci.com"
git config --global user.name "Travis CI"
git config core.sshCommand "ssh -i ./streaming-machine-user.pem -F /dev/null"

mvn scm:tag && slack "Tagged $ARTIFACT_ID_POM with version $VERSION_POM"
