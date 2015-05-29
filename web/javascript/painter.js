var GamePainter = function(agent_name, _width, _height) {
    "use strict";
    var canvas = document.getElementById("GGCanvas");
    var context = canvas.getContext("2d");
    var primary_color = "green";
    var secondary_color = "blue";
    var statePainter = new StatePainter(context, agent_name, primary_color, secondary_color);
    var actionStatusPainter = new ActionStatusPainter(primary_color);
    var actionPainter = new ActionPainter(canvas, context, primary_color);
    var statusPainter = new StatusPainter(context, primary_color);
    var worldsPainter = new WorldsPainter();
    var activePainter = new ActiveWorldsPainter();
    var endPainter = new EndScreenPainter(_width, _height, context);
    var callbacks = [];
    var width = _width;
    var height = _height;
    //canvas.addEventListener('mousedown', function(e) {console.log("click " + e.x + " " + e.y);}); 
    
    statePainter.addPainter(new CellPainter(context, "circle"), "Agent");
    statePainter.addPainter(new CellPainter(context, "square"), "Goal");
    statePainter.addPainter(new WallPainter(context), "Wall");
    statePainter.addPainter(new CellPainter(context, "square"), "Toll");
    
    var loadButtons = function() {
        var state_width = width - actionPainter.width();
        var state_height = height - statusPainter.height();

        var buttons = [];
        buttons = buttons.concat(actionPainter.loadButtons(state_width, 0));
        return buttons;
    };
    var button_locations = loadButtons();
    

    this.draw = function(state, client_score, chosen_action) {
        context.fillStyle = "white";
        context.fillRect(0,0, width, height);

        var state_width = width - actionPainter.width();
        var state_height = height - statusPainter.height();

        statePainter.draw(state, state_width, height);
        //actionStatusPainter.draw(chosen_action);
        actionPainter.draw(state_width, 0, chosen_action);
        statusPainter.draw(client_score, state_width, height - 20);
    };

    this.drawEnd = function(state, score) {
        this.draw(state, score, undefined);
        endPainter.draw(score);
    };

    this.setActive = function(activeWorlds) {
        activePainter.setActive(activeWorlds);
    };

    this.setWorlds = function(worlds) {
        worldsPainter.setWorlds(worlds);
    };

    this.addButtonCallback = function(callback) {
        callbacks.push(callback);
    };

    var onMouseDown = function(event) {
        console.log("click " + event.x + ", " + event.y);
        var coordinates = getCoordinates(event);
        var button = getButtonAtCoordinates(coordinates);
        console.log("button: " + button);

        if (typeof button !== 'undefined') {
            for (var i = 0;i < callbacks.length; i++) {
                callbacks[i](button);
            }
        }
    };
    canvas.addEventListener('mousedown', onMouseDown); 
    

    var getCoordinates = function(mouse_event) {
        var offset = getOffset();
        var mouseX = mouse_event.pageX - offset.x;
        var mouseY = mouse_event.pageY - offset.y;
        return {x:mouseX, y:mouseY};
    };

    var getOffset = function() {
        var offsetX = 0, offsetY = 0;
        var element = canvas;
        if (element.offsetParent !== undefined) {
            do {
              offsetX += element.offsetLeft;
              offsetY += element.offsetTop;
            } while ((element = element.offsetParent));
        }
        return {x:offsetX, y:offsetY};
    };

    var getButtonAtCoordinates = function(coords) {
        for (var i = 0; i < button_locations.length; i++) {
            var button = button_locations[i];
            if (typeof button !== 'undefined' && button.IsInside(coords.x, coords.y)) {
                return button.Name();
            }
        }
    };
};

var Button = function(_name, _x, _y, _width, _height) {
    "use strict";
    var name = _name;
    var x = _x;
    var y = _y;
    var width = _width;
    var height = _height;

    this.Name = function() {
        return name;
    };

    this.X = function() {
        return x;
    };

    this.Y = function() {
        return y;
    };

    this.Width = function() {
        return width;
    };

    this.Height = function() {
        return height;
    };

    this.Left = function() {
        return x;
    };

    this.Right = function() {
        return x + width;
    };

    this.Bottom = function() {
        return y;
    };

    this.Top = function() {
        return y + height;
    };

    this.IsInside = function(x, y) {
        return (x >= this.Left() && x <= this.Right() &&
                y >= this.Bottom() && y <= this.Top());
    };


};

