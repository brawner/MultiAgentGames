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
java -jar compiler.jar -O SIMPLE --js_output_file=web/all.js 'web/javascript/**.js'

echo "Syncing grid games files"
mkdir -p ~/grid_games/results

rsync -avz ./resources/ ~/grid_games/
cp ./web/index_with_closure.html /var/www/multi_grid_games/index.html
cp ./web/task_ui_with_closure.html /var/www/multi_grid_games/task_ui.html
cp ./web/all.js /var/www/multi_grid_games/index.html
rsync -avz ./web/ /var/www/multi_grid_games/
sudo chown $USER:www-data -R /var/www/multi_grid_games
sudo chmod 750 -R /var/www/multi_grid_games

