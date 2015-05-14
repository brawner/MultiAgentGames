//////////////////
//Action Handler//
//////////////////

var GeneralHandler = function() {
	"use strict";
	document.addEventListener("keydown", onKeyPress);
        //Start game
	var callbacks = [];
	var onKeyPress = function(event) {
		for (var i = 0; i < callbacks.length; i++) {
			callbacks[i].onKeyPress(event);
		}
	};

	var addCallback = function(callback) {
		callbacks.push(callback);
	};

	var removeCallback = function(callback) {
		callbacks.remove(callback);
	};
};