var StatePainter = function(context, name, color1, color2) {
    "use strict";
    
    var background_color = "white";
    var primary_color = color1;
    var secondary_color = color2;
    var agent_number;
    var agent_name = name;
    var painters = {};

    var getDimensions = function(state) {
        var maxX = 0;
        var maxY = 0;
        for (var i = 0; i < state.Walls.length; i++) {
            var wall = state.Walls[i];
            maxX = Math.max(maxX, wall.EndX);
            maxY = Math.max(maxY, wall.EndY);
        }
        return {x:maxX, y:maxY};
    };

    var getAgentNumber = function(state) {
        var number;
        for (var i = 0; i < state.Agents.length; i++) {
            var agent = state.Agents[i];
            console.log("agent.Name: %s agent_name: %s agent.Number: %f", agent.Name, agent_name, agent.Number);
            if (agent.Name === agent_name) {
                number = agent.Number;
                break;
            }
        }
        return number;
    }
    this.draw = function(state, width, height) {
        agent_number = getAgentNumber(state);
        var dims = getDimensions(state);
        var cell_width = width / dims.x;
        var cell_height = height / dims.y;
        console.log("Agent number %f", agent_number);
        
        drawObjects(state.Goals, painters["Goal"], cell_width, cell_height, height);
        drawObjects(state.Walls, painters["Wall"], cell_width, cell_height, height);
        drawObjects(state.Tolls, painters["Toll"], cell_width, cell_height, height);
        drawObjects(state.Agents, painters["Agent"], cell_width, cell_height, height);
    };

    var drawObjects = function(objects, painter, cell_width, cell_height, height) {
        if (typeof painter === 'undefined') {
            return;
        }
        for (var i = 0; i < objects.length; i++) {
            var object = objects[i];
            var color = getColorForObject(object);
            painter.draw(object, color, cell_width, cell_height, height);
            //break;
        }
    };

    var getColorForObject = function(object) {
        var color = "black";
        if (object instanceof Agent) {
            color = (object.Number === agent_number) ? primary_color : secondary_color;
        } else if (object instanceof Goal) {
            if (object.GoalType === 0) {
                color = "grey";
            } else if (object.GoalType === agent_number + 1) {
                color = primary_color;
            } else {
                color = secondary_color;
            }
        }
        if (object instanceof Toll) {
            color = "red";
        }
        if (object instanceof Wall) {
            color = "black";
        }
        return color;
    };


    this.addPainter = function(painter, classType) {
        if (!(classType in painters)) {
            painters[classType] = painter;
        }
    };

    this.removePainter = function(classType) {
        painters.remove(classType);
    };
};

var CellPainter = function(context, shape) {
    "use strict";
    
    this.CIRCLE = "circle";
    this.SQUARE = "square";
    
    this.draw = function(cell, color, cell_width, cell_height, canvas_height) {
        var left = cell.X * cell_width;
        var bottom = canvas_height - cell.Y * cell_height - cell_height;
        
        context.fillStyle = color;
                
        switch(shape) {
            case this.CIRCLE:
                
                var centerX = left + cell_width / 2.0;
                var centerY = 
                bottom + cell_height / 2.0;

                var radius = Math.min(cell_width, cell_height) / 2.0;
                
                context.beginPath();
                context.arc(centerX, centerY, radius, 0, 2.0 * Math.PI, false);
                context.fill();

                break;
            case this.SQUARE:
                context.fillRect(left, bottom, cell_width, cell_height);
                break;
            default:
                break;
        }
    };
};

