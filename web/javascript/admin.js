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
    var admin = new Admin();
    admin.go();    
}

var Admin = function() {
    "use strict";
    
    //This sets up several necessary objects
    var connection = new GameConnect(),
        message_reader = new MessageReader(),
        message_writer = new MessageWriter(),
        handler,
        painter,
        config_painter,
        worlds,
        active,
        agentTypes,
        client_id;
    
    var self = this;
        
    var isGameIdValid = function(text) {
        return !isNaN(text);
    }

    var onWorldClick = function(label) {
        var msg = message_writer.initializeGameMsg(label);
        connection.Send(msg);
    };

    var onSubmitConfig = function(label, agent_configurations) {
        var msg = message_writer.configurationMsg(label, agent_configurations);
        connection.Send(msg);
    };

    var onRunGame = function(label) {
        var msg = message_writer.runGameMsg(label);
        connection.Send(msg);
    };

    var onRemoveGame = function(label) {
        var msg = message_writer.removeGameMsg(label);
        connection.Send(msg);
    };

    var onActiveClick = function(label) {
       
        var description = "";
        var agents = [];
        var maxAgents = -1;
        for (var i = 0; i < active.length; i++) {
            if (label === active[i].Label) {
                description = active[i].Description;
                agents = active[i].Agents;
                maxAgents = active[i].NumAgents;
                break;
            }
        }

        if (typeof config_painter !== 'undefined') {
            config_painter.clear();
        }
        config_painter = new GameConfigPainter(label, description, agentTypes, onSubmitConfig, onRunGame, onRemoveGame);

        config_painter.draw(agents, maxAgents, 600, 100);
    };

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

    var onDisconnect = function() {
        connection.Close();
    };

    var connection_painter = new ConnectionStatusPainter(onConnect, onDisconnect, 0,0);
        
    this.go = function() {
        
        //handler = new GeneralHandler(actions);
        connectToServer();
        painter = new AdminPagePainter(onWorldClick, onActiveClick);
        
        var inputDiv = document.createElement("div");
        inputDiv.setAttribute("id", "divvyDiv");
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
        if (MessageFields.WORLDS in msg) {
            worlds = message_reader.getWorlds(msg);
        }
        if (MessageFields.ACTIVE in msg) {
            active = message_reader.getActiveWorlds(msg);
        }
        if (MessageFields.AGENTS in msg) {
            agentTypes = message_reader.getAgentTypes(msg);
        }

        console.log(msgType);
        if (typeof painter !== 'undefined') {
            painter.draw(worlds, active);
        }
    };

    this.onClose = function(msg) {
          connection_painter.draw(false, connection.URL(), 10, 10);
    };

    this.onError = function(msg) {

    };

    this.onOpen = function(msg) {
        console.log("Connected to server");
        connection_painter.draw(true, connection.URL(), 10, 10);
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

