#!/usr/bin/env bash
set -e
source ~/.profile

echo "Building burlap"
cd ~/workspace/burlap
ant dist
cp dist/burlap.jar ../norm_learning/lib/

echo "Building norm learning"
cd ~/workspace/norm_learning
ant dist
cp dist/burlap.jar ../MultiAgentGames/lib/


echo "Updating and building MultiAgentGames"
cd ../MultiAgentGames
ant
java -jar compiler.jar -O ADVANCED --js_output_file=web/all.js \
	--externs web/closure\ externs/jquery-1.9.js \
	--externs web/closure\ externs/underscore-1.5.2.js \
	--externs web/javascript/lib/raphael.js \
	--externs web/javascript/lib/tiny-pubsub.js \
	web/javascript/MessageFields.js \
	web/javascript/MessageReader.js \
	web/javascript/MessageWriter.js \
	web/javascript/clientMDP.js \
	web/javascript/config.js \
	web/javascript/connect.js \
	web/javascript/game.js \
	web/javascript/gridworld_painter.js \
	web/javascript/handler.js \
	web/javascript/objects.js \
	web/javascript/painter.js

echo "Syncing grid games files"
mkdir -p ~/grid_games/results
rsync -avz ./resources/ ~/grid_games/

echo "Copying website files"
cp ./web/index.html /var/www/multi_grid_games/index.html
cp ./web/task_ui.html /var/www/multi_grid_games/task_ui.html
mkdir -p /var/www/multi_grid_games/javascript/lib
cp -r ./web/javascript/* /var/www/multi_grid_games/javascript/
#cp ./web/all.js /var/www/multi_grid_games/javascript/
#cp ./web/javascript/lib/* /var/www/multi_grid_games/javascript/lib/

sudo chown $USER:www-data -R /var/www/multi_grid_games
sudo chmod 750 -R /var/www/multi_grid_games