var WallPainter = function(context) {
    "use strict";
    
    var thickness = 6;
   
    this.draw = function(wall, color, cell_width, cell_height, canvas_height) {
        var width = cell_width * (wall.EndX - wall.StartX) + thickness;
        var height = cell_height * (wall.EndY - wall.StartY) + thickness;

        width = Math.max(width, thickness);
        height = Math.max(height, thickness);

        var bottom = canvas_height;
        var left = cell_width * wall.StartX;
        if (width == thickness) {
            left -= width / 2.0;
            bottom += thickness/2.0;
        }
        
        bottom += - (cell_height * wall.StartY + height);
        if (height == thickness) {
            bottom += height / 2.0;
            left -= thickness / 2.0;
        }

        context.fillStyle = color;

        //console.log("cell.x1 %f cell.y1 %f cell.x2 %f cell.y2 %f ", wall.StartX, wall.StartY, wall.EndX, wall.EndY);
        //console.log("rect x %i rect y %i width %i height %i", left, bottom, width, height);
        
        context.fillRect(left, bottom, width, height);
    };

};


// Draws chosen action over screen
var ActionStatusPainter = function(context, agentColor) {
    "use strict";
    
    var background_color = "gray";
    var box_width = 128;
    var box_height = 64;
    
    this.draw = function(action, width, height) {
        $("#action_status").html();
        $("#action_status_background").html();

        if (typeof action !== 'undefined' && action !== null) {
            var boxPosX = width / 2.0;
            var boxPosY = height / 2.0;
            drawTransparentRect(width, height);
            $("#action_status").html(action);
            $("#action_status").css({"position":"absolute", "left": boxPosX + "px", "bottom": boxPosY + "px", "width": box_width  + "px", "height": box_height + "px", 
            "background-color": agentColor});
        }
    };

    var drawTransparentRect = function(width, height) {
        $("#action_status_background").css({"position":"absolute", "left":"0px", "bottom":"0px", "width": width  + "px", "height":height + "px", 
            "background-color": background_color});
    };

};

// Paints actions so they can be clicked
var ActionPainter = function(_canvas, _context, agentColor) {
    "use strict";
    
    var canvas = _canvas;
    var context = _context
    var actions = {"North":"north", "South":"south", "East":"east", "West":"west", "Wait":"noop"};
    var positions = {"North":[0, 1], "South":[0,-1], "East":[1,0], "West":[-1,0], "Wait":[0,0]}
    var bar_width = 100;
    var bar_height = 32;
    var spacing = 3;
    var color = agentColor;
    var chosen_action_color = "red";
    var textColor = "black";

    var getButtonPosition = function(startX, startY, buttonName) {
        var location = positions[buttonName];
        var actualX = bar_width + startX + location[0] * (bar_width + spacing);
        var actualY = bar_height + startY - location[1] * (bar_height + spacing);
        return {x:actualX, y:actualY};
    };
    
    this.draw = function(x, startY, current_action) {
        
        for (var key in actions) {
            var position = getButtonPosition(x, startY, key);
            draw_bar(key, actions[key], position.x, position.y, current_action);
        }
        draw_waiting(x, startY + this.width(), current_action);
    };

    var draw_bar = function(text, divName, x, y, current_action) {
        console.log("Drawing action " + current_action);
        if (typeof current_action === 'undefined' || current_action == null ||
            current_action !== text) {
            context.fillStyle = color;    
        } else {
            context.fillStyle = chosen_action_color;
        }
        
        context.fillRect(x, y, bar_width, bar_height);
        context.strokeStyle = textColor;
        context.strokeRect(x, y, bar_width, bar_height);
        context.font = "bold 14pt sans-serif";
        context.textAlign = "center";
        context.fillStyle = textColor;
        context.fillText(text, x + bar_width / 2.0, y + bar_height - 10);
    };

    var draw_waiting = function(x, y, current_action) {
        if (typeof current_action !== 'undefined') {
            context.font = "bold 14pt sans-serif";
            context.textAlign = "left";
            context.fillStyle = textColor;
            context.fillText("Waiting for partner's action...", x + 20, y);
        }
        
    };

    this.width = function() {
        return (bar_width + spacing) * 3;
    };

    this.height = function() {
        return (bar_height  + spacing) * 3;
    };

    this.loadButtons = function(x, y) {
        var buttons = [];

        for (var key in actions) {
            var position = getButtonPosition(x, y, key);
            var button = new Button(key, position.x, position.y, bar_width, bar_height);
            buttons.push(button);
        }

        return buttons;
    };
};

