#!/usr/bin/env bash
set -e
source ~/.profile

TIMESTAMP=$(date +"%m_%d_%y__%M_%k")
mkdir -p "~/grid_games/server_out"

SERVER_OUT_DIR="~/grid_games/server_out/$TIMESTAMP"
mkdir -p $SERVER_OUT_DIR

PIDS_TO_KILL=""
PIDS_NOT_BEING_KILLED=""
while read -r line; do
	P_USER=$(echo $line | awk '{print $1}')
	PID=$(echo $line | awk '{print $2}')
	if [ "$P_USER" == "$USER" ] 
		then 
		PIDS_TO_KILL=$PIDS_TO_KILL" $PID"
	else 
		echo "User: $P_USER, Process: $PID"
		PIDS_NOT_BEING_KILLED=$PIDS_NOT_BEING_KILLED" $PID"
	fi
done < <(ps -f -C java |grep multi-agent-games.jar)

if [ -n "$PIDS_NOT_BEING_KILLED" ] 
	then
	echo "There are java processes run by another user that need to be killed."
	echo "Please kill them manually."
	echo "Offending PIDs:$PIDS_NOT_BEING_KILLED"
	exit 1
fi

if [ -n "$PIDS_TO_KILL" ]
	then
	echo "Killing your user processes:$PIDS_TO_KILL"
	kill $PIDS_TO_KILL
fi

echo "Starting java process"
nohup java -cp ~/workspace/MultiAgentGames/multi-agent-games.jar networking.server.GGWebSocketServer 2> "$SERVER_OUT_DIR/std.err" > "$SERVER_OUT_DIR/std.out"&
