#!/usr/bin/env bash
set -e

cd ~/workspace/burlap
ant dist
cp dist/burlap.jar ../MultiAgentGames/lib/
cd ../MultiAgentGames
ant

PIDS_TO_KILL=""
PIDS_NOT_BEING_KILLED=""
ps -f -C java |grep multi-agent-games.jar |while read -r line; do
	P_USER=line | awk '{print $1}'
	PID=line | awk '{print $2}'
	if [ "$P_USER" != "$USER" ] 
		then; PIDS_NOT_BEING_KILLED=$PIDS_NOT_BEING_KILLED" $PID";
		else; PIDS_TO_KILL=$PIDS_TO_KILL" $PID"; 
	fi
if [ -z "$PIDS_NOT_BEING_KILLED" ] 
	then; 
	echo "There are java processes run by another user that need to be killed."
	echo "Please kill them manually."
	echo "Offending PIDs: $PIDS_NOT_BEING_KILLED"
	exit 1
fi

if [-z "$PIDS_TO_KILL" ]
	then;
	echo "Killing your user processes: $PIDS_TO_KILL"
	kill PIDS_TO_KILL
fi

echo "Staring java process"
nohup java multi-agent-games.jar networking.server.GGWebSocketServer &