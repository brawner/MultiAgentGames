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

/*
var SplashPainter = function(startOnClick, textOnKeyDown) {
    "use strict";
    var painters = [];
    var allowReset = true;

    this.draw = function() {
         $("#stage").append("<div id='"+gameConfig.SceneSplashName+"'></div>");
        
        //Title
        $("#"+gameConfig.SceneSplashName).append("<h1 id='splashTitle'>" + gameConfig.Title + "</h1>");
        //console.log("Debug: Splash Scene -> Generating Title at " + gamePos.TitleY + " " + gamePos.TitleX);
        $("#splashTitle").css({
            "color": gameConfig.TitleColor,
            "font-size": gameConfig.TitleSize,
            "position":"absolute",
            "top":gamePos.TitleY,
            "left":gamePos.TitleX});
        
        //Name input
        $("#"+gameConfig.SceneSplashName).append("<input id='playerName' type='text'>");
        //console.log("Debug: Splash Scene -> Generating Player Name text input at...");

        $("#playerName").css({
            "width":256,
            "position":"relative",
            "top":240,
            "left":228}).val('Enter thy name Chef!')
            .keydown(function() {
                allowReset = false;
                textOnKeyDown();})
            .click(function() {if (allowReset) {$("#playerName").val("");}});

        //Start button
        //console.log("Debug: Splash Scene -> Generating Start button at " + gamePos.StartY + " " + gamePos.StartX);
        $("#"+gameConfig.SceneSplashName).append("<button id='splashButton' type='button'>" + gameConfig.StartButton + "</button>");
        $("#splashButton").css({
            "position":"absolute",
            "top":gamePos.StartY,
            "left":gamePos.StartX})
            .click(startOnClick);
    };

    this.addPainter = function(painter) {
        painters.push(painter);
    };
};

var GamePainter = function(playground, mouseTracker) {
    "use strict";
    
    var painters = [];
    playground.pauseGame().clearScenegraph().startGame().addGroup(
            "background", {width: gameConfig.StageWidth, height: gameConfig.StageHeight}).addSprite(
            "background", {animation: gameAnimations.background1, width: gameConfig.StageWidth, height: gameConfig.StageHeight}).end()
        .mousemove(mouseTracker.onMouseMove)
        .mousedown(mouseTracker.onMouseDown)
        .mouseup(mouseTracker.onMouseUp)
        .addGroup("grid", {width: 768, height: 256, posx: 0, posy: 192}).end()    
        .addGroup("holding", {width: 756, height: 512, posx: 0, posy: 0})
            .end()
        .addGroup("selectionDiv", {width: 756, height: 512, posx: 0, posy: 0})
            .end()
        .addGroup("recipeBackground", {width: 384, height: 192, posx: 384, posy: 0})
            .css({"background-image": "url('./Sprites/RecipeDivBG.PNG')", "overflow": "visible"})
            .addGroup("recipeDiv", {width: 374, height: 186, posx: 10, posy: 6})
                .css({"font-size": "10pt", "color": "black", "overflow": "auto"})
                .end().end()
        .addGroup("consoleBackground", {width: 384, height: 192, posx: 0, posy: 0})
            .css({"background-image": "url('./Sprites/TerminalDivBG.PNG')", "overflow": "visible"})
            .addGroup("consoleDiv", {width: 364, height: 180, posx: 10, posy: 6})
                .css({"font-size": "10pt", "color": "green", "overflow": "auto"})
                .end().end()
        .addGroup("actionDiv",{width: 768, height: 64, posx: 0, posy: 448})
            .css({"font-size": "8pt", "color": "yellow", "overflow": "auto"})
            .addSprite("actSelector", {animation: gameAnimations.overSelectionP1, width: 64, height: 64, posx: 0})
            .append("<button id='matchResetBtn' type='button'>Reset</button>")
            .end()
        .addGroup(actionText.DisplayDiv, {width: 256, height: 64, posx: 448, posy: 448})
            .end();

    this.draw = function () {
        var i = 0;  
        for (i = 0; i < painters.length; i++) {
            painters[i].draw();
        }
    };

    this.addPainter = function (painter) {
        painters.push(painter);
    };

    this.addPainters = function(newPainters) {
        painters = painters.concat(newPainters);
    };
};

var GridPainter = function() {
    "use strict";
    var painters = [];
    
    var initGroup = function() {
        if ($("#appliances").length === 0) {
            $("#grid").addGroup("appliances", {width: 384, height: 128, posx: 0, posy: 0}).end()
        }
        if ($("#containers").length === 0) {
            $("#grid").addGroup("containers", {width: 768, height: 64, posx: 0, posy: 128}).end()
        }
        if ($("#ingredients").length === 0) {
            $("#grid").addGroup("ingredients", {width: 768, height: 64, posx: 0, posy: 192}).end()
        }
    };
    initGroup();

    this.addPainter = function(painter) {
        painters.push(painter);
    };

    this.removePainter = function(painter) {
        var position = $.inArray(painter, painters);
        if (position != -1) 
        {
            painters[position].clear();
            painters.splice(position, 1);
        }
    };

    this.setPainters = function(newPainters) {
        painters = newPainters;
    };

    this.getBounds = function() {
        var left = $("#grid").x();
        var right = $("#grid").x() + $("#grid").width();
        var bottom = $("#grid").y();
        var top = $("#grid").y() + $("#grid").height();
        return {top:top, bottom:bottom, left:left, right:right};
    };  

    this.clear = function() {
        for (var i = 0; i < painters.length; i++) {
            painters[i].clear();
        }
        painters = [];
        clearGroup($("#appliances"));
        clearGroup($("#containers"));
        clearGroup($("#ingredients"));
    };

    var clearGroup = function(group) {
        for (var i = 0; i < group.length; i++) {
            group.remove();
        }
        initGroup();
    };

    this.draw = function() {
        for (var i = 0; i < painters.length; i++) {
            painters[i].draw();
        }
    };
};

var ActionBarPainter = function(usedActions, onClick, onReset) {
    "use strict";
    var actions = usedActions,
        selector = 0;
    var DisplayDiv = "actionDiv";



    //Configure reset button
    $("#matchResetBtn").css({
        "position":"absolute",
        "top":16,
        "left":708}).click(onReset);     
        


    this.setSelector = function(position) {
        if (0 <= position && position < actions.length) {
            selector = position;
        }
        this.draw();
    };

    this.draw = function() {
        var i = 0,
            x = 0,
            action = 0,
            actionDiv = "",
            animation = null;
        var actionGroup = $("#" + DisplayDiv);

        for (i = 0; i < actions.length; i++) {
            x = i * 64;
            action = actions[i];
            animation = getSprite(action);
            actionDiv = "action_" + i.toString();

            actionGroup.addSprite(actionDiv, {animation: animation, width: 64, height: 64, posx: x});
            $("#" + actionDiv).click(onClick);
        }

        $("#actSelector").x(64 * selector);
    };

    this.getBounds = function() {
        var div = $("#" + DisplayDiv);
        return {"left":div.x(), "right":div.x() + div.width(), "bottom":div.y(), "top": div.y() + div.height()};
    };

    var getSprite = function(action) {
        switch(action) {
        case EnumActions.Look:
            return gameAnimations.actLook;
            break;
        case EnumActions.Use:
            return gameAnimations.actUse;
            break;
        case EnumActions.Mix:
            return gameAnimations.actMix;
            break;
        case EnumActions.Spread:
            return gameAnimations.actSpread;
            break;
        case EnumActions.TurnOnOff:
            return gameAnimations.actTurnOnOff;
            break;
        case EnumActions.Peel:
            return gameAnimations.actPeel;
            break;
        case EnumActions.Shape:
            return gameAnimations.actShape;
            break;
        case EnumActions.Cut:
            return gameAnimations.actCut;
            break;
        default:
            return -1;
        }
        return -1;
    };
};

var AppliancePainter = function(sprite, posx, posy, currentSlot, containerPainters) {
    "use strict";

    var x = posx;
    var y = posy;
    var group = "appliances";
    var slot = currentSlot;
    var containers = containerPainters;
    var applianceGroup = "appliances" + "_" + slot.toString();
    var animation;
    var spritePainter;
    var imageUrl = "./Sprites/" + sprite;
    var self = this;

    var imageExists = function() {
        animation = new $.gameQuery.Animation({imageURL: imageUrl });
        self.draw();
    };

    var imageNotExists = function() {
        animation = new $.gameQuery.Animation({imageURL: "./Sprites/Appliance.PNG"})
        spritePainter = new SpritePainter(sprite, "sprite_" + sprite, applianceGroup);
        spritePainter.setSize(128,128);
        self.draw();
    };

    $.get(imageUrl)
        .done(imageExists).fail(imageNotExists);

    this.clear = function() {
        removeAll();
    };

    var groupObject = function() {
        return $("#" + applianceGroup.toString());
    };

    var slotObject = function() {
        return $("#" + slot.toString());
    };

    var getGroupObject = function() {
        if (groupObject().length === 0) {
            setApplianceGroup();
        }

        return groupObject();
    };

    var getSlotObject = function() {
        if (slotObject().length === 0) {
            setSprite();
        }

        return slotObject();
    };

    var removeAll = function() {
        slotObject().remove();
        groupObject().remove();
    };

    var setApplianceGroup = function() {
        groupObject().remove();
        
        $("#" + group).addGroup(applianceGroup.toString(), 
                {width: 128, height: 128, posx: x, posy: y});
    };

    var setSprite = function() {
        slotObject().remove();
        $("#" + applianceGroup).addSprite(slot.toString(), 
                {animation:animation, width: 128, height: 128});
    };

    var setContainers = function() {
        var containerSlot,
        containerX,
        containerY,
        containerGroup;

        for (var i = 0; i < containers.length; i++) {
            containerX = (i % 2) * 64;
            containerY = Math.floor(i / 2) * 64;
            containerSlot = slot + "_" + i.toString();

            containers[i].setConfiguration(containerSlot, containerX, containerY, applianceGroup);
        }
    };

    var setAll = function() {
        setApplianceGroup();
        setSprite();
        setContainers();
    };

    this.addPainter = function(newContainer) {
        containers.push(newContainer);
    };

    this.setPainters = function(newContainers) {
        containers = newContainers;
    };

    this.removePainter = function(toRemove) {
        var position = $.inArray(toRemove, containers);
        if (position != -1) {
            containers.splice(position, 1);
        }
    }

    this.setPosition = function(newX, newY) {
        removeAll();
        x = newX;
        y = newY;
    };
    this.setGroup = function(newGroup) {
        removeAll();
        group = newGroup;
        applianceGroup = group.toString() + "_" + slot.toString();
    };

    this.setAnimation = function(newAnimation) {
        removeAll();
        animation = newAnimation;
    };

    this.setSlot = function(newSlot) {
        removeAll();
        slot = newSlot;
        applianceGroup = group.toString() + "_" + slot.toString();
    };

    this.setConfiguration = function(newSlot, newX, newY, newGroup) {
        removeAll();
        if (typeof newSlot !== 'undefined') {
            slot = newSlot;
            applianceGroup = group.toString() + "_" + slot.toString();    
        }
        if (typeof newX !== 'undefined') {
            x = newX;
        }
        if (typeof newY !== 'undefined') {
            y = newY;
        }
        if (typeof newGroup !== 'undefined') {
            group = newGroup;
            applianceGroup = group.toString() + "_" + slot.toString();
        }
    };

    this.draw = function() {
        setAll();
        if (typeof spritePainter !== 'undefined') {
            spritePainter.draw();
        }
        for (var i = 0; i < containers.length; i++) {
            containers[i].draw();
        }
    };
};

var ContainerPainter = function(text, sprite, posx, posy, currentSlot, containerGroup) {
    "use strict";
    var group = (typeof containerGroup !== 'undefined') ? containerGroup : "containers";
    var containerGroup = group + "_" + currentSlot.toString();
    var imageUrl = "./Sprites/" + sprite;
    var animation,
        spritePainter;
    var slot = currentSlot;
    var x = posx;
    var y = posy;
    var self = this;
    var initialized = false;

    var imageExists = function() {
        animation = new $.gameQuery.Animation({imageURL: imageUrl });
        initialized = true;
        self.draw();
    };

    var imageNotExists = function() {
        animation = new $.gameQuery.Animation({imageURL: "./Sprites/Container.PNG"})
        spritePainter = new SpritePainter(text, "sprite_" + sprite, containerGroup);
        initialized = true;
        self.draw();
    };

    $.get(imageUrl)
        .done(imageExists).fail(imageNotExists);

    var slotObject = function() {
        return $("#" + slot.toString());
    };

    var containerObject = function() {
        return $("#" + containerGroup);
    };

    var groupObject = function() {
        return $("#" + group);
    };

    this.clear = function() {
        this.clearAnimation();
    }

    

    var drawSprite = function() {
        if (typeof group !== 'undefined') {
            if (containerObject().length === 0)
            {
                groupObject().addGroup(containerGroup.toString(), 
                    {width: 64, height: 64, posx: x, posy: y})
            }
            if (slotObject().length === 0)
            {
                containerObject().addSprite(slot.toString(), 
                    {animation: animation, width: 64, height: 64});
            }
        }
    };

    this.setPosition = function(newX, newY) {
        slotObject().remove();
        x = newX;
        y = newY;
    };

    this.setGroup = function(newGroup) {
        slotObject().remove();
        group = newGroup;
        containerGroup = group + "_" + slot.toString();
        if (typeof spritePainter !== 'undefined') 
        {
            spritePainter.setGroup(containerGroup);
        }
    };

    this.setAnimation = function(newAnimation) {
        animation = newAnimation;
    };

    this.clearAnimation = function() {
        if (slotObject().length !== 0) {
            slotObject().setAnimation();
            slotObject().remove();
        }
    };

    this.setSlot = function(newSlot) {
        slotObject().remove();
        slot = newSlot;
        containerGroup = group + "_" + slot.toString();
        if (typeof spritePainter !== 'undefined') 
        {
            spritePainter.setGroup(containerGroup);
        }
    };

    this.setConfiguration = function(newSlot, newX, newY, newGroup) {
        slotObject().remove();

        if (typeof newSlot !== 'undefined') {
            slot = newSlot;
        }
        if (typeof newX !== 'undefined') {
            x = newX;
        }
        if (typeof newY !== 'undefined') {
            y = newY;
        }
        if (typeof newGroup !== 'undefined') {
            group = newGroup;
        }
        containerGroup = group + "_" + slot.toString();
        if (typeof spritePainter !== 'undefined') 
        {
            spritePainter.setGroup(containerGroup);
        }        
    };

    this.draw = function() {
        if (slot != "-1" && initialized) {
            drawSprite();
            if (typeof spritePainter !== 'undefined') {
                spritePainter.draw();   
            }
        }
    };
};

var RecipePainter = function() {
    "use strict";
    var x, y;
    var text;
    var status;
    var div = "recipeDiv";

    var recipeDiv = function() {
        return $("#" + div);
    }

    this.SetX = function(newX) {
        x = newX;
    };

    this.SetY = function(newY) {
        y = newY;
    };

    this.setText = function(newText) {
        text = newText;
        if (typeof status === 'undefined') {
            status = [];
            for (var i = 0; i < text.length; i++) {
                status[i] = false;
            }
        }
    };

    this.setStatus = function(newStatus) {
        status = newStatus;
        for (var i = status.length; i < text.length; i++) {
            status[i] = false;
        }
    };

    this.draw = function(){
        if (typeof text === 'undefined') {
            return;
        }
        recipeDiv().html("");
        recipeDiv().append("<u>" + text[0] + "</u>");
        recipeDiv().append("<ol>");
        var color;
        for (var i = 1; i < text.length; i++) {
            color = (status[i-1]) ? "gray" : "black";
            recipeDiv().append("<li class='recipe_" + color + "'>" + text[i] + "</li>");
        }
        recipeDiv().append("</ol>");

        $(".recipe_gray").css({"color":"gray"});
        $(".recipe_black").css({"color":"black"});
    };
};

$.fn.textHeight = function()
 {
   var self = $(this),
         children = self.children(),
         calculator = $('<span style="display: inline-block;" />'),
         height;
 
    var selfHeight = self.height();
     
     children.wrap(calculator);
     height = children.parent().height(); // parent = the calculator wrapper
     children.unwrap();
     return height * children.length;
 };

var MatchConsolePainter = function() {
    "use strict";
    var DisplayDiv = "consoleDiv";
    var text = [];
    var error = [];
    var height = $("#" + DisplayDiv).height() - 8;

    var divObject = function() {
        return $("#" + DisplayDiv);
    }

    this.setText = function(lines, status) {
        text = lines;
        error = status;
    };

    this.draw = function() {
        divObject().html("");
        var color;
        for (var i = 0; i < text.length; i++) {
            color = (error[i] == 0) ? "console_green" : "console_red";
            divObject().append("<span class='" + color + "'>" + text[i] + "</span></br>");
        }
        $(".console_green").css({"color":"green"});
        $(".console_red").css({"color":"red"});
        var scrollAmount = divObject().textHeight();
       divObject().scrollTop(scrollAmount);
    };
};

var HoldingBoxPainter = function() {
    "use strict";
    var x, y;

    $("#holding").addGroup("holdingBox", {width:64, height:64}).end();

    var holdingGroup = "holdingBox";

    var holdingObjectPainter;

    this.clear = function() {
        this.clearHoldingObjectPainter();
    };

    var groupObject = function() {
        return $("#" + holdingGroup);
    };

    var getGroupObject = function() {
        if (groupObject().length === 0) {
            setGroupObject();
        }
        return groupObject();
    };

    var setGroupObject = function() {
        $("#holding").addGroup("holdingBox", {width:64, height:64}).end();        
    };

    this.setPosition = function(newX, newY) {
        x = newX;
        y = newY;
        this.draw();
    };

    this.setHoldingObjectPainter = function(objPainter, tileX, tileY) {
        //objPainter.clearAnimation();
        //objPainter.setGroup();

        holdingObjectPainter = objPainter;
        holdingObjectPainter.setGroup(holdingGroup);
        holdingObjectPainter.setPosition(-tileX,-tileY);
        this.draw();
    };

    this.clearHoldingObjectPainter = function() {
        if (holdingObjectPainter) {
            holdingObjectPainter.clearAnimation();
            holdingObjectPainter.setGroup();
        }

        holdingObjectPainter = undefined;

        groupObject().remove();
        groupObject().html();
    };

    this.draw = function() {
        var obj = getGroupObject();
        obj.x(x);
        obj.y(y);

        if (typeof holdingObjectPainter !== 'undefined') {
            holdingObjectPainter.draw();
        }
    };
};*/