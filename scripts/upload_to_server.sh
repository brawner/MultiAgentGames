#!/usr/bin/env bash
set -e

DEFAULT_SERVER="elzar"
BURLAP_BRANCH="mag_v2"
MAG_BRANCH="burlap_v2"
SERVER=${1:-$DEFAULT_SERVER}

kinit
cd ~/workspace/burlap
#maybe check which branch and if it's not correct, then checkout and pull
git checkout $BURLAP_BRANCH
git pull
rsync -avz --exclude '.git' ./ $SERVER:~/workspace/burlap
cd ~/workspace/MultiAgentGames
git checkout $MAG_BRANCH
git pull
rsync -avz --exclude '.git' ./ $SERVER:~/workspace/MultiAgentGames
ssh -t $SERVER "~/workspace/MultiAgentGames/scripts/build_and_run.sh"
