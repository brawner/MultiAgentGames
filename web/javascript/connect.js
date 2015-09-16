

//////////////////////////////////////////////////////
// gameconnect.js
// ----------------------------
// This module handles all websocket related stuff,
//  including message passing and top-of-the-chain
//  message handling.
// ---------------------
// Note: Set gameConnect.wsurl and then call Init
//  to connect.
//////////////////////////////////////////////////////

/**
 * @constructor
 */
var GameConnect = function(){
    "use strict";

    if (GameConnect.prototype._gameConnect) {
        return GameSceneMatch.prototype._gameConnect;
    }
    GameConnect.prototype._gameSceneMatch = this;
    
    var wsurl = "ws://elzar.cs.brown.edu:8787/events/",
        callbacks = [],
        callbackIds = [],
        ws = 0,
        clientId = 0,
        websocket = null,
        isOpen = false;
    
    this.IsOpen = function() {
        return isOpen;
    };

    this.URL = function() {
        return wsurl;
    };

    this.SetUrl = function(url) {
        wsurl = url;
    };

    this.IsValidUrl = function(url) {
        if (typeof url !== 'string') {
            console.log("URL value is not a string");
            return false;
        }
        var split_values = url.split(":");
        if (split_values.length !== 3) {
            console.log("URL needs to be of the form ws://server_url:port/web_socket_handler_name/");
            return false;
        }
        if (split_values[0] !== "ws") {
            console.log("URL needs to be a websocket address (use ws://)");
            return false;
        }

        var port_events = split_values[2];
        var port_split = port_events.split("/");
        if (port_split.length < 2) {
            console.log("URL needs a port and a events handler name");
            return false;
        }

        if (isNaN(port_split[0])) {
            console.log("URL does not contain a valid port");
            return false;
        }

        if (port_split[1].length == 0) {
            console.log("URL does not contain an appropriate events handler name");
            return false;
        }
        return true;
    };

    this.Open = function() {
        websocket = new WebSocket(wsurl);

        
        
        websocket.onopen = function() {
            OnOpen();
        };
        websocket.onmessage = function(evt) {
            OnMessage(evt);
        };
        websocket.onclose = function(evt) {
            OnClose(evt);
        };
        websocket.onerror = function(err) {
            OnError(err);
        };
    };

    this.Close = function() {
        if (typeof websocket !== 'undefined') {
            websocket.close();
        }
    };
    this.AddCallback = function(callback) {
        var id = 0;
        if (callbackIds.length > 0) {
            id = callbackIds[callbackIds.length - 1] + 1;
        }
        callbacks.push(callback);
        callbackIds.push(id);
        return id;
    };

    this.RemoveCallback = function(id) {
        for (var i = 0; i < callbackIds.length; i++) {
            if (callbackIds[i] == id) {
                callbackIds.splice(i, 1);
                callbacks.splice(i, 1);
            }
        }
    };

    var OnOpen = function() {
        console.log("Connection to websocket opened!");

        
        for (var i = 0; i < callbacks.length; i++) {
            callbacks[i].onOpen();
        }
        isOpen = true;


    };

    var OnMessage = function(evt) {
        if (JSON.stringify(evt.data) === '"{}"') {
            return;
        }
        var msg = JSON.parse(evt.data);
        console.log("Websocket Message from server %O", msg);

        if (MessageFields.CLIENT_ID in msg) {
            clientId = msg[MessageFields.CLIENT_ID];
        }
        for (var i = 0; i < callbacks.length; i++) {
            callbacks[i].onMessage(msg);
        }

    };
    
    var OnClose = function(evt) {
        console.log("Connection to websocket closed!");
        for (var i = 0; i < callbacks.length; i++) {
            callbacks[i].onClose(evt);
        }
        isOpen = false;
            
    };

    var OnError = function(err) {
        console.log("Websocket Error: " + err);
        for (var i = 0; i < callbacks.length; i++) {
            callbacks[i].onError(err);
        }
    };

    this.Send = function(msg){
        msg[MessageFields.CLIENT_ID] = clientId;
        var msgString = JSON.stringify(msg);
        if (msg[MessageFields.MSG_TYPE] !== MessageFields.HEARTBEAT) {
            console.log("Sending: " + msgString);
        }
        websocket.send(msgString);
    };

    //Reports actions taken by players to the server
    //Please note the functions which report success are contained in either gameobjects.js or gamerecipes.js.
    this.ReportCmdSucc = function(obj, target, action, log) {

        var token = {
                msgtype: "action",
                msg: {
                action: action,
                logmsg: log
                }
            };
        token.msg.params = (target) ? ["human", obj, target] : ["human", obj];
        if (action !== "") {         
            this.Send(token);
        }
        else {
            console.log("Failed to send message: " + JSON.stringify(token));
        }
    };

    this.requestReset = function() {
        var token = {
            msgtype: "reset"
        };
        this.Send(token);
    };



    

}
