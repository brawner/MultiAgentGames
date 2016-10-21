/**
 * @constructor
 */
var GridWorldPainter = function (gridworld) {
	"use strict";
	this.TILE_SIZE = 100;
	this.COLORS = ['blue', 'green', 'red'];
	this.PRIMARY_AGENT_COLOR = 'orange';
	this.AGENT_WIDTH = this.TILE_SIZE*.3;
	this.WALL_STROKE_SIZE = 10;
	this.ACTION_ANIMATION_TIME = 150;
	this.CONTRACT_SIZE = .9;
	this.SUBACTION_PROPORTION = .2;
	this.SUBACTION_DISTANCE = .2;
	this.SUBACTION_COLLISION_PROPORTION = .7;
	this.SUBACTION_COLLISION_DISTANCE = .7;
	this.REWARD_DISPLAY_TIME = 800;

	this.gridworld = gridworld;
	this.animationTimeouts = [];
	this.objectImages = {};
	this.currentAnimations = {};
}

GridWorldPainter.prototype.init = function (container, this_agent_name, tile_click_cb) {
	"use strict";
	this.container = container;
	this_agent_name = typeof this_agent_name !== 'undefined' ? this_agent_name : 'agent1';
	this.width = this.TILE_SIZE*this.gridworld.width;
	this.height = this.TILE_SIZE*this.gridworld.height;
	if (typeof this.paper === 'undefined') {
		this.paper = Raphael(container, this.width, this.height);
	} else {
		this.paper.setSize(this.width, this.height);
	}

	//assign colors to agents
	this.AGENT_COLORS = {};
	var j = 0;
	for (var i = 0; i < this.gridworld.agents.length; i++){
		if(this.gridworld.agents[i].name==this_agent_name){
			this.AGENT_COLORS[this.gridworld.agents[i].name] = this.PRIMARY_AGENT_COLOR;
		}else{
			this.AGENT_COLORS[this.gridworld.agents[i].name] = this.COLORS[j];
			j++;
		}
	}

	this.walls = {};
	this.goals = {};
	this.drawTilesGoals2DWalls(tile_click_cb);
	this.draw1DWalls.apply(this);
}

GridWorldPainter.prototype.convertOffsetToGrid = function(px, py) {
	return {x:px / this.TILE_SIZE, y:py / this.TILE_SIZE}
}

GridWorldPainter.prototype.drawTilesGoals2DWalls = function(cb) {
	//draw tiles
	for (var x = 0; x < this.gridworld.width; x++) {
		for (var y = 0; y < this.gridworld.height; y++) {
			var tile_params = {
				type : 'rect',
				x : x*this.TILE_SIZE,
				y : this.height - (y+1)*this.TILE_SIZE,
				width : this.TILE_SIZE,
				height : this.TILE_SIZE,
				stroke : 'black',
				fill : 'white'
			};

			//goals
			for (var g = 0; g < this.gridworld.goals.length; g++) {
				var goal = this.gridworld.goals[g];
				this.goals[goal.location] = goal; //store locations of all goals for animations
				if (String(goal.location) === String([x,y])) {
					tile_params.fill = this.AGENT_COLORS[goal.agent];
				}
			}
			//walls
			for (var w = 0; w < this.gridworld.walls.length; w++) {
				var wall = this.gridworld.walls[w];
				if (String([wall[0], wall[1]]) == String([x,y])) {
					this.walls[wall] = 1; //store locations of all walls for animations
					if (wall.length == 2) {
						tile_params.fill = 'black';
					}
				}
			}

			var tileWidth = this.TILE_SIZE;
			var gwHeight = this.gridworld.height - 1;
			var tile = this.paper.add([tile_params])[0];
			tile.attr({
			    cursor: 'pointer',
			}).mouseup(function(e) {
				var px = Math.round(e.offsetX / tileWidth - 0.5);
			    var py = gwHeight - Math.round(e.offsetY / tileWidth - 0.5);
			    console.log("Click " + px + " " + py);
			    cb(px,py);
			}); 
		}
	}
}

