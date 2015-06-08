
// Construct messages of different types. Again, doesn't really do much, but helps to be explicit.
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


	this.configurationMsg = function(label, agentDescriptions) {
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

	this.removeGameMsg = function(label) {
		var msg = {};
		msg[MessageFields.MSG_TYPE] = MessageFields.REMOVE_GAME;
		msg[MessageFields.WORLD_ID] = label;
		return msg;
	};
};