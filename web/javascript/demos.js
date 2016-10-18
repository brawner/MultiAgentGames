var GridWorldDemo = function (gridworld, initState, key_handler, initText, task_display, text_display) {
	this.gridworld = gridworld;
	this.initState = initState;
	this.task_display = document.getElementById('task_display');
	this.text_display = $(text_display);
	this.state = initState;
	this.key_handler = (function (context, key_handler) {
							return function (event) {
								key_handler.call(context, event);
							}
						})(this, key_handler);
	this.initText = initText;
}

GridWorldDemo.prototype.start = function () {
	this.mdp = new ClientMDP(this.gridworld);
	this.painter = new GridWorldPainter(this.gridworld);

	$(document).bind('keydown.gridworld', this.key_handler);

	this.text_display.html(this.initText);
	this.painter.init(this.task_display);
	$(this.painter.paper.canvas).css({display : 'block', margin : 'auto'}); //center the task
	this.painter.drawState(this.state);
}

GridWorldDemo.prototype.end = function () {
	this.painter.remove();
	$(document).unbind('keydown.gridworld');
}

//Practicing moving around
$(document).ready(function () {
	var gridWorld_single_actions = function (event) {
								var action;
								switch (event.which) {
									case 37:
										action = 'left';
										break
									case 38:
										action = 'up';
										break
									case 39:
										action = 'right';
										break
									case 40:
										action = 'down';
										break
									case 32:
										action ='wait';
										break
									default:
										return
								}
								//choose random actions for other agents
								var agentActions = {};
								var availableActions = ['left','up','right','down','wait']
								for (var agent in this.state) {
									if (this.state[agent].type == 'agent' && agent !== 'agent1') {
										agentActions[agent] = availableActions[Math.floor(Math.random()*availableActions.length)]
									}
								}
								agentActions['agent1'] = action;

								var nextState = this.mdp.getTransition(this.state, agentActions);
								this.painter.drawTransition(this.state, agentActions, nextState, this.mdp);
								this.state = nextState;

								$(document).unbind('keydown.gridworld');

								//rewards
								for (var agent in this.state) {
									if (this.mdp.inGoal(nextState[agent]['location'], agent)) {
										var celebrateGoal = (function (painter, location, agent) {
											return function () {
												painter.showReward(location, agent, 'Goooaal')
											}
										})(this.painter, nextState[agent]['location'], agent)
										var th = setTimeout(celebrateGoal, this.painter.ACTION_ANIMATION_TIME);
										$.subscribe('killtimers', (function (th) {
												return function () {clearTimeout(th)}
											})(th)
										)
									}
								}
									
								//note: you need a closure in order to properly reset
								var reset_key_handler = (function (key_handler) {
									return function () {
										$(document).bind('keydown.gridworld', key_handler);
									}
								})(this.key_handler);
								var th = setTimeout(reset_key_handler, this.painter.ACTION_ANIMATION_TIME);
								$.subscribe('killtimers', (function (th) {
												return function () {clearTimeout(th)}
											})(th)
										)
							}

	var gridWorld_game_actions = function (event) {
								var action;
								switch (event.which) {
									case 37:
										action = 'left';
										break
									case 38:
										action = 'up';
										break
									case 39:
										action = 'right';
										break
									case 40:
										action = 'down';
										break
									case 32:
										action ='wait';
										break
									default:
										return
								}
								//choose random actions for other agents
								var agentActions = {};
								//var availableActions = ['left','up','right','down','wait']
								//for (var agent in this.state) {
								//	if (this.state[agent].type == 'agent' && agent !== 'agent1') {
								//		agentActions[agent] = availableActions[Math.floor(Math.random()*availableActions.length)]
								//	}
								//}
								agentActions['agent2'] = this.agent2Policy[this.state['agent2'].location];
								agentActions['agent1'] = action;

								var nextState = this.mdp.getTransition(this.state, agentActions);
								this.painter.drawTransition(this.state, agentActions, nextState, this.mdp);
								this.state = nextState;

								$(document).unbind('keydown.gridworld');

								//rewards
								var reset_key_time = this.painter.ACTION_ANIMATION_TIME;
								for (var agent in this.state) {
									if (this.mdp.inGoal(nextState[agent]['location'], agent)) {
										var goalCelebrate = (function (demo, location, agent) {
											return function () {
												demo.painter.showReward(location, agent, 'Goooaal');
												if (typeof demo.points === 'undefined') {
													demo.points = {agent1 : 0, agent2 :0}
												}
												demo.points[agent]++;
												demo.text_display.html('Agents win a point for getting to their goal<br>(Points determine bonuses)<br>You: '+demo.points['agent1']+
																		'    Red: '+demo.points['agent2'])
											}
										})(this, nextState[agent]['location'], agent)
										var th = setTimeout(goalCelebrate, this.painter.ACTION_ANIMATION_TIME)
										$.subscribe('killtimers', (function (th) {
												return function () {clearTimeout(th)}
											})(th)
										)

										var resetRound = (function (demo) {
											return function () {
												demo.state = demo.initState;
												demo.painter.drawState(demo.initState);
											}
										})(this)
										var th = setTimeout(resetRound, this.painter.ACTION_ANIMATION_TIME+1000);
										$.subscribe('killtimers', (function (th) {
												return function () {clearTimeout(th)}
											})(th)
										)
										reset_key_time = this.painter.ACTION_ANIMATION_TIME + 1000;
									}
								}
									
								//note: you need a closure in order to properly reset
								var reset_key_handler = (function (key_handler) {
									return function () {
										$(document).bind('keydown.gridworld', key_handler);
									}
								})(this.key_handler);
								var th = setTimeout(reset_key_handler, reset_key_time);
								$.subscribe('killtimers', (function (th) {
												return function () {clearTimeout(th)}
											})(th)
										)
							}

	demo0 = new GridWorldDemo(
		//gridworld
		{
			height : 3,
			width : 3,
			walls : [],
			goals : [],
			agents : [{name : 'agent1'}]
		},
		//initial state
		{
			agent1 : {name : 'agent1', location : [1,1], type : 'agent'}
		}, 
		function (event) {}
		,
		//initial text, display id, message id
		'This is your agent. It can move to different tiles on the board. <br> Press enter to continue ',
		'#task_display',
		'#messages'
	);

	demo1 = new GridWorldDemo(
		//gridworld
		{
			height : 3,
			width : 3,
			walls : [],
			goals : [],
			agents : [{name : 'agent1'}]
		},
		//initial state
		{
			agent1 : {name : 'agent1', location : [1,1], type : 'agent'}
		}, 
		gridWorld_single_actions
		,
		//initial text, display id, message id
		'Arrow keys move you around, and the spacebar makes you wait. Try it! <br> Press enter to continue ',
		'#task_display',
		'#messages'
	);

	demo2 = new GridWorldDemo(
		//gridworld
		{
			height : 3,
			width : 3,
			walls : [],
			goals : [{agent:'agent1', location: [0,0]}],
			agents : [{name : 'agent1'}]
		},
		//initial state
		{
			agent1 : {name : 'agent1', location : [2,2], type : 'agent'}
		}, 
		gridWorld_single_actions
		,
		//initial text, display id, message id
		'The green tile is your goal.<br>Try going to your goal! <br> Press enter to continue',
		'#task_display',
		'#messages'
	);

	//
	demo3 = new GridWorldDemo(
		//gridworld
		{
			height : 3,
			width : 3,
			walls : [[0,0,'right'],[1,0,'left'],[0,1,'right'],[1,1,'left'],[0,2,'right'],[1,2,'left'],
					 [2,2,'down'],[2,1,'up']],
			goals : [{agent:'agent1', location: [0,0]}],
			agents : [{name : 'agent1'}]
		},
		//initial state
		{
			agent1 : {name : 'agent1', location : [2,0], type : 'agent'}
		}, 
		gridWorld_single_actions
		,
		//initial text, display id, message id
		'The wide black lines are walls. <br>These sometimes get in your way :( <br> Press enter to continue',
		'#task_display',
		'#messages'
	);

	demo4 = new GridWorldDemo(
		//gridworld
		{
			height : 3,
			width : 3,
			walls : [],
			goals : [{agent:'agent1', location: [0,0]}, {agent:'agent2', location: [2,2]}],
			agents : [{name : 'agent1'}, {name : 'agent2'}]
		},
		//initial state
		{
			agent1 : {name : 'agent1', location : [2,0], type : 'agent'},
			agent2 : {name : 'agent2', location : [0,2], type : 'agent'}
		}, 
		gridWorld_single_actions
		,
		//initial text, display id, message id
		'Other agents also have their own goals! <br> Are they friends or foes? Who knows! <br> Press enter to continue',
		'#task_display',
		'#messages'
	);

	demo5 = new GridWorldDemo(
		//gridworld
		{
			height : 1,
			width : 4,
			walls : [],
			goals : [{agent : 'agent1', location : [0,0]}, {agent : 'agent2', location : [3,0]}],
			agents : [{name : 'agent1'}, {name : 'agent2'}]
		},
		//initial state
		{
			agent1 : {name : 'agent1', location : [3,0], type : 'agent'},
			agent2 : {name : 'agent2', location : [0,0], type : 'agent'}
		}, 
		gridWorld_game_actions
		,
		//initial text, display id, message id
		'Only one agent is allowed on each tile. <br> When 2 agents collide, nobody moves. <br> Press enter to continue',
		'#task_display',
		'#messages'
	);
	
	demo5.agent2Policy = {'0,0':'right', '1,0':'left'};

	demo6 = new GridWorldDemo(
		//gridworld
		{
			height : 4,
			width : 3,
			walls : [],
			goals : [{agent:'agent1', location: [0,0]}, {agent:'agent2', location: [1,3]}],
			agents : [{name : 'agent1'}, {name : 'agent2'}]
		},
		//initial state
		{
			agent1 : {name : 'agent1', location : [2,0], type : 'agent'},
			agent2 : {name : 'agent2', location : [0,0], type : 'agent'}
		}, 
		gridWorld_game_actions
		,
		//initial text, display id, message id
		'In this task, you will play with another agent for 20 rounds <br> Each round ends when somebody reaches their goal<br>Press Enter to continue',
		'#task_display',
		'#messages'
	);

	demo6.agent2Policy = {'0,0':'right', '1,0':'up', '1,1':'up', '1,2':'up'}

	endDemo = new GridWorldDemo(
		//gridworld
		{
			height : 3,
			width : 3,
			walls : [],
			goals : [{agent:'agent1', location: [0,0]},{agent:'agent1', location: [0,1]},
					 {agent:'agent1', location: [0,2]},{agent:'agent1', location: [1,0]},
					 {agent:'agent1', location: [1,2]},{agent:'agent1', location: [2,0]},
					 {agent:'agent1', location: [2,1]},{agent:'agent1', location: [2,2]}],
			agents : [{name : 'agent1'}]
		},
		//initial state
		{
			agent1 : {name : 'agent1', location : [1,1], type : 'agent'}
		}, 
		gridWorld_single_actions
		,
		//initial text, display id, message id
		"That's how it works!!!<br>.~*`*~.~*`*~.~*`*~.~*`*~.<br>-=-=-=-=-=-=-=-=-=-=-=-",
		'#task_display',
		'#messages'
	);
	endDemo.end = function () {}
})