GridWorldPainter.prototype.draw1DWalls = function() {
for (var x = 0; x < this.gridworld.width; x++) {
		for (var y = 0; y < this.gridworld.height; y++) {

			for (var w = 0; w < this.gridworld.walls.length; w++) {
				var wall = this.gridworld.walls[w];
				if (String([wall[0], wall[1]]) == String([x,y])) {
					this.walls[wall] = 1; //store locations of all walls for animations
					if (wall.length == 2) {
						//tile_params.fill = 'black';
						continue
					}
					else if (wall.length == 3) {
						var from, to;
						switch (wall[2]) {
							case 'up':
								from = [0, 1];
								to = [1, 1];
								break
							case 'down':
								from = [0,0];
								to = [1, 0];
								break
							case 'left':
								from = [0,0];
								to = [0, 1];
								break
							case 'right':
								from = [1, 0];
								to = [1, 1];
								break
						}
						from = [(x+from[0])*this.TILE_SIZE, (this.gridworld.height - (from[1] + y))*this.TILE_SIZE];
						to = [(x+to[0])*this.TILE_SIZE, (this.gridworld.height - (to[1] + y))*this.TILE_SIZE];

						var wall_path = 'M '+from.join(',') + ' L '+to.join(',');
						console.log(wall_path);
						var wall_img = this.paper.path(wall_path);
						wall_img.attr({"stroke-width" : this.WALL_STROKE_SIZE, stroke : 'black'});
					}
				}
			}
		}
	}

}

GridWorldPainter.prototype.clear = function() {
	this.paper.clear();
}

GridWorldPainter.prototype.reset = function(gridworld, container, agent_name, tile_click_cb) {
	this.gridworld = gridworld;
	this.animationTimeouts = [];
	this.objectImages = {};
	this.currentAnimations = {};
	this.paper.clear();
	if (typeof container.empty !== 'undefined' && typeof(container.empty) == typeof(Function)) {
		container.empty();
	}
	this.init(container, agent_name, tile_click_cb);
}

GridWorldPainter.prototype.drawState = function (state) {
	"use strict";
	//paper.getPaper(this.canvas);
	for (var object in state) {
		if (!state.hasOwnProperty(object)) {
			continue
		}
		object = state[object];

		//draw agents
		if (object.type == 'agent') {
			var agent_params = {cx : (object.location[0] + .5)*this.TILE_SIZE, 
				                cy : (this.gridworld.height - object.location[1] - .5)*this.TILE_SIZE};
			if (this.objectImages.hasOwnProperty(object.name)) {
				this.objectImages[object.name].attr(agent_params);
			}
			else {
				agent_params.type = 'circle';
				agent_params.fill = this.AGENT_COLORS[object.name];
				agent_params.r = this.AGENT_WIDTH;
				agent_params.stroke = 'white';
				agent_params['stroke-width'] = 1;

				var objectImage = this.paper.add([agent_params])[0];
				this.objectImages[object.name] = objectImage;
			}
		}
	}
}

//cases:
//normal move
//wait
//hit wall
//hit edge of world
//hit another agent (1) who waits; (2) who is also moving to the same tile

