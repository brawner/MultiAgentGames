

var Agent = function(name, playerNum, posX, posY) {
    "use strict";

    this.Name = name;
    this.Number = playerNum;
    this.X = posX;
    this.Y = posY;
};

var Goal = function(name, goalType, posX, posY) {
    "use strict";

    this.Name = name;
    this.GoalType = goalType;
    this.X = posX;
    this.Y = posY;
};

var Wall = function(name, wallType, startX, startY, endX, endY) {
    "use strict";

    this.Name = name;
    this.StartX = startX;
    this.StartY = startY;
    this.EndX = endX;
    this.EndY = endY;
};

var Toll = function(name, cost, posX, posY) {
    "use strict";

    this.Name = name;
    this.Cost = cost;
    this.X = posX;
    this.Y = posY;
};

var State = function(agents, goals, walls, tolls) {
    "use strict";

    this.Agents = agents;
    this.Goals = goals;
    this.Walls = walls;
    this.Tolls = tolls;
};

var World = function(label, description, maxAgents) {
    "use strict";

    this.Label = label;
    this.Description = description;
    this.NumAgents = maxAgents;
};

var ActiveWorld = function(label, description, agents, maxAgents) {
    "use strict";

    this.Label = label;
    this.Description = description;
    this.Agents = agents;
    this.NumAgents = maxAgents;
}

var AgentConfig = function(type, number) {
    "use strict";

    this.Type = type;
    this.Number = number;
};