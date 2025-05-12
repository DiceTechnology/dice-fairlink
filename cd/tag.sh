#!/bin/bash
# Script to tag the GIT repository with a specific version taken from the POM file

set -x

function slack {
  local PAYLOAD="{\"text\":\"$1\"}"
  echo Sending message to slack
  set +x
  curl -o /dev/null -s -w "%{http_code}\n" -X POST -H 'Content-type: application/json' --data "$PAYLOAD" $SLACK_URL
  set -x
}

# Get VERSION from top level POM
VERSION_POM=$( mvn help:evaluate -Dexpression=project.version | grep -v '\[.*' | tail -n1 )

# Get ARTIFACT_ID from top level POM
ARTIFACT_ID_POM=$( mvn help:evaluate -Dexpression=project.artifactId | grep -v '\[.*' | tail -n1 )

# Setup Git Configuration
git config --global user.email "build@dice.technology"
git config --global user.name "DiceTech CI"

git tag "${VERSION_POM}" -m "[GH] Released ${VERSION_POM}" 2>/dev/null && \
git push origin --tags 2>/dev/null && \
echo "Tagged $ARTIFACT_ID_POM with version $VERSION_POM" && \
slack "Tagged $ARTIFACT_ID_POM with version $VERSION_POM"
