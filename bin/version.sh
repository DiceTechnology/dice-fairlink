#!/bin/bash
# Script to create a semver version as MAJOR.MINOR.PATCH (https://semver.org/) where:
#   - MAJOR.MINOR is taken from top level POM
#   - PATCH is different for each branch:
#       - master branch, will be based on latest tag like MAJOR.MINOR.PREVIOUS_PATCH and return PREVIOUS_PATCH + 1
#       - other branches, will be the Git Hash

#set -x

# Get PATCH from git latest tag starting with major.minor, otherwise 0
function getPatchFromGitTag {
    local VERSION_TAG=$( git tag -l "${MAJOR}.${MINOR}*" | sort -t . -k 3 -g | tail -n1 )
    local VERSION_TAG_BITS=(${VERSION_TAG//./ })

    local PATCH=0
    if [ ! -z "${VERSION_TAG_BITS[2]}" ]
    then
        PATCH=$((${VERSION_TAG_BITS[2]} + 1))
    fi

    echo "$PATCH"
}

function getPatchFromGitHash {
  local GIT_HASH=$( git rev-parse HEAD | cut -c 1-7 )
  local PATCH="0-$GIT_HASH"
  echo "$PATCH"
}

# Extract branch from Travis CI
if [[ "$TRAVIS_PULL_REQUEST" = "false" ]]
then
  BRANCH=$TRAVIS_BRANCH
else
  BRANCH=$TRAVIS_PULL_REQUEST_BRANCH
fi

# Get MAJOR and MINOR from top level POM
VERSION_POM=$( mvn help:evaluate -Dexpression=project.version | grep -v '\[.*' | tail -n1 )
VERSION_POM_BITS=(${VERSION_POM//./ })

MAJOR=${VERSION_POM_BITS[0]}
MINOR=${VERSION_POM_BITS[1]}

# Get PATCH depending on branch
if [[ "$BRANCH" = "master" ]]
then
    PATCH=$(getPatchFromGitTag)
else
    PATCH=$(getPatchFromGitHash)
fi

# Set the new version in POM
mvn versions:set -DgenerateBackupPoms=false -DnewVersion="${MAJOR}.${MINOR}.${PATCH}"