// Displays score, agent color, and available keyboard commands. 
var StatusPainter = function(context, agentColor) {
    "use strict";
    var textColor = "black";

    this.draw = function(score, x, y) {
        context.font = "bold 14pt sans-serif";
        context.textAlign = "left";
        context.fillStyle = textColor;
        context.fillText("Score: " + score, x + 20, y + 128);
    };

    this.height = function() {
        return 128;
    };
};

var WorldsPainter = function(context) {
    "use strict";
    
    var worlds;

    this.setWorlds = function(w) {
        worlds = w;
    };
};

var ActiveWorldsPainter = function(context) {
    "use strict";
    
    var active;

    this.setActive = function(a) {
        active = a;
    };
};

var OpeningScreenPainter = function(width, height) {
    "use strict";
    var canvas = document.getElementById("GGCanvas");
    var context = canvas.getContext("2d");
    
    this.draw = function(textBox, button) {
        context.fillStyle = "black";
        context.fillRect(0,0, width, height);
        textBox.style.left = width/2.0;
        textBox.style.bottom = height/2.0;
        textBox.style.align = "center";
        textBox.style.position = "absolute";
        button.style.left = width/2.0;
        button.style.bottom = height/2.0 - 30;
        button.style.align = "center";
        button.style.position = "absolute";
    };
};

var EndScreenPainter = function(width, height, context) {
    "use strict";
    var textColor = "white";
    var background = "black";

    this.draw = function(score) {
        context.fillStyle = background;
        context.fillRect(0,0, width, height);
        context.font = "bold 48pt sans-serif";
        context.textAlign = "center";
        context.fillStyle = textColor;
        context.fillText("Score: " + score, width / 2.0, height / 2.0);
        
    };
};

var AdminPagePainter = function(onWorldClick, onActiveClick) {
    "use strict";
    var worldButtons = {};
    var activeButtons = {};
    var onWorld = onWorldClick;
    var onActive = onActiveClick;

    var allElements = [];
    var self = this;

    this.clear = function() {
        for (var i = 0; i < allElements.length; i++) {
            document.body.removeChild(allElements[i]);
        }
        allElements = [];
        worldButtons = {};
        activeButtons = {};
    };

    this.draw = function(worlds, active) {
        this.clear();

        drawTitles(100, 80, 100, 400);        
        drawButtons(worlds, worldButtons, onWorld);
        drawButtons(active, activeButtons, onActive);

        placeButtons(100, 100, worldButtons);
        placeButtons(100, 420, activeButtons);
            
    };

    var drawTitles = function(worldsX, worldsY, activeX, activeY) {
        var startWorldDiv = document.createElement("div");
        var startWorldText = document.createTextNode("Start new game");
        var configureActiveDiv = document.createElement("div");
        var configureActiveText = document.createTextNode("Configure/Run Existing Game");

        startWorldDiv.appendChild(startWorldText);
        configureActiveDiv.appendChild(configureActiveText);

        document.body.appendChild(startWorldDiv);
        allElements.push(startWorldDiv);
        document.body.appendChild(configureActiveDiv);
        allElements.push(configureActiveDiv);
        startWorldDiv.style.position = "absolute";
        configureActiveDiv.style.position = "absolute";

        startWorldDiv.style.left = worldsX + "px";
        startWorldDiv.style.top = worldsY + "px";

        configureActiveDiv.style.left = activeX + "px";
        configureActiveDiv.style.top = activeY + "px";
    };

    var drawButtons = function(worlds, buttons, callback) {
        
        for (var i = 0; i < worlds.length; i++) {
            
            var label = worlds[i].Label;
            var description = worlds[i].Description;
            if (!(label in buttons)) {
                var button = document.createElement("BUTTON");
                button.title = description;
                button.id = label;
                button.addEventListener("click", function() {callback(this.id);});
                
                var text = document.createTextNode(label);
                button.appendChild(text);
                buttons[label] = button;

                var descripDiv = document.createElement('div');
                var descrText = document.createTextNode(description);
                descripDiv.id = label + "_desc";
                descripDiv.appendChild(descrText);

                document.body.appendChild(button);
                document.body.appendChild(descripDiv); 
                
                allElements.push(button);
                allElements.push(descripDiv);
            }
        }
    };


    var placeButtons = function(startX, startY, buttons) {
        var offset = 0;
        for (var key in buttons) {
            var button = document.getElementById(key);
            if (typeof button === 'undefined' || button === null) {
                continue;
            }
            button.style.position = "absolute";
            button.style.left = startX + "px";
            button.style.top = (startY + offset) + "px";
            button.style.width = "64px";
            
            var description = document.getElementById(key + "_desc");
            description.style.position = "absolute";
            description.style.left = (startX + 70) + "px";
            description.style.top = (startY + offset) + "px";

            offset += 24;
                
        }
    };
};