GridWorldPainter.prototype.drawTransition = function (state, action, nextState, mdp, goal_callback) {
 	"use strict";
	console.log('------------')
	this.drawState(state);
	var animation_time = 0;

	var intendedLocation = {};
	for (var agent in state) {
		if (state[agent].type !== 'agent') {
			continue
		}
		if (typeof mdp == 'undefined') {
			intendedLocation[agent] = nextState[agent];
		}
		else {
			intendedLocation[agent] = mdp.getNextIntendedLocation(state[agent].location, action[agent]);
		}
	}

	//draw movements for each agent
	for (var agent in state) {
		if (!state.hasOwnProperty(agent) || state[agent].type !== 'agent') {
			continue
		}
		//waiting
		if (action[agent] == 'wait') {
			console.log(agent + ' waits');
			this.drawWaiting(agent);
		}
		//try and fail
		else if (String(intendedLocation[agent]) !== String(nextState[agent].location)) {
			console.log(agent +' tried and failed')

			//distance to try depends on failure condition
			//1 - hit a wall or just hit another agent waiting
			var SUBACTION_DISTANCE = this.SUBACTION_DISTANCE;
			var SUBACTION_PROPORTION = this.SUBACTION_PROPORTION;
			for (var otherAgent in state){
				if (!state.hasOwnProperty(otherAgent) || state[otherAgent].type !== 'agent' || agent == otherAgent) {
					continue
				}

				//2 - 2 agents try to get into the same spot
				if ((String(intendedLocation[agent]) == String(nextState[otherAgent].location) ||
					String(intendedLocation[agent]) == String(intendedLocation[otherAgent])) 
					&& 
					!(String(intendedLocation[agent]) == String(nextState[otherAgent].location) &&
					String(intendedLocation[otherAgent]) == String(nextState[agent].location))) {
					console.log(agent +' collided with ' + otherAgent)
					SUBACTION_DISTANCE = this.SUBACTION_COLLISION_DISTANCE;
					SUBACTION_PROPORTION = this.SUBACTION_COLLISION_PROPORTION;
				}
			}

			this.drawTryAndFail(agent, intendedLocation, state, SUBACTION_PROPORTION, SUBACTION_DISTANCE);
		}
		//normal movement
		else {
			console.log(agent + ' makes normal movement');
			console.log("object images %O", this.objectImages);
			var movement = Raphael.animation({cx : (nextState[agent].location[0] + .5)*this.TILE_SIZE,
											  cy : (this.gridworld.height - nextState[agent].location[1] - .5)*this.TILE_SIZE}, 
											 this.ACTION_ANIMATION_TIME, 'easeInOut');
			this.objectImages[agent].animate(movement);
			//this.drawState(nextState);

			$.subscribe('killtimers', this.makeTimerKiller(this.objectImages[agent], movement));
		}

		if (typeof this.objectImages['nextMove'] !== 'undefined') {
			this.objectImages['nextMove'].remove();
			delete this.objectImages['nextMove'];
		}
	}
	animation_time += this.ACTION_ANIMATION_TIME;

	//draw rewards (if there are any)
	if (typeof goal_callback === 'undefined') {
		goal_callback = function (painter, location, agent) {
			painter.showReward(location, agent, 'Home')
		}
	}
	var reward_time = false;
	for (var agent in state) {
		if (typeof mdp === 'undefined') {
			break
		}
		var goal_animation = (function (painter, location, agent) {
			return function () {
				goal_callback(painter, location, agent);
			}
		})(this, nextState[agent].location, agent)
		if (mdp.inGoal(nextState[agent].location, agent)) {
			var th = setTimeout(goal_animation, this.ACTION_ANIMATION_TIME);
			$.subscribe('killtimers', (function (th) {
					return function () {clearTimeout(th)}
				})(th)
			)
			reward_time = true;
		}
	}

	if (reward_time) {
		animation_time += this.REWARD_DISPLAY_TIME;
	}

	return animation_time
}

GridWorldPainter.prototype.showReward = function (loc, agent, text) {
	"use strict";
	var params = {type : 'text',
							 x : (loc[0] + .5)*this.TILE_SIZE, 
							 y : (this.gridworld.height - loc[1] - .5)*this.TILE_SIZE, 
							 text : text,
							 "font-size" : 40,
							 stroke : this.AGENT_COLORS[agent],
							 fill : 'yellow'};
	var r = this.paper.add([params])[0];
	var r_animate = Raphael.animation({y : r.attr("y") - .5*this.TILE_SIZE, opacity : 0}, this.REWARD_DISPLAY_TIME)
	
	r.animate(r_animate);
	$.subscribe('killtimers', this.makeTimerKiller(r, r_animate))
}

GridWorldPainter.prototype.drawWaiting = function (agent) {
	"use strict";
	var expand = (
		function (painter, agentImage, original_size, time) {
			return function () {
				var anim = Raphael.animation({r : original_size}, time,	'backOut');
				agentImage.animate(anim);
				$.subscribe('killtimers', painter.makeTimerKiller(agentImage, anim))
			}
		})(this, this.objectImages[agent], this.objectImages[agent].attr('r'), this.ACTION_ANIMATION_TIME*.5);

	var contract = Raphael.animation({r : this.objectImages[agent].attr('r')*this.CONTRACT_SIZE}, this.ACTION_ANIMATION_TIME*.5, 
										'backIn', expand);
	this.objectImages[agent].animate(contract);

	//attach to killtimer
	$.subscribe('killtimers', this.makeTimerKiller(this.objectImages[agent], contract))
}

GridWorldPainter.prototype.drawTryAndFail = function (agent, intendedLocation, state, SUBACTION_PROPORTION, SUBACTION_DISTANCE) {
	"use strict";
	var moveBack = (
		function (painter, agentImage, original_x, original_y, time) {
			return function () {
				var anim = Raphael.animation({cx : original_x, cy: original_y}, time, 'backOut')
				agentImage.animate(anim);
				$.subscribe('killtimers', painter.makeTimerKiller(agentImage, anim))
			}
		})(this, this.objectImages[agent], this.objectImages[agent].attr('cx'), this.objectImages[agent].attr('cy'), 
			this.ACTION_ANIMATION_TIME*SUBACTION_PROPORTION)

	var new_x = (intendedLocation[agent][0]*SUBACTION_DISTANCE) + 
				(state[agent].location[0]*(1 - SUBACTION_DISTANCE));

	var new_y = ((this.gridworld.height - intendedLocation[agent][1])*SUBACTION_DISTANCE)
				+ (this.gridworld.height - state[agent].location[1])*(1 - SUBACTION_DISTANCE);

	new_x = (new_x + .5)*this.TILE_SIZE;
	new_y = (new_y - .5)*this.TILE_SIZE;

	var moveToward = Raphael.animation({cx : new_x, cy : new_y},
										this.ACTION_ANIMATION_TIME*(1-SUBACTION_PROPORTION), 'backIn',
										moveBack);

	this.objectImages[agent].animate(moveToward);
	$.subscribe('killtimers', this.makeTimerKiller(this.objectImages[agent], moveToward));
}

