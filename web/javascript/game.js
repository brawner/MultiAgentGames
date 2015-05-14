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
        currentState,
        currentAction,
        currentScore = 0;
    var self = this;
    var width = 768,
        height = 512;
        
	 //Sets up the playground and stage div    
    this.go = function() {
        
        var context = document.getElementById("GGCanvas").getContext("2d");
        context.fillStyle = "#FFFFFF";
        context.fillRect(0,0,768,512);
        handler = new GeneralHandler();


        connectToServer();
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

        if (typeof painter !== 'undefined') {
            painter.setWorlds(worlds);
            painter.setActive(active);
            console.log("Painting");
            painter.draw(currentState, currentScore, currentAction, width, height);
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
            var msg = message_writer.startGameMsg(label, client_id);  
            connection.Send(msg);  
            return;
        }

        console.log("No games initialized. Initialize game first");
        
    };

    var initialize_game = function(msg) {
        var initMsg = message_reader.getInitializationMsg(msg);
        console.log(initMsg);
        if (typeof initMsg !== 'undefined') {
            console.log("Initializing game " + initMsg.world_type);
            painter = new GamePainter(initMsg.agent_name);
            currentState = initMsg.state;
            painter.draw(currentState, currentScore, currentAction, width, height);
        }
    };

    var update_game = function(msg) {
        var updateMsg = message_reader.getUpdateMsg(msg);
        if (typeof initMsg !== 'undefined') {
            currentState = updateMsg.state;
            currentScore = updateMsg.score;
        }
    };

    var game_complete = function(msg) {
        var closeMsg = message_reader.getCloseMsg(msg);
        if (typeof closeMsg !== 'undefined') {
            painter.drawEnd(closeMsg.state, closeMsg.score, width, height);
        }
    };

    var onKeyPress = function(event) {

    };

	
};

