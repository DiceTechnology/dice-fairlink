#!/bin/bash
# Script to publish the build artifacts to S3 for a specific version taken from the POM file

#set -x

function slack {
  local SLACK_URL="https://hooks.slack.com/services/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";
  local PAYLOAD="payload={\"channel\": \"dev_streaming_notify\", \"text\":\" $1 \", \"username\": \"Travis\", \"icon_url\": \"https://fst.slack-edge.com/66f9/img/services/travis_36.png\"}"
  curl -X POST --data-urlencode "$PAYLOAD" $SLACK_URL
}

# Only publish for PRs and master branch
if [[ $TRAVIS_PULL_REQUEST != false ]] || [[ "$TRAVIS_BRANCH" = "master" ]]
then
  # Get VERSION from top level POM
  VERSION_POM=$( ./mvnm help:evaluate -Dexpression=project.version | grep -v '\[.*' | tail -n1 )

  # Get ARTIFACT_ID from top level POM
  ARTIFACT_ID_POM=$( ./mvnm help:evaluate -Dexpression=project.artifactId | grep -v '\[.*' | tail -n1 )

  ./mvnm deploy -DskipTests && slack "Published $ARTIFACT_ID_POM $VERSION_POM"
fi
