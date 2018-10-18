#!/bin/bash
# Script to deploy using an AWX template for a specific version taken from the POM file

#set -x

function deploy {
    local env=$1
    local version=$2
    local component=$3

    NESTED_JSON="{ \"version\": \"$version\", \"app\": \"dve\", \"component\": \"$component\", \"env\": \"$env\" }"
    ESCAPED_NESTED_JSON=$(echo $NESTED_JSON | sed "s~\(['\"\/]\)~\\\\\1~g")
    BODY="{ \"extra_vars\": \"$ESCAPED_NESTED_JSON\" }"

    echo "curl -k -X POST \
        https://infrastructure-management.imggaming.com/api/v1/job_templates/XXXXX/launch/ \
        -H "authorization: Basic $TOWER_AUTH_STRING" \
        -H 'content-type: application/json' \
        -d "$BODY""
}

function slack {
  local SLACK_URL="https://hooks.slack.com/services/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";
  local PAYLOAD="payload={\"channel\": \"dev_streaming_notify\", \"text\":\" $1 \", \"username\": \"Travis\", \"icon_url\": \"https://fst.slack-edge.com/66f9/img/services/travis_36.png\"}"
  curl -X POST --data-urlencode "$PAYLOAD" $SLACK_URL
}

# Get VERSION from top level POM
VERSION_POM=$( ./mvnw help:evaluate -Dexpression=project.version | grep -v '\[.*' | tail -n1 )

# Get ARTIFACT_ID from top level POM
ARTIFACT_ID_POM=$( ./mvnw help:evaluate -Dexpression=project.artifactId | grep -v '\[.*' | tail -n1 )

deploy prod $VERSION_POM $ARTIFACT_ID_POM && slack "Deployed $ARTIFACT_ID_POM with version $VERSION_POM"
