#!/usr/bin/env bash
set -e
source ~/.profile

echo "Updating and building burlap"
cd ~/workspace/burlap
ant dist
cp dist/burlap.jar ../MultiAgentGames/lib/

echo "Updating and building MultiAgentGames"
cd ../MultiAgentGames
ant

echo "Syncing grid games files"
mkdir -p ~/grid_games/results

rsync -avz ./resources/ ~/grid_games/
rsync -avz ./web/ /var/www/multi_grid_games/

