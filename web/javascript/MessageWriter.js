var MessageWriter = function() {
	"use strict";
	var self = this;

	this.startGameMsg = function(label, id) {
		var msg = {};
		msg[MessageFields.MSG_TYPE] = MessageFields.JOIN_GAME;
		msg[MessageFields.WORLD_ID] = label;
		msg[MessageFields.CLIENT_ID] = id;
		return msg;
	}
};