//////////////////
//Action Handler//
//////////////////

// Handles all key and button interactions for the game. If you want to add different key interactions, do it here.
// shouldn't be to hard to map arrows or other keys to the actions.
var GeneralHandler = function(_actions) {
	"use strict";
	    //Start game
	var actions = _actions;
	var keyLookup = {65:"West", 87:"North", 83:"South", 68:"East", 81:"Wait"};
	var actionCallbacks = [];
	var interactionCallbacks = [];
	
	var onKeyPress = function(event) {
		var key = event.keyCode;
		if (typeof key === 'undefined') {
			return;
		}
		console.log("key: " + key);
		//key = key.toLowerCase();
		if (key in keyLookup) {
			for (var i = 0; i < actionCallbacks.length; i++) {
				actionCallbacks[i](keyLookup[key]);
			}
		} else {
			for (var i = 0; i < interactionCallbacks.length; i++) {
				interactionCallbacks[i](key);
			}
		}
		
	};
	document.addEventListener("keydown", onKeyPress);
    

	var onButtonPress = function(button) {
		if (button in actions) {
			runActionCallbacks(button);
		} else {
			runInteractionCallbacks(button);
		}
	};

	
	this.addActionCallback = function(callback) {
		actionCallbacks.push(callback);
	};

	this.addInteractionCallback = function(callback) {
		interactionCallbacks.push(callback);
	};

	this.removeCallback = function(callback) {
		if (callback in actionCallbacks) {
			actionCallbacks.remove(callback);
		} else if (callback in interactionCallbacks) {
			interactionCallbacks.remove(callback);
		}
	};

	var runActionCallbacks = function(event) {
		console.log("Action: " + event);
		for (var i = 0; i < actionCallbacks.length; i++) {
			actionCallbacks[i](event);
		}
	};

	var runInteractionCallbacks = function(event) {
		for (var i = 0; i < interactionCallbacks.length; i++) {
			interactionCallbacks[i](event);
		}
	};

	this.registerWithPainter = function(painter) {
		painter.addButtonCallback(onButtonPress);
	};
};