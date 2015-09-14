#!/usr/bin/env bash
set -e
source ~/.profile

cd ~/workspace/burlap
ant dist
cp dist/burlap.jar ../MultiAgentGames/lib/
cd ../MultiAgentGames
ant