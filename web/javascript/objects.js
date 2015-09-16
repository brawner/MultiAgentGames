// Any explicit objects are specified here. Used for state descriptions, world descriptions, 
// game descriptions, and agent descriptions

/**
 * @constructor
 */ 
var Agent = function(name, playerNum, posX, posY) {
    "use strict";

    this.Name = name;
    this.Number = playerNum;
    this.X = posX;
    this.Y = posY;
};

/**
 * @constructor
 */
var Goal = function(name, goalType, posX, posY) {
    "use strict";

    this.Name = name;
    this.GoalType = goalType;
    this.X = posX;
    this.Y = posY;
};

/**
 * @constructor
 */
var Wall = function(name, wallType, startX, startY, endX, endY) {
    "use strict";

    this.Name = name;
    this.StartX = startX;
    this.StartY = startY;
    this.EndX = endX;
    this.EndY = endY;
};

/**
 * @constructor
 */
var Toll = function(name, cost, posX, posY) {
    "use strict";

    this.Name = name;
    this.Cost = cost;
    this.X = posX;
    this.Y = posY;
};

/**
 * @constructor
 */
var State = function(agents, goals, walls, tolls) {
    "use strict";

    this.Agents = agents;
    this.Goals = goals;
    this.Walls = walls;
    this.Tolls = tolls;
};

/**
 * @constructor
 */
var World = function(label, description, maxAgents) {
    "use strict";

    this.Label = label;
    this.Description = description;
    this.NumAgents = maxAgents;
};

/**
 * @constructor
 */
var ActiveWorld = function(label, description, agents, maxAgents) {
    "use strict";

    this.Label = label;
    this.Description = description;
    this.Agents = agents;
    this.NumAgents = maxAgents;
}

/**
 * @constructor
 */
var AgentConfig = function(type, number) {
    "use strict";

    this.Type = type;
    this.Number = number;
};