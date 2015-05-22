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

	this.updateActionMsg = function(id, action, agentName) {
		var msg = {};
		msg[MessageFields.MSG_TYPE] = MessageFields.TAKE_ACTION;
		msg[MessageFields.ACTION] = action;
		msg[MessageFields.ACTION_PARAMS] = [agentName];
		return msg;
	};
};