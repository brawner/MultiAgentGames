/** GameController.js
*   Robocook main logic controller
*   by: Stephen Brawner
*   Development version
*/

/**
* Method called from index.html. Start point.
*/
function fnMain(jQuery) {
    "use strict";
    console.log($('#task_display'));
    var game = new Game();
    game.go();    
}

window['fnMain'] = fnMain;

/**
* This handles the coordination for an interactive game.
*/


/**
 * @constructor
 */
var Game = function() {
    "use strict";
    
    //The websocket connection object
    var connection = new GameConnect(),
        // Interprets messages received from the server
        message_reader = new MessageReader(),
        // Writes appropriate messages for sending to the server
        message_writer = new MessageWriter(),
        // Handles key and button interactions
        handler,
        // the current main painter object
        painter,
        text_messages,
        score_board,
        // tracks the connection status and displays it
        status_painter,
        // The client id assigned by the server to this client
        client_id,
        // The name given to the agent associated with this client
        agent_name,
        // Last state sent by the server
        currentState,
        // The last action requested by this client
        currentAction,
        // The current score for this agent.
        currentScore = 0,
        //for mark's code
        clientmdp,
        previousState;

    var round_ended = false;
    var step_start_time = 0;
    var more_games = true;
    var END_OF_ROUND_PAUSE = 1000 * 5;
    var WAIT_FOR_PARTNER = 1000 * 60 * 5;
    var redirect_page;


    var heartbeatSender;


    var agent_waited = false;

    var vars = {};
    var q = document.URL.split('?')[1];
    if(q != undefined){
        q = q.split('&');
        for(var i = 0; i < q.length; i++){
            var hash = q[i].split('=');
            //vars.push(hash[1]);
            vars[hash[0]] = hash[1];
            }
    }
    vars.length=q.length;
    console.log("Vars: %O", vars);

    var preferred_url;
    if (MessageFields.SERVER in vars){
        preferred_url = vars[MessageFields.SERVER];
    }
    var client_id;
    var url_client_id;
    var marks_code = true;
    var stephens_code = false;
    var is_initialized = false;
    var new_game = false;

    // Available actions to take
    var actions = {"North":"north", "South":"south", "East":"east", "West":"west", "Wait":"noop"};
    var agents = ["human","random"];
    
    var self = this;
    var width = 768,
        height = 512;

    var action_number =0;
    var game_number = 0;
    var last_msg;
        
    var isGameIdValid = function(text) {
        return !isNaN(text);
    }

    // When the submit button is clicked, this attempts to join the game with the server
    var onSubmitClick = function() {
        var textBox = document.getElementById('text_box');
        if (typeof textBox !== 'undefined' && textBox !== null){ 
            var text = textBox.value;
            if (isGameIdValid(text)) {
                var msg = message_writer.startGameMsg(text, client_id);  
                console.log("Sending from submit: " + msg);
                connection.Send(msg);  
            }
        }
    };

    // If the URL has the correct query terms, this attempts to join the game with the server
    var onURLWithQueryTerms = function() {
            console.log(" running on URL");
            //if the game exists, have agent join game
            
            url_client_id = vars['t_id'];
            redirect_page = vars['redirect']+"/?"+"SID="+vars['SID']+"&t_id="+url_client_id;

            if(vars['human']!= null && vars['game']!=null){
                var label = vars['game'];
                var run_two_humans = vars['human'];
                if(run_two_humans=='yes'){
                    agents = ["human","human"];
                }
            }else{
                //load from config file
                var label = null;
                var exp_name = vars['exp_name'];
                agents = null;
            }

            console.log("Running based on URL");
            

            var msg = message_writer.urlJoinMsg(label,agents,url_client_id, exp_name, vars);
            console.log(msg)
            connection.Send(msg);
    };

    this.onActionPress = function (event) {
        if (agent_waited) {
            return;
        }
        var action;
        console.log("On action press");
        $(document).unbind('keydown.gridworld');
        action_number++;
        switch (event.which) {
            case 37: //87: //
                action = 'west';
                break;
            case 38: //78: //
                action = 'north';
                break;
            case 39: //69: //
                action = 'east';
                break;
            case 40: //83: //
                action = 'south';
                break
            case 32: //80: //'p for pause'
                action ='noop';
                break;
            default:
                return;
        }

        //get reaction time
        var rt = (+new Date) - step_start_time;
        console.log("reaction time");
        console.log(rt);
        //client_id, agent_name, t_id, rt, game_number, action_number
        var rt_msg = message_writer.log_reaction_time(client_id, agent_name, url_client_id, rt, game_number,action_number);
        connection.Send(rt_msg);

        //$(document).unbind('keydown.gridworld');
        var msg = message_writer.updateActionMsg(client_id, action, agent_name);

        if(action=='noop'){
            text_messages.html('You are the '+painter.PRIMARY_AGENT_COLOR+' player! <br> Your Score: '+ currentScore+"<br> You waited."+"<br> Waiting on other player to move...");
        
        }else{
            text_messages.html('You are the '+painter.PRIMARY_AGENT_COLOR+' player! <br> Your Score: '+ currentScore+"<br> You went "+action+"."+"<br> Waiting on other player to move...");
        }

         var send_action = $.proxy(function () {
            console.log(msg);
            connection.Send(msg); 
                    
        }, this);

        send_action();
        //setTimeout(send_action, END_OF_ROUND_PAUSE/4);
    }

	// sets up everything
    this.go = function() {
        
        if (stephens_code) {
            var context = document.getElementById("GGCanvas").getContext("2d");
            context.fillStyle = "#FFFFFF";
            context.fillRect(0,0,768,512);
            handler = new GeneralHandler(actions);
            handler.addActionCallback(onAction);
            handler.addInteractionCallback(onInteraction);
        }
        if (marks_code) {
            console.log(" running go");
            $(document).bind('keydown.gridworld', this.onActionPress);
        }
        connectToServer();
        if(vars.length==0){

            painter = new OpeningScreenPainter(768, 512);
            
            var inputDiv = document.createElement("div");
            inputDiv.setAttribute("id", "divvyDiv");

            var element = document.getElementById('text_box');
            
            var button = document.getElementById("submit_button");
            
            button.onclick = onSubmitClick;
                
            painter.draw(element, button);
        }

        var refresh_game = $.proxy(function () {
            var initState = getAgentLocals(currentState);
            console.log("Printing again %O", initState);
            painter.drawState(initState);
        });

        $(window).focus(function() {
            
            setTimeout(refresh_game, 10);
        });
        
    };

    

    this.drawInitialState = function(){
        painter = new GamePainter(vars['t_id'], width, height);
        handler.registerWithPainter(painter);
        currentState = initialState;
        painter.draw(currentState, currentScore, currentAction);   
    }

    // When the connect button is clicked, this method attempts to connect with the server
    var onConnect = function(url) {
        if (!connection.IsValidUrl(url)) {
            return "URL is not a valid websocket url";
        }
        connection.SetUrl(url);
        connection.Open();
        if (connection.IsOpen()) {
            return 0;
        } else {
            return "Could not connect to server";
        }
    };

    // When the disconnect button is clicked, this ottempts to disconnect the server.
    var onDisconnect = function() {
        connection.Close();
    };

    var connection_painter = new ConnectionStatusPainter(onConnect, onDisconnect, 0, height);
    

    
    // Initial conenction to server method
    var connectToServer = function() {
        if (connection.IsValidUrl(preferred_url)) {
                connection.SetUrl(preferred_url);
        }     
        connection.AddCallback(self);
        connection.Open();
    };

    // On every message received from the server, this method handles it accordingly
    this.onMessage = function(msg) {
        if (MessageFields.Error in msg && msg[MessageFields.ERROR] === true) {
            console.log(msg[MessageFields.WHY_ERROR]);
        }
        var msgType = message_reader.getMessageType(msg);
        if (msgType == null || typeof msgType === 'undefined') {
            return;
        }
        var worlds = message_reader.getWorlds(msg);
        var active = message_reader.getActiveWorlds(msg);

        console.log(msgType);
        switch(msgType) {
            case MessageFields.HELLO_MESSAGE:
                hello(msg);
                break;
            case MessageFields.INITIALIZE:
                initialize_game(msg);
                break;
            case MessageFields.ACTION_REQUEST:
                break;
            case MessageFields.UPDATE:
                this.update_game(msg);
                break;
            case MessageFields.GAME_COMPLETE:
                game_complete(msg);
                break;
            case MessageFields.EXPERIMENT_COMPLETE:
                experiment_complete(false);
                break;
            default:
                console.log("Unknown message type " + msgType);
                break;
        }

        if (typeof painter !== 'undefined' && painter instanceof GamePainter) {
            painter.setWorlds(worlds);
            painter.setActive(active);
            
        }
    };

    // Called when the websocket closes
    this.onClose = function(msg) {
          //connection_painter.draw(false, connection.URL(), 10, height + 10);
          //CALL MARK'S CODE HERE OR REMOVE THIS CODE
    };

    // Called when the websocket has an error
    this.onError = function(msg) {

    };

    // Called when the websocket connects
    this.onOpen = function(msg) {
        console.log("Connected to server");
        //connection_painter.draw(true, connection.URL(), 10, height + 10);
        //CALL MARK'S CODE HERE OR REMOVE THIS CODE
    };

    // When receiving a hello message from the server, start things
    var hello = function(msg) {
        console.log("Running hello: vars length "+vars.length);

        client_id = message_reader.getClientId(msg);

        var active = message_reader.getActiveWorlds(msg);

        if(vars.length>0){

            onURLWithQueryTerms();
            console.log("Ran on URL");

        }else{

            //var active = message_reader.getActiveWorlds(msg);
            for (var key in active) {
                var label = active[key].Label;
            
                return;
            }
    

            console.log("No games initialized. Initialize some game first");

        }

        var heartbeat = function(){

            var msg = {};
            msg[MessageFields.MSG_TYPE] = MessageFields.HEARTBEAT;
            connection.Send(msg);
        }

        heartbeatSender = setInterval(heartbeat, 10000);
    };

    var waitForOtherAgent = function() {
        if (agent_waited) {
            experiment_complete(true);
        }
    }

    // When receiving an initialization message from the server, initialize a new game
    var initialize_game = function(msg) {
        var initMsg = message_reader.getInitializationMsg(msg);
        console.log(initMsg);
        if (typeof initMsg !== 'undefined') {
            console.log("Initializing game " + initMsg.world_type);
            agent_name = initMsg.agent_name;

            //example gridworld and initState

            if (!is_initialized) {
                var gridworld = getGridworld(initMsg.state);
                var initState = getAgentLocals(initMsg.state);
                clientmdp = new ClientMDP(gridworld);
                painter = new GridWorldPainter(gridworld);
                painter.init($('#task_display')[0], initMsg.agent_name);
                $(painter.paper.canvas).css({display :'block', margin : 'auto'}); //center the task
                painter.drawState(initState);
                previousState = initState;

                step_start_time = +new Date;

                //score_board = $('#score_board')
                //score_board.html('End of round')
                //score_board.hide();

                text_messages = $('#messages');
                text_messages.html('You are the '+painter.PRIMARY_AGENT_COLOR+' player! <br> Your Score: '+ currentScore +"<br>      "+"<br>        "+"<br>        ");

                // show waiting board here if only one
                if(initMsg.ready == "false"){
                    agent_waited = true;
                    painter.draw_waiting();
                    setTimeout(waitForOtherAgent, WAIT_FOR_PARTNER);
                }
                is_initialized = true;
            } else {
                var load_next_round = function () { 
                    var gridworld = getGridworld(initMsg.state);
                    var initState = getAgentLocals(initMsg.state);
                    previousState = initState;
                    painter.reset(gridworld, $('#task_display')[0], initMsg.agent_name);
                    $(painter.paper.canvas).css({display :'block', margin : 'auto'}); //center the task
                    painter.drawState(initState);
                    step_start_time = +new Date;

                    //score_board = $('#score_board')
                    //score_board.html('End of round')
                    //score_board.hide();

                    text_messages = $('#messages');
                    text_messages.html('You are the '+painter.PRIMARY_AGENT_COLOR+' player! <br> Your Score: '+ currentScore +"<br>      "+"<br>        "+"<br>        ");
                }
                setTimeout(load_next_round, END_OF_ROUND_PAUSE-500);
                   
            }

            
        }
    };



    var getGridworld = function(state){
        console.log(JSON.stringify(state));
        var gridworld = {};
        gridworld.height = getHeightFromState(state.Walls);
        gridworld.width = getWidthFromState(state.Walls);
        gridworld.walls = convertWalls(state.Walls,gridworld.width,gridworld.height);
        console.log(gridworld.walls);
        gridworld.goals = convertGoals(state.Goals,gridworld.width,gridworld.height, state.Agents);
        gridworld.agents = [];
        for(var a = 0;a<state.Agents.length; a++){
            var temp = {};
            temp.name = state.Agents[a].Name;
            gridworld.agents.push(temp);
        }
        console.log(gridworld);
        return gridworld;
        
    }

    var getHeightFromState = function(walls){
        var maxY = 0;
        
        for(var w = 0; w<walls.length;w++){
            if(walls[w].EndY > maxY){
                maxY = walls[w].EndY;
            }
        }
        return maxY;
    }


    var getWidthFromState = function(walls){
        var maxX = 0;
        for(var w = 0; w<walls.length;w++){
            if(walls[w].EndX > maxX){
                maxX = walls[w].EndX;
            }
        }
        return maxX;
    }

    var convertWalls = function(walls,width,height){
        var newWalls = [];

        var wall;
        for(var w = 0; w<walls.length;w++){
            wall = walls[w];
            if((wall.StartX== 0 && wall.EndX==0)||(wall.StartX== width && wall.EndX==width)
                ||(wall.StartY== 0 && wall.EndY==0)||(wall.StartY== height && wall.EndY==height)){
                //this is 
               // console.log(height);
                //console.log(width);
                //console.log(wall);
            }else{
                    //add wall
                    if(wall.StartX==wall.EndX){
                        //make vertical walls
                        for(i = wall.StartY;i < wall.EndY;i++){
                            var vwr = [wall.StartX-1,i,'right'];
                            var vwl = [wall.StartX,i,'left'];
                            newWalls.push(vwr);
                            newWalls.push(vwl);
                        }
                    }else if(wall.StartY==wall.EndY){
                        //make horizontal wall
                        for(i = wall.StartX;i < wall.EndX;i++){
                            var hwu = [i,wall.StartY-1,'up'];
                            var hwd = [i,wall.StartY,'down'];
                            newWalls.push(hwu);
                            newWalls.push(hwd);
                        }
                    }
            }

        }
        return newWalls;

    }

    var convertGoals = function(goals,width,height,agents){
        var newGoals = [];
        var goal;
        for(var g = 0; g<goals.length;g++){
            goal = goals[g];
            var newGoal = {};
            for(var a = 0; a<agents.length;a++){
                if(agents[a].Number == goal.GoalType-1){
                    newGoal.agent = agents[a].Name;
                }
            }
            
            newGoal.location = [goal.X,goal.Y];
            newGoals.push(newGoal);

        }
        return newGoals;

    }

    // Handle a game update, and update the state and visualization
    this.update_game = function(msg) {
        if(agent_waited){
            painter.hide_waiting();
            agent_waited = false;
        }
        console.log("Running update_game with msg below");
        console.log(msg);
        var updateMsg = message_reader.getUpdateMsg(msg);
        if (typeof updateMsg !== 'undefined') {
            currentState = updateMsg.state;
            if (typeof updateMsg.score !== 'undefined') {
                currentScore = updateMsg.score;
            }
            currentAction = undefined;
        }

    
        if (stephens_code) {
            painter.draw(currentState, currentScore, currentAction);    
        }
        
        //CALL MARK'S CODE HERE
        if (marks_code) {

            console.log(" RUNNING MARKS UPDATE CODE");
           
            
            if(updateMsg.action !=null){
                
                console.log(updateMsg.is_terminal);
                
                var currentActions = convertActions(updateMsg.action);

                var nextState = getAgentLocals(updateMsg.state);
                console.log("current actions:");
                console.log(currentActions);
                var animation_time = painter.drawTransition(previousState, currentActions, nextState, clientmdp);
                text_messages.html('You are the '+painter.PRIMARY_AGENT_COLOR+' player! <br> Your Score: '+ currentScore+"<br>        "+"<br>        "+"<br>        "); // +"<br> Other Agent's Score: " + 0);

                previousState = nextState;

                var load_next_step = $.proxy(function () {
                    step_start_time = +new Date;
                    if (round_ended) {
                        var draw_score = $.proxy(function () {
                            painter.draw_score()
                        }, this)
                        var hide_score = $.proxy(function() {
                            painter.hide_score();
                            $(document).bind('keydown.gridworld', this.onActionPress);
                        }, this);
                        setTimeout(draw_score, END_OF_ROUND_PAUSE/2);
                        setTimeout(hide_score, END_OF_ROUND_PAUSE);
                    }
                    else {
                        $(document).bind('keydown.gridworld', this.onActionPress);
                    }
                }, this);

                setTimeout(load_next_step, animation_time);
            } 

            if(round_ended){
                //if its an initial state, wait a bit to load
                var initState = getAgentLocals(updateMsg.state);

                var load_next_round = (function (initState) {
                    return function () {
                        painter.drawState(initState);
                        step_start_time = +new Date;
                        previousState = initState;

                        text_messages.html('You are the '+painter.PRIMARY_AGENT_COLOR+' player! <br> Your Score: '+ currentScore+"<br>        "+"<br>        "+"<br>        ");
                        round_ended = false;
                    }
                }) (initState);

                load_next_round = $.proxy(load_next_round, this);

                setTimeout(load_next_round, END_OF_ROUND_PAUSE);
            }
        }
        
    };

   
    var convertActions = function(taken_actions){

        var jointaction = {};
        for(var j = 0; j < taken_actions.length;j++){
            var an = taken_actions[j]['agent'];
            var a = taken_actions[j]['action'];
            switch (a) {
            case 'west': 
                a = 'left';
                break;
            case 'north': 
                a= 'up';
                break;
            case 'east': 
                a = 'right';
                break;
            case 'south': 
                a = 'down';
                break
            case 'noop':
                a ='wait';
                break;
            default:
                return;
        }
            jointaction[an] = a;
        }
        return jointaction;
    }


    var getAgentLocals = function(state){

        var locals = {}
        var agent;
        for(var a = 0; a<state.Agents.length;a++){
            var temp = {};
            agent = state.Agents[a];
            temp.name = agent.Name;
            temp.location = [agent.X,agent.Y];
            temp.type = 'agent';
            locals[agent.Name] = temp;
        }
        return locals;
        
    }
    // Handle the game complete state
    var game_complete = function(msg) {
        var closeMsg = message_reader.getCloseMsg(msg);
        if (typeof closeMsg !== 'undefined') {
            
            if (stephens_code) {
                painter.drawEnd(currentState, closeMsg.score);    
            }
            
            round_ended = true;
               
            console.log("Round " + game_number + " ended");
            game_number++;
            action_number = 0;
            //CALL MARK'S CODE HERE
        }
    };

    var experiment_complete = function(partnerTimeout) {
        
        if (stephens_code) {
            painter.drawEnd(currentState, closeMsg.score);    
        }
        if(marks_code){
            
            console.log("In experiment_complete");

            var closeAlert = function(){
                return 'Please do not close or reload this window before completing the task. Doing so will invalidate your responses!'
                }
            $(window).bind('beforeunload',closeAlert);

             var load_next_step = $.proxy(function () {
                
                var draw_finalscreen = $.proxy(function () {
                    console.log("Drawing final screen");
                    painter.draw_finalscreen()
                }, this)
                var go_to_next_url = $.proxy(function () {
                    var found_partner = !partnerTimeout;
                    var url = redirect_page + "&found_partner=" + found_partner.toString();

                    console.log("Redirecting to: " + url);
                    $(location).attr('href',url);
                }, this)

                setTimeout(draw_finalscreen, END_OF_ROUND_PAUSE);
                console.log("Drawing done");
                setTimeout(go_to_next_url, END_OF_ROUND_PAUSE*2);
                console.log("Redirecting done");
                    //add timeout for redirect
            }, this);

            $(window).unbind('beforeunload');
            setTimeout(load_next_step, END_OF_ROUND_PAUSE/3);
        }
    }

    // Callback function for the Action Handler when someone presses a button or enters a key action
    var onAction = function(event) {
        if (event in actions) {
            currentAction = event;
            sendActionUpdate();
        }
        console.log("Current action " + currentAction);
        painter.draw(currentState, currentScore, currentAction);
        //CALL MARK'S CODE HERE
    };

    // Not used
    var onInteraction = function(event) {

    };

    // Send the action update to the server.
    var sendActionUpdate = function() {
        var actualAction = actions[currentAction];
        var msg = message_writer.updateActionMsg(client_id, actualAction, agent_name);
        connection.Send(msg);
    };

	
};