GridWorldPainter.prototype.makeTimerKiller = function (element, animation) {
	"use strict";
		return function () {
			element.stop(animation);
		}
	} 

GridWorldPainter.prototype.remove = function () {
	"use strict";
	this.paper.remove();
}

GridWorldPainter.prototype.draw_score = function () {
	"use strict";
	var scoreboard_params = {
		type : 'rect',
		x : 0,
		y : 0,
		width : this.width,
		height : this.height,
		stroke : 'black',
		fill : 'black'
	};
	this.scoreboard_background = this.paper.add([scoreboard_params])[0];

	var text_params = {
		type: 'text', 
		x : this.width/2,
		y : this.height/2,
		text : 'End of round',
		'font-size' : this.width/8,
		fill : 'white'
	}
	this.scoreboard_text = this.paper.add([text_params])[0];
}

GridWorldPainter.prototype.draw_finalscreen = function () {
	"use strict";
	var scoreboard_params = {
		type : 'rect',
		x : 0,
		y : 0,
		width : this.width,
		height : this.height,
		stroke : 'black',
		fill : 'black'
	};
	this.scoreboard_background = this.paper.add([scoreboard_params])[0];

	var text_params = {
		type: 'text', 
		x : this.width/2,
		y : this.height/2,
		text : 'End of Task',
		'font-size' : this.width/8,
		fill : 'white'
	}
	this.scoreboard_text = this.paper.add([text_params])[0];
}

GridWorldPainter.prototype.draw_waiting = function () {
	"use strict";
	var scoreboard_params = {
		type : 'rect',
		x : 0,
		y : 0,
		width : this.width,
		height : this.height,
		stroke : 'black',
		fill : 'black'
	};
	this.scoreboard_background = this.paper.add([scoreboard_params])[0];

	var text_params = {
		type: 'text', 
		x : this.width/2,
		y : this.height/2,
		text : 'Waiting for partner to join...',
		'font-size' : this.width/30,
		fill : 'white'
	}
	this.scoreboard_text = this.paper.add([text_params])[0];
}

GridWorldPainter.prototype.hide_waiting = function () {
	"use strict";
	this.scoreboard_background.animate({opacity : 0}, 250);
	this.scoreboard_text.animate({opacity :0}, 250)
}

GridWorldPainter.prototype.hide_score = function () {
	"use strict";
	if (typeof this.scoreboard_background !== 'undefined') {
	this.scoreboard_background.animate({opacity : 0}, 250);
	this.scoreboard_text.animate({opacity :0}, 250);
	}
}

GridWorldPainter.prototype.drawAction = function(action, x, y) {
	var newX = x,
		newY = y;
	switch(action) {
		case "north":
			newY = y + 1;
			break;
		case "west":
			newX = x - 1;
			break;
		case "south":
			newY = y - 1;
			break;
		case "east":
			newX = x + 1;
			break;
	}
	var agent_params = {cx : (newX + .5)*this.TILE_SIZE, 
				                cy : (this.gridworld.height - newY - .5)*this.TILE_SIZE};
			if (this.objectImages.hasOwnProperty("nextMove")) {
				this.objectImages['nextMove'].attr(agent_params);
			}
			else {
				agent_params.type = 'circle';
				agent_params.fill = this.PRIMARY_AGENT_COLOR;
				agent_params['fill-opacity'] = 0.5;
				agent_params.r = this.AGENT_WIDTH;
				agent_params.stroke = 'white';
				agent_params['stroke-width'] = 1;

				var objectImage = this.paper.add([agent_params])[0];
				this.objectImages['nextMove'] = objectImage;
			}
}

var ButtonPainter = function () {
	"use strict";
	this.PRIMARY_AGENT_COLOR = 'orange';

	this.BUTTON_WIDTH = 150;
	this.BUTTON_HEIGHT = 100;
	this.BUTTON_CORNER_RADIUS = 10;
	this.BUTTON_SPACING = 20;
};

