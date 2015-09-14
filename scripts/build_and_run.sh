#!/usr/bin/env bash
set -e

cd ~/workspace/MultiAgentGames
bash scripts/build_java_server.sh

bash scripts/run_server.sh