var GameConfigPainter = function(_label, _description, _submitConfigCallback, _onRunCallback, _onRemoveCallback) {
    "use strict";

    var configChange = _submitConfigCallback;
    var onRunCallback = _onRunCallback;
    var onRemoveCallback = _onRemoveCallback;
    var self = this;
    var agentMenus = [];
    var allElements = [];
    var label = _label;
    var description = _description;

    var onSubmit = function() {
        var agentConfigurations = [];
        for (var i = 0; i < agentMenus.length; i++) {
            agentConfigurations.push(agentMenus[i].value);
        }

        self.clear();

        configChange(label, agentConfigurations);
        
    };

    var onRun = function() {

        self.clear();
        onRunCallback(label);
    };

    var onRemove = function() {
        self.clear();
        onRemoveCallback(label);
    };

    var agentTypes = ["random", "cooperative", "human"];

    this.clear = function() {
        for (var i = 0; i < allElements.length; i++) {
            document.body.removeChild(allElements[i]);
        }
        allElements = [];
    };
    this.draw = function(agents, maxAgents, x, y) {
        if (maxAgents == -1) {
            maxAgents = 2;
        }
        drawText(label, description, x, y);
        var offset = drawAgents(agents, maxAgents, x + 20, y + 40);
        var buttonY =  offset + y + 40;
        drawButton(x, buttonY, "Submit", onSubmit);
        drawButton(x + 60, buttonY, "Run", onRun);
        drawButton(x + 180, buttonY, "Remove", onRemove);
    };

    var drawButton = function(x, y, text, callback) {
        var button = document.createElement("button");
        var buttonText = document.createTextNode(text);
        button.appendChild(buttonText);
        allElements.push(button);

        document.body.appendChild(button);
        button.style.position = "absolute";
        button.style.top = y + "px";
        button.style.left = x + "px";
        button.addEventListener("click", callback);
    };

    var drawText = function(label, description, x, y) {
        var headerDiv = document.createElement('div');
        var headerText = document.createTextNode("Game \u00A0 Description");
        headerDiv.appendChild(headerText);
        allElements.push(headerDiv);

        var labelDiv = document.createElement('div');
        var labelText = document.createTextNode(label);
        labelDiv.appendChild(labelText);
        allElements.push(labelDiv);

        var descriptionDiv = document.createElement('div');
        var descriptionText = document.createTextNode(description);
        descriptionDiv.appendChild(descriptionText);
        allElements.push(descriptionDiv);

        document.body.appendChild(headerDiv);
        document.body.appendChild(labelDiv);
        document.body.appendChild(descriptionDiv);

        headerDiv.style.position = "absolute";
        headerDiv.style.left = (x - 12) + "px";
        headerDiv.style.top = (y - 20) + "px";

        labelDiv.style.position = "absolute";
        labelDiv.style.left = x + "px";
        labelDiv.style.top = y + "px";

        descriptionDiv.style.position = "absolute";
        descriptionDiv.style.left = (x + labelDiv.clientWidth + 30) + "px";
        descriptionDiv.style.top = y + "px";
    };



    var drawAgents = function(agents, numAgents, x ,y) {
        var offset = 24;
        var count = 0;

        var headerDiv = document.createElement('div');
        var headerText = document.createTextNode("Agent \u00A0\u00A0 Type");
        headerDiv.appendChild(headerText);
        allElements.push(headerDiv);

        document.body.appendChild(headerDiv);
        headerDiv.style.position = "absolute";
        headerDiv.style.top = (y) + "px";
        headerDiv.style.left = (x-20) + "px";

        for (var key in agents) {
            var agent = agents[key];
            count++;
            var numberDiv = document.createElement('div');
            var numberText = document.createTextNode(key);
            numberDiv.appendChild(numberText);
            allElements.push(numberDiv);
            document.body.appendChild(numberDiv);
            numberDiv.style.position = "absolute";
            numberDiv.style.top = (y + offset) + "px";
            numberDiv.style.left = x + "px";

            var descripDiv = document.createElement('div');
            var descripText = document.createTextNode(agent);
            descripDiv.appendChild(descripText);

            descripDiv.style.position = "absolute";
            descripDiv.style.top = (y + offset) + "px";
            descripDiv.style.left = (x + 30) + "px";

            document.body.appendChild(descripDiv);
            allElements.push(descripDiv);
            offset += 24;
        }

        for (var i = count; i < numAgents; i++) {
            var numberDiv = document.createElement('div');
            var numberText = document.createTextNode(i);
            numberDiv.appendChild(numberText);
            allElements.push(numberDiv);

            document.body.appendChild(numberDiv);
            numberDiv.style.position = "absolute";
            numberDiv.style.top = (y + offset) + "px";
            numberDiv.style.left = x + "px";

            var select = createDropDown(i, agentTypes);
            select.style.position = "absolute";
            select.style.top = (y + offset) + "px";
            select.style.left = (x + 30) + "px";
            offset += 24;
        }

        return offset;
    };

    var createDropDown = function(agentNumber, agentTypes, selectedType) {

        var select = document.createElement("select");
        allElements.push(select);
        agentMenus.push(select);
        select.id = "agent_" + agentNumber;

        for (var i = 0; i < agentTypes.length; i++) {
            var option = document.createElement("option");
            option.value = agentTypes[i];
            if (agentTypes[i] === selectedType) {
                option.selected = true;
            }
            option.innerHTML = agentTypes[i];
            select.add(option);
        }
        document.body.appendChild(select);
        return select;
    };


};

var ConnectionStatusPainter = function(tryConnectCallback, disconnectCallback) {
        var allElements = [];
        var tryConnect = tryConnectCallback;
        var disconnect = disconnectCallback;
        var textBox;
        this.clear = function() {
            for (var i = 0; i < allElements.length; i++) {
                document.body.removeChild(allElements[i]);
            }
            allElements = [];
        };

        var onConnectClick = function() {
            if (typeof textBox !== 'undefined') {
                var serverText = textBox.value;
                var result = tryConnect(serverText);

                if (result !== 0) {
                    var textDiv = document.createElement('div');
                    var text = document.createTextNode(result);
                    textDiv.appendChild(text);
                    document.body.appendChild(textDiv);
                    allElements.push(textDiv);

                    textDiv.style.position = "absolute";
                    textDiv.style.top = "50px";
                    textDiv.style.left = "10px";
                }
            }
        };  

        var onDisconnectClick = function() {
            disconnect();
        };

        this.draw = function(isConnected, server, x, y) {
            this.clear();

            var headerDiv = document.createElement('div');
            var str = (isConnected) ? " Connected to " + server : "Disconnected";
            var connectedText = document.createTextNode(str);
            headerDiv.appendChild(connectedText);
            allElements.push(headerDiv);
            document.body.appendChild(headerDiv);

            headerDiv.style.position = "absolute";
            headerDiv.style.top = y + "px";
            headerDiv.style.left = x + "px";

            if (!isConnected) {
                textBox = document.createElement('input');
                textBox.value = server;
                textBox.style.position = "absolute";
                textBox.left = x + "px";
                textBox.top = (y + 20) + "px";

                var button = document.createElement("button");
                var buttonText = document.createTextNode("Connect");
                button.appendChild(buttonText);
                button.addEventListener("click", onConnectClick);
                button.style.position = "absolute";
                button.style.left = (x) + "px";
                button.style.top = (y + 20) + "px";

                document.body.appendChild(textBox);
                allElements.push(textBox);
                document.body.appendChild(button);
                allElements.push(button);
            } else {
                var button = document.createElement("button");
                var buttonText = document.createTextNode("Disconnect");
                button.appendChild(buttonText);
                button.addEventListener("click", onDisconnectClick);

                document.body.appendChild(button);
                allElements.push(button);

                button.style.position = "absolute";
                button.style.left = x + "px";
                button.style.top = (y + 20) + "px";
            }
            
        };  
    };