/** GameController.js
*   Robocook main logic controller
*   by: Lee Painton
*   by: Stephen Brawner
*   Development version
*/

//?Possible Recipes?
//Cookies
//Meatloaf
//Brownies
//Gnocchi

//Enum game state below tells the game what its current state is
//To change state set CurrentGameState to the appropriate state
//Note that game scenes may need to be reset() as well
var EnumGameState = {
    GameInit: 0,        	//Game initializing, precache
    SplashInit: 10,     	//Splash intro
    SplashIdle: 11,
    SplashTrans: 12,    	//Splash transition out
    MainMenuInit: 20,   	//Main menu active
    MainMenuIdle: 21,
    MainMenuTrans: 22,
    MatchmakingInit: 30,   //Matchmaking step, connection to server
    MatchmakingIdle: 31,
    MatchmakingTrans: 32,
    MatchInit: 40,      	//Main play started > init state
    MatchIntro: 50,     	//Main intro display
    MatchActive: 60,    	//Main active play state
    MatchEnd: 70,       	//Main play finished success or failure
    MatchTrans: 71,     	//Match transition out
    PostMatch: 80   			//Post main play conditions, restart?
};

//Set dummy player name
var PlayerName = "";

//Set initial game state
var CurrentGameState = EnumGameState.GameInit;

function fnMain(jQuery) {
    "use strict";
    var game = new Game();
    game.go();    
}

var Game = function() {
    "use strict";
    
    //This sets up several necessary objects
    var connection = new GameConnect(),
        message_reader = new MessageReader(),
        message_writer = new MessageWriter(),
        handler,
        playground,
        painter,
        client_id,
        agent_name,
        currentState,
        currentAction,
        currentScore = 0;
    var actions = {"North":"north", "South":"south", "East":"east", "West":"west", "Wait":"noop"};
    
    var self = this;
    var width = 768,
        height = 512;
        
    var isGameIdValid = function(text) {
        return !isNaN(text);
    }

    var onSubmitClick = function() {
        var textBox = document.getElementById('text_box');
        if (typeof textBox !== 'undefined' && textBox !== null){ 
            var text = textBox.value;
            if (isGameIdValid(text)) {
                var msg = message_writer.startGameMsg(text, client_id);  
                console.log("Sending " + msg);
                connection.Send(msg);  
            }
        }
    };
	 //Sets up the playground and stage div    
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
        //element.setAttribute("id", "text_box")
        //element.type = "text";
        
        var button = document.getElementById("submit_button");
        //var buttonText = document.createTextNode("Submit");
        //button.appendChild(buttonText);
        button.onclick = onSubmitClick;
        
        //inputDiv.appendChild(element);
        //inputDiv.appendChild(button);
        //inputDiv.style = "z-index:2; position:absolute";

        painter.draw(element, button)
        
    };

    this.loop = function() {

        //Debugging code
        /*if (CurrentGameState === EnumGameState.GameInit) {
            console.log("DEBUG: Main loop initialized.");
        }*/
        
        //This is the entry point for  decision making in the game loop.
        //All possible game states should be reflected here.
        switch(CurrentGameState)
        {
			case EnumGameState.GameInit:
            self.connectToServer();
                break;
            default:
                break;
                
        }
        painter.draw();
    };

    

    

    var connectToServer = function() {
        connection.AddCallback(self);
        connection.Open();
    };

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

    this.onClose = function(msg) {

    };

    this.onError = function(msg) {

    };

    this.onOpen = function(msg) {
        console.log("Connected to server");
    };

    var hello = function(msg) {

        var active = message_reader.getActiveWorlds(msg);
        for (var i = 0; i < active.length; i++) {
            var label = active[i].Label;
            
            return;
        }

        console.log("No games initialized. Initialize game first");
        
    };

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

    var game_complete = function(msg) {
        var closeMsg = message_reader.getCloseMsg(msg);
        if (typeof closeMsg !== 'undefined') {
            painter.drawEnd(currentState, closeMsg.score);
        }
    };

    var onAction = function(event) {
        if (event in actions) {
            currentAction = event;
            sendActionUpdate();
        }
        console.log("Current action " + currentAction);
        painter.draw(currentState, currentScore, currentAction);
    };

    var onInteraction = function(event) {

    };

    var sendActionUpdate = function() {
        var actualAction = actions[currentAction];
        var msg = message_writer.updateActionMsg(client_id, actualAction, agent_name);
        connection.Send(msg);
    };

	
};