ButtonPainter.prototype.init = function(container) {
	console.log("Initializing button painter");
	this.container = container;
	this.width = this.BUTTON_WIDTH * 3 + this.BUTTON_SPACING * 2;
	this.height = this.BUTTON_HEIGHT * 3 + this.BUTTON_SPACING * 2;
	if (typeof this.paper === 'undefined') {
		this.paper = Raphael(container, this.width, this.height);
	} else {
		this.paper.setSize(this.width, this.height);
	}

};

ButtonPainter.prototype.addButton = function(x, y, width, height, txt, cb) {
		var group = this.paper.set();
		var btn = this.paper.rect(x, y, width, height, this.BUTTON_CORNER_RADIUS);
		var text = this.paper.text(x + width/2.0,y + height/2.0,txt);
		btn.attr("fill", this.PRIMARY_AGENT_COLOR );
		group.push(btn);
		group.push(text);
		$(text.node).css({'font-size':'x-large'})
		//group.click(cb);

		group.attr({
		    cursor: 'pointer',
		}).mouseup(function(e) {
		    cb();
		}); 
		return group;
};


ButtonPainter.prototype.drawDirectionalButtons = function(up_cb, left_cb, down_cb, right_cb, wait_cb) {
	"use strict";
	console.log("Drawing buttons");
	var left_left = 0
	var up_wait_down_left = left_left + this.BUTTON_WIDTH + this.BUTTON_SPACING
	var right_left = up_wait_down_left + this.BUTTON_WIDTH + this.BUTTON_SPACING

	var up_top = 0
	var left_wait_right_top = up_top + this.BUTTON_HEIGHT + this.BUTTON_SPACING
	var down_top = left_wait_right_top + this.BUTTON_HEIGHT + this.BUTTON_SPACING

	this.addButton(up_wait_down_left, up_top, this.BUTTON_WIDTH, this.BUTTON_HEIGHT, "Up", up_cb);
	this.addButton(left_left, left_wait_right_top, this.BUTTON_WIDTH, this.BUTTON_HEIGHT, "Left", left_cb);
	this.addButton(up_wait_down_left, down_top, this.BUTTON_WIDTH, this.BUTTON_HEIGHT, "Down", down_cb);
	this.addButton(right_left, left_wait_right_top, this.BUTTON_WIDTH, this.BUTTON_HEIGHT, "Right", right_cb);
	this.addButton(up_wait_down_left, left_wait_right_top, this.BUTTON_WIDTH, this.BUTTON_HEIGHT, "Wait", wait_cb);
	
};

ButtonPainter.prototype.clear = function() {
	this.paper.clear();
}


var GridButtonPainter = function(container, gwPainter) {
	"use strict";
	console.log("Initializing button painter");
	this.width = gwPainter.width;
	this.height = gwPainter.height;
	this.TILE_SIZE = gwPainter.this.TILE_SIZE;
	this.paper = gwPainter.paper;
};


GridButtonPainter.prototype.addButton = function(name, cb) {
	"use strict";
	
};

GridButtonPainter.prototype.drawClearButtons = function(cb) {
	"use strict";
	console.log("Drawing buttons");
	var left_left = 0
	var up_wait_down_left = left_left + this.BUTTON_WIDTH + this.BUTTON_SPACING
	var right_left = up_wait_down_left + this.BUTTON_WIDTH + this.BUTTON_SPACING

	var up_top = 0
	var left_wait_right_top = up_top + this.BUTTON_HEIGHT + this.BUTTON_SPACING
	var down_top = left_wait_right_top + this.BUTTON_HEIGHT + this.BUTTON_SPACING

	this.addButton(up_wait_down_left, up_top, this.BUTTON_WIDTH, this.BUTTON_HEIGHT, "Up", up_cb);
	this.addButton(left_left, left_wait_right_top, this.BUTTON_WIDTH, this.BUTTON_HEIGHT, "Left", left_cb);
	this.addButton(up_wait_down_left, down_top, this.BUTTON_WIDTH, this.BUTTON_HEIGHT, "Down", down_cb);
	this.addButton(right_left, left_wait_right_top, this.BUTTON_WIDTH, this.BUTTON_HEIGHT, "Right", right_cb);
	this.addButton(up_wait_down_left, left_wait_right_top, this.BUTTON_WIDTH, this.BUTTON_HEIGHT, "Wait", wait_cb);
	
};







