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
    var game = new Game();
    game.go();    
}

/**
* This handles the coordination for an interactive game.
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
        currentScore = 0;

    var vars = [], hash;
    var q = document.URL.split('?')[1];
    if(q != undefined){
        q = q.split('&');
        for(var i = 0; i < q.length; i++){
            hash = q[i].split('=');
            vars.push(hash[1]);
            vars[hash[0]] = hash[1];
            }
    }

    // Available actions to take
    var actions = {"North":"north", "South":"south", "East":"east", "West":"west", "Wait":"noop"};
    var agents = ["human","qlearner"];
    
    var self = this;
    var width = 768,
        height = 512;
        
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
        
            //if the game exists, have agent join game
            var label = vars['exp_name'];
            var url_client_id = vars['t_id'];
            console.log("Running based on URL");
            
            if (isGameIdValid(label)) {

                var msg = message_writer.configurationMsg(label, agents);
                connection.Send(msg); 

                var msg = message_writer.runGameMsg(label);
                connection.Send(msg);

                var msg = message_writer.startGameMsg(label, client_id); 
                console.log("Sending after valid " + msg);
                connection.Send(msg);
            //else create the game and have agent join game
            }else{
                //create game
                var initMsg = message_writer.initializeGameMsg(label);
                connection.Send(initMsg);

                var configmsg = message_writer.configurationMsg(label, agents);
                connection.Send(configmsg); 

                var runmsg = message_writer.runGameMsg(label);
                connection.Send(runmsg);

                var startmsg = message_writer.startGameMsg(label, client_id);  
                console.log("Sending not valid: " + startmsg);
                connection.Send(startmsg);


            }
            

        
    };

   

	// sets up everything
    this.go = function() {
        
        var context = document.getElementById("GGCanvas").getContext("2d");
        context.fillStyle = "#FFFFFF";
        context.fillRect(0,0,768,512);
        handler = new GeneralHandler(actions);
        handler.addActionCallback(onAction);
        handler.addInteractionCallback(onInteraction);
        connectToServer();
        painter = new OpeningScreenPainter(768, 512);
        
        var inputDiv = document.createElement("div");
        inputDiv.setAttribute("id", "divvyDiv");

        var element = document.getElementById('text_box');
        
        var button = document.getElementById("submit_button");
        
        button.onclick = onSubmitClick;
        
        painter.draw(element, button);
        
    };

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
        connection.AddCallback(self);
        connection.Open();
    };

    // On every message received from the server, this method handles it accordingly
    this.onMessage = function(msg) {
        if (MessageFields.Error in msg && msg[MessageFields.ERROR] === true) {
            console.log(msg[MessageFields.WHY_ERROR]);
        }
        var msgType = message_reader.getMessageType(msg);
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
            case MessageFields.UPDATE:
                update_game(msg);
                break;
            case MessageFields.GAME_COMPLETE:
                game_complete(msg);
                break;
            default:
            break;
        }

        if (typeof painter !== 'undefined' && painter instanceof GamePainter) {
            painter.setWorlds(worlds);
            painter.setActive(active);
            
        }
    };

    // Called when the websocket closes
    this.onClose = function(msg) {
          connection_painter.draw(false, connection.URL(), 10, height + 10);
    };

    // Called when the websocket has an error
    this.onError = function(msg) {

    };

    // Called when the websocket connects
    this.onOpen = function(msg) {
        console.log("Connected to server");
        connection_painter.draw(true, connection.URL(), 10, height + 10);
    };

    // When receiving a helo message from the server, start things
    var hello = function(msg) {
        console.log("Running hello vars length "+vars.length);
        if(vars.length>0){


            onURLWithQueryTerms();
            console.log("Ran on URL");

        }else{

        var active = message_reader.getActiveWorlds(msg);
        for (var i = 0; i < active.length; i++) {
            var label = active[i].Label;
            
            return;
        }
    }

        console.log("No games initialized. Initialize some game first");

        
    };

    // When receiving an initialization message from the server, initialize a new game
    var initialize_game = function(msg) {
        var initMsg = message_reader.getInitializationMsg(msg);
        console.log(initMsg);
        if (typeof initMsg !== 'undefined') {
            console.log("Initializing game " + initMsg.world_type);
            agent_name = initMsg.agent_name;

            var element = document.getElementById('text_box');
            element.style.visibility = "hidden";
            var button = document.getElementById('submit_button');
            button.style.visibility = "hidden";

            painter = new GamePainter(initMsg.agent_name, width, height);
            handler.registerWithPainter(painter);
            currentState = initMsg.state;
            painter.draw(currentState, currentScore, currentAction);
        }
    };


    // Handle a game update, and update the state and visualization
    var update_game = function(msg) {
        var updateMsg = message_reader.getUpdateMsg(msg);
        if (typeof updateMsg !== 'undefined') {
            currentState = updateMsg.state;
            if (typeof updateMsg.score !== 'undefined') {
                currentScore = updateMsg.score;
            }
            currentAction = undefined;
        }
        painter.draw(currentState, currentScore, currentAction);
    };

    // Handle the game complete state
    var game_complete = function(msg) {
        var closeMsg = message_reader.getCloseMsg(msg);
        if (typeof closeMsg !== 'undefined') {
            painter.drawEnd(currentState, closeMsg.score);
        }
    };

    // Callback function for the Action Handler when someone presses a button or enters a key action
    var onAction = function(event) {
        if (event in actions) {
            currentAction = event;
            sendActionUpdate();
        }
        console.log("Current action " + currentAction);
        painter.draw(currentState, currentScore, currentAction);
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

