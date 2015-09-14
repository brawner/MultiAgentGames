#!/usr/bin/env bash
set -e

DEFAULT_SERVER="elzar"
BURLAP_BRANCH="multi_agent_games"
MAG_BRANCH="master"
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
ssh -t $SERVER "bash ~/workspace/MultiAgentGames/scripts/build_and_run.sh"