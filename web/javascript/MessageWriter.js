
// Construct messages of different types. Again, doesn't really do much, but helps to be explicit.
/**
 * @constructor
 */
var MessageWriter = function() {
	"use strict";
	var self = this;

	this.startGameMsg = function(label, id) {
		var msg = {};
		msg[MessageFields.MSG_TYPE] = MessageFields.JOIN_GAME;
		msg[MessageFields.WORLD_ID] = label;
		msg[MessageFields.CLIENT_ID] = id;
		return msg;
	};

	this.log_reaction_time = function(client_id, agent_name, t_id, rt, game_number, action_number){
		var msg = {};
		msg[MessageFields.MSG_TYPE] = MessageFields.REACTION_TIME;
		msg[MessageFields.CLIENT_ID] = client_id;
		msg[MessageFields.AGENT_NAME] = agent_name;
		msg[MessageFields.URL_ID] = t_id;
		msg[MessageFields.REACTION_TIME] = rt;
		msg[MessageFields.GAME_NUMBER] = game_number;
		msg[MessageFields.ACTION_NUMBER] = action_number;
		return msg;
	}

	this.startGameURLMsg = function(label, id, url_client) {
		var msg = {};
		msg[MessageFields.MSG_TYPE] = MessageFields.JOIN_GAME;
		msg[MessageFields.WORLD_ID] = label;
		msg[MessageFields.CLIENT_ID] = id;
		msg[MessageFields.URL_ID] = url_client;
		return msg;
	};

	this.updateActionMsg = function(id, action, agentName) {
		var msg = {};
		msg[MessageFields.MSG_TYPE] = MessageFields.TAKE_ACTION;
		msg[MessageFields.ACTION] = action;
		msg[MessageFields.ACTION_PARAMS] = [agentName];
		return msg;
	};

	this.initializeGameMsg = function(label) {
		var msg = {};
		msg[MessageFields.MSG_TYPE] = MessageFields.INITIALIZE_GAME;
		msg[MessageFields.WORLD_ID] = label;
		return msg;
	};


	this.configurationMsg = function(label, agentDescriptions) {;
		var msg = {};
		msg[MessageFields.MSG_TYPE] = MessageFields.CONFIG_GAME;
		msg[MessageFields.WORLD_ID] = label;
		msg[MessageFields.AGENTS] = agentDescriptions;
		return msg;
	};

	this.runGameMsg = function(label) {
		var msg = {};
		msg[MessageFields.MSG_TYPE] = MessageFields.RUN_GAME;
		msg[MessageFields.WORLD_ID] = label;
		return msg;
	};

	this.urlJoinMsg = function(label,agentDescriptions,url_client_id,exp_name, numPlayers, vars){
		
		var msg = {};
		msg[MessageFields.MSG_TYPE] = MessageFields.RUN_URL_GAME;
		msg[MessageFields.WORLD_ID] = label;
		msg[MessageFields.AGENTS] = agentDescriptions;
		msg[MessageFields.AGENT_TYPE] = "human";
		msg[MessageFields.URL_ID] = url_client_id;
		msg[MessageFields.NUM_PLAYERS] = numPlayers;
		msg[MessageFields.EXP_NAME] = exp_name;

		var other_vars = {}
		for (var key in vars) {
			if (key in msg) {
				continue;
			}
			other_vars[key] = vars[key];
		}

		msg[MessageFields.OTHER_VARS] = other_vars;

		return msg;


	};

	this.removeGameMsg = function(label) {
		var msg = {};
		msg[MessageFields.MSG_TYPE] = MessageFields.REMOVE_GAME;
		msg[MessageFields.WORLD_ID] = label;

		return msg;
	};

	this.surveyResponseMsg = function(humanOrComputer) {
		var msg = {};
		msg[MessageFields.MSG_TYPE] = MessageFields.SURVEY_RESPONSE;
		msg[MessageFields.OPPONENT_TYPE] = humanOrComputer;
		return msg;
	};
};