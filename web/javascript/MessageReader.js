
// Allows of reading from a a json message. Really, it doesn't do much, just helps being explicit.

/**
 * @constructor
 */
var MessageReader = function() {
	"use strict";
	var self = this;
	this.getMessageType = function(message) {
		var msgType;
		if (MessageFields.MSG_TYPE in message) {
			msgType = message[MessageFields.MSG_TYPE];
		}
		
		return msgType;
	};

	this.getClientId = function(message) {
		var clientId;
		if (MessageFields.CLIENT_ID in message) {
			clientId = message[MessageFields.CLIENT_ID];
		}
		return clientId;
	};

	this.getInitializationMsg = function(message) {
		var initializationMsg;
		if (!(MessageFields.AGENT in message) ||
			!(MessageFields.STATE in message) ||
			!(MessageFields.WORLD_TYPE in message)) {
			return initializationMsg;
		}
		initializationMsg = {};
		initializationMsg.agent_name = message[MessageFields.AGENT];
		initializationMsg.state = self.getStateFromMessage(message[MessageFields.STATE]);
		initializationMsg.world_type = message[MessageFields.WORLD_TYPE];
		initializationMsg.ready = message[MessageFields.IS_READY];
		return initializationMsg;

	}

	this.getUpdateMsg = function(message) {
		var updateMsg;
		
		if (!(MessageFields.UPDATE in message)) {
			return updateMsg;
		}

		var update = message[MessageFields.UPDATE];
		if (!(MessageFields.STATE in update)) {
			return updateMsg;
		}

		updateMsg = {};
		if (MessageFields.SCORE in update) {
			updateMsg.score = update[MessageFields.SCORE];
		}
		if (MessageFields.ACTION in update){
				
			console.log("Joint actions:");
			console.log(update[MessageFields.ACTION][MessageFields.JOINT_ACTION]);
			updateMsg.action = update[MessageFields.ACTION][MessageFields.JOINT_ACTION];

		} 
		//if (MessageFields.IS_TERMINAL in update){
			//updateMsg.is_terminal = update[MessageFields.IS_TERMINAL];
		//}
		updateMsg.state = self.getStateFromMessage(update[MessageFields.STATE]);
		return updateMsg;
	};

	this.getCloseMsg = function(message) {
		var closeMsg;
		if (!(MessageFields.SCORE in message)) {
			return closeMsg;
		}

		closeMsg = {};
		closeMsg.score = message[MessageFields.SCORE];
		return closeMsg;
	};


	var ObjectsToIgnore = [
		"counter","shelf"
	];


	// TODO I think this method would be more efficient if getObjects method return dictionaries instead of lists
	this.getStateFromMessage = function(state) {

		var agents = getObjectsOfTypeFromMessage(state, MessageFields.AGENT, getAgentFromMessage);
		var goals = getObjectsOfTypeFromMessage(state, MessageFields.GOAL, getGoalFromMessage);
		
		var walls = [];
		var horizontal_walls = getObjectsOfTypeFromMessage(state, MessageFields.HORIZONTAL_WALL, getHorizontalWallFromMessage);
		var vertical_walls = getObjectsOfTypeFromMessage(state, MessageFields.VERTICAL_WALL, getVerticalWallFromMessage);
		walls = walls.concat(horizontal_walls, vertical_walls);

		var tolls = getObjectsOfTypeFromMessage(state, MessageFields.REWARD, getTollFromMessage);
		var s = new State(agents, goals, walls, tolls);

		console.log("State %O", s);
		return s;
	};

	var getObjectsOfTypeFromMessage = function(state, fieldName, parserFn) {
		var objects = [];
		
		for (var i = 0; i < state.length; i++) {
			if (state[i]["class"] === fieldName) {
				var obj = parserFn(state[i]);
				if (typeof obj !== 'undefined' && obj !== null) {
					objects.push(obj);
				} else {
					console.log("Could not parse %O", state[i]);
				}
			}
		}

		return objects;
	};

	var getAgentFromMessage = function(msg) {
		var agent;
		if (!(MessageFields.NAME in msg) ||
			!(MessageFields.PLAYER_NUMBER in msg) ||
			!(MessageFields.ATTX in msg) ||
			!(MessageFields.ATTY in msg)) {
			return agent;
		}

		var name = msg[MessageFields.NAME];
		var num = msg[MessageFields.PLAYER_NUMBER];
		var x = msg[MessageFields.ATTX];
		var y = msg[MessageFields.ATTY];

		agent = new Agent(name, num, x, y);
		return agent;
	};

	var getGoalFromMessage = function(msg) {
		var goal;
		if (!(MessageFields.NAME in msg) ||
			!(MessageFields.GOAL_TYPE in msg) ||
			!(MessageFields.ATTX in msg) ||
			!(MessageFields.ATTY in msg)) {
			return goal;
		}

		var name = msg[MessageFields.NAME];
		var goalType = msg[MessageFields.GOAL_TYPE];
		var x = msg[MessageFields.ATTX];
		var y = msg[MessageFields.ATTY];

		goal = new Goal(name, goalType, x, y);
		return goal;
	};

	var getHorizontalWallFromMessage = function(msg) {
		var wall;
		if (!(MessageFields.NAME in msg) ||
			!(MessageFields.WALL_TYPE in msg) ||
			!(MessageFields.ATTP in msg) ||
			!(MessageFields.ATTE1 in msg) ||
			!(MessageFields.ATTE2 in msg)) {
			return wall;
		}

		var name = msg[MessageFields.NAME];
		var wallType = msg[MessageFields.WALL_TYPE];
		var p = msg[MessageFields.ATTP];
		var e1 = msg[MessageFields.ATTE1];
		var e2 = msg[MessageFields.ATTE2];



		wall = new Wall(name, wallType, e1, p, e2 + 1, p);
		return wall;
	};

	var getVerticalWallFromMessage = function(msg) {
		var wall;
		if (!(MessageFields.NAME in msg) ||
			!(MessageFields.WALL_TYPE in msg) ||
			!(MessageFields.ATTP in msg) ||
			!(MessageFields.ATTE1 in msg) ||
			!(MessageFields.ATTE2 in msg)) {
			return wall;
		}

		var name = msg[MessageFields.NAME];
		var wallType = msg[MessageFields.WALL_TYPE];
		var p = msg[MessageFields.ATTP];
		var e1 = msg[MessageFields.ATTE1];
		var e2 = msg[MessageFields.ATTE2];



		wall = new Wall(name, wallType, p, e1, p, e2 + 1);
		return wall;
	};

	var getTollFromMessage = function(msg) {
		var toll;
		if (!(MessageFields.NAME in msg) ||
			!(MessageFields.COST in msg) ||
			!(MessageFields.ATTX in msg) ||
			!(MessageFields.ATTY in msg)) {
			return toll;
		}

		var name = msg[MessageFields.NAME];
		var cost = msg[MessageFields.COST];
		var x = msg[MessageFields.ATTX];
		var y = msg[MessageFields.ATTY];

		toll = new Toll(name, cost, x, y);
		return toll;
	};

	this.getWorlds = function(msg) {
		var worlds = [];
		if (!(MessageFields.WORLDS in msg)) {
			return worlds;
		}
		var worldMsgs = msg[MessageFields.WORLDS];
		for (var i = 0; i < worldMsgs.length; i++) {
			var world = getWorld(worldMsgs[i]);
			if (typeof world !== 'undefined') {
				worlds.push(world);
			}
		}

		return worlds;
	};

	var getWorld = function(worldMsg) {
		var world;
		if (!(MessageFields.AGENTS in worldMsg) ||
			!(MessageFields.LABEL in worldMsg) ||
			!(MessageFields.DESCRIPTION in worldMsg)) {
			return world;
		}

		var label = worldMsg[MessageFields.LABEL];
		var description = worldMsg[MessageFields.DESCRIPTION];
		var maxAgents = worldMsg[MessageFields.AGENTS].length;
		return new World(label, description, maxAgents);
	};

	this.getActiveWorlds = function(msg) {
		var actives = [];
		if (!(MessageFields.ACTIVE in msg)) {
			return actives;
		}
		var activeMsgs = msg[MessageFields.ACTIVE];
		for (var i = 0; i < activeMsgs.length; i++) {
			var label = activeMsgs[i][MessageFields.LABEL];
			var description = activeMsgs[i][MessageFields.DESCRIPTION];
			var agents = activeMsgs[i][MessageFields.AGENTS];
			var maxAgents = activeMsgs[i][MessageFields.NUM_AGENTS];
			var active = new ActiveWorld(label, description, agents, maxAgents);
			actives.push(active);
		}

		return actives;
	};

	this.getAgentTypes = function(msg) {
		var agents;
		if (!(MessageFields.AGENTS in msg)) {
			return agents;
		}

		agents = msg[MessageFields.AGENTS];
		return agents;
	};
};