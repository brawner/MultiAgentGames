package networking.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.stochasticgame.PolicyFromJointPolicy;
import burlap.behavior.stochasticgame.agents.RandomAgent;
import burlap.behavior.stochasticgame.agents.mavf.MultiAgentVFPlanningAgent;
import burlap.behavior.stochasticgame.mavaluefunction.backupOperators.MaxQ;
import burlap.behavior.stochasticgame.mavaluefunction.policies.EGreedyMaxWellfare;
import burlap.behavior.stochasticgame.mavaluefunction.vfplanners.MAValueIteration;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;

/**
 * The GridGameConfiguration class tracks how a game is to be configured. Instead of configuring a world, the configuration
 * keeps track of the base world, the different agent types and any other information that needs to be tracked for a game.
 * 
 * @author brawner
 *
 */
public class GridGameConfiguration {

	/**
	 * The base type of world for this configuration.
	 */
	private final World baseWorld;
	
	/**
	 * A list of the agents in their assigned order. I.e. the first agent added will be agent 0, regardless of what type it is.
	 */
	private final List<String> orderedAgents;
	
	/**
	 * If an Agent needs to precompute a policy and should be recycled from each run, then it should be a repeated agent. Becareful to make sure
	 * that any repeated agent is not shared with a different game, and all data members are fully copied when added to this configuration.
	 */
	private final Map<String, Agent> repeatedAgents;
	
	/**
	 * If a simple agent can be regenerated anytime a new game is started, or when this game is restarted then it would be a regenerated agent.
	 */
	private final Map<String, String> regeneratedAgents;
	
	/**
	 * Humans that connect to the game are network agents. This map maps the agent names with the game handler.
	 */
	private final Map<String, GameHandler> networkAgents;
	
	/**
	 * This map maps the client ids to their handlers.
	 */
	private final Map<String, GameHandler> handlerLookup;
	
	/**
	 * The game scores that are tracked through an games run. This should be modified at the end of each run with how reward maps to overall score
	 */
	private final Map<String, Double> scores;
	
	/**
	 * Once all the agents have been assigned, the configuration is considered closed.
	 */
	private final AtomicBoolean isClosed;
	
	/**
	 * Determines how many times the game can be restarted. Currently, there setter method is not called anywhere.
	 */
	private final AtomicInteger maxIterations;
	
	/**
	 * Tracks the number of times the game has been restarted.
	 */
	private final AtomicInteger iterationCount;
	
	/**
	 * The maximum number of turns agents are allowed in a world. -1 is unlimited, and would be a bad idea because games could
	 * possibly run forever on a server. Default is 10000;
	 */
	private final AtomicInteger maxTurns;
	
	private static final int DEFAULT_MAX_TURNS = 10000;
	private static final int DEFAULT_MAX_ITERATIONS = 5;
	
	public GridGameConfiguration(World world) {
		this.baseWorld = world;
		this.orderedAgents = Collections.synchronizedList(new ArrayList<String>());
		this.regeneratedAgents = Collections.synchronizedMap(new HashMap<String, String>());
		this.repeatedAgents = Collections.synchronizedMap(new HashMap<String, Agent>());
		this.networkAgents = Collections.synchronizedMap(new HashMap<String, GameHandler>());
		this.handlerLookup = Collections.synchronizedMap(new HashMap<String, GameHandler>());
		this.scores = Collections.synchronizedMap(new HashMap<String, Double>());
		this.isClosed = new AtomicBoolean(false);
		this.maxIterations = new AtomicInteger(DEFAULT_MAX_ITERATIONS);
		this.iterationCount = new AtomicInteger();
		this.maxTurns = new AtomicInteger(DEFAULT_MAX_TURNS);
	}
	
	/**
	 * Generates a new id for an agent added to this configuration.
	 * @return
	 */
	private String generateAgentId() {
		synchronized(this.orderedAgents) {
			if (this.orderedAgents.isEmpty()) {
				return "0";
			} else {
				return Integer.toString(Integer.parseInt(this.orderedAgents.get(this.orderedAgents.size() - 1)) + 1);
			}
		}
	}
	
	/**
	 * Returns a copy of the base world.
	 * @return
	 */
	public World getBaseWorld() {
		return this.baseWorld.copy();
	}
	
	/**
	 * Checks if any more agents can be added to the world.
	 * @return
	 */
	public boolean canAddAgent() {
		int maxAgentsCanJoin = this.baseWorld.getMaximumAgentsCanJoin();
		if (maxAgentsCanJoin == -1) {
			return true;
		}
		synchronized(this.orderedAgents) {
			return this.orderedAgents.size() < maxAgentsCanJoin;
		}
	}
	
	/** 
	 * Checks if there are any more items that can be modified for this configuration. Currently, it just checks for all agents being assigned.
	 * @return
	 */
	public boolean isFullyConfigured() {
		return !this.canAddAgent();
	}
	
	/**
	 * Returns a copy of the baseworld with all the added agents (that have been added so far).
	 * @return
	 */
	public World getWorldWithAgents() {
		World world = this.getBaseWorld();
		
		for (String agentName : this.orderedAgents) {
			this.addAgentToWorld(world, agentName);
		}
		
		return world;
	}
	

	/**
	 * Joins the actual agent object with the world. If the agent needs to be generated, then it is.
	 * @param world
	 * @param agentName
	 */
	private void addAgentToWorld(World world, String agentName) {
		if (this.repeatedAgents.containsKey(agentName)) {
			Agent agent = this.repeatedAgents.get(agentName);
			AgentType agentType = agent.getAgentType();
			agent.joinWorld(world, agentType);
			
		} else if (this.regeneratedAgents.containsKey(agentName)) {
			String agentTypeStr = this.regeneratedAgents.get(agentName);
			Agent agent = this.getNewAgentForWorld(world, agentTypeStr);
			AgentType agentType = 
					new AgentType(agentTypeStr, world.getDomain().getObjectClass(GridGame.CLASSAGENT), world.getDomain().getSingleActions());
			agent.joinWorld(world, agentType);
			
		} else if (this.networkAgents.containsKey(agentName)) {
			GameHandler handler = this.networkAgents.get(agentName);
			handler.addNetworkAgent(world);
		}
	}
	
	/**
	 * Constructs a new Agent object of the given type.
	 * @param world
	 * @param agentType
	 * @return
	 */
	private Agent getNewAgentForWorld(World world, String agentType) {
		switch(agentType) {
		case GridGameManager.RANDOM_AGENT:
			return this.getNewRandomAgent();
		case GridGameManager.COOPERATIVE_AGENT:
			return this.getNewMAVIAgent(world);
		}
		return null;
	}
	
	/**
	 * Constructs a new Random agent
	 * @return
	 */
	private Agent getNewRandomAgent() {
		return new RandomAgent();
	}
	
	/**
	 * Constructs a default multi agent value iteration agent. It's cooperative, and breaks ties randomly.
	 * @param world
	 * @return
	 */
	private Agent getNewMAVIAgent(World world) {
		
		SGDomain domain = world.getDomain();
		EGreedyMaxWellfare ja0 = new EGreedyMaxWellfare(0.0);
		ja0.setBreakTiesRandomly(false);
		JointReward rf = world.getRewardModel();
		StateHashFactory hashingFactory = new NameDependentStateHashFactory();
		MAValueIteration vi = 
				new MAValueIteration((SGDomain) domain, world.getActionModel(), rf, world.getTF(), 
						0.95, hashingFactory, 0., new MaxQ(), 0.00015, 50);
		return new MultiAgentVFPlanningAgent((SGDomain) domain, vi, new PolicyFromJointPolicy(ja0));
		
	}
	
	/**
	 * Set the max number of iterations the game will be restarted.
	 * @param iterations
	 */
	public void setMaxIterations(int iterations) {
		this.maxIterations.set(iterations);
	}
	
	/**
	 * Get the max number of iterations the game will be restarted.
	 * @return
	 */
	public int getMaxIterations() {
		return this.maxIterations.get();
	}
	
	/**
	 * Increments the current iterations.
	 * @return
	 */
	public int incrementIterations() {
		return this.iterationCount.incrementAndGet();
	}
	
	/**
	 * Checks if the iteration count has reached the maximum.
	 * @return
	 */
	public boolean hasReachedMaxIterations() {
		return (this.iterationCount.get() >= this.maxIterations.get());
	}
	
	
	/**
	 * Closes this configuration, so it can no longer be modified.
	 */
	public void close() {
		this.isClosed.set(true);
	}
	
	/**
	 * Checks whether this configuration hsa been closed.
	 * @return
	 */
	public boolean isClosed() {
		return this.isClosed.get();
	}
	
	/**
	 * Gets the total number of agents that have been configured
	 * @return
	 */
	public int getNumberAgents() {
		synchronized(this.orderedAgents) {
			return this.orderedAgents.size();
		}
	}
	
	/**
	 * Adds an agent object that will be reused with each restart. This is helpful if the agent already has a policy computed. Be
	 * surethat an agent that is added to this configuration does not share any mutable data members with other agents running in different
	 * worlds.
	 * @param agent
	 */
	public void addAgent(Agent agent) {
		if (!this.canAddAgent()) {
			return;
		}
		String agentName = this.generateAgentId();
		synchronized(this.repeatedAgents) {
			if (!this.repeatedAgents.containsValue(agent)) {
				this.repeatedAgents.put(agentName, agent);
				this.orderedAgents.add(agentName);
			}
		}
	}
	
	/**
	 * Adds a new agent of a specific type. Because each agent is regenerated at each restart, there is no concern about thread interaction with
	 * other agents.
	 * @param agentType
	 */
	public void addAgentType(String agentType) {
		if (!this.canAddAgent()) {
			return;
		}
		if (agentType.equals(GridGameManager.HUMAN_AGENT)) {
			this.addHumanVacancy();
			return;
		}
		String agentName = this.generateAgentId();
		synchronized(this.regeneratedAgents) {
			this.regeneratedAgents.put(agentName, agentType);
			this.orderedAgents.add(agentName);
		}
	}
	
	/**
	 * Get the score for a specific agent.;
	 * @param agentName
	 * @return
	 */
	public Double getScore(String agentName) {
		synchronized(this.scores) {
			return this.scores.get(agentName);
		}
	}
	
	/** 
	 * Add the updates to the current scores.
	 * @param updates
	 */
	public void appendScores(Map<String, Double> updates) {
		synchronized(this.scores) {
			for (Map.Entry<String, Double> entry : updates.entrySet()) {
				String agentName = entry.getKey();
				Double current = this.scores.get(agentName);
				
				Double update = entry.getValue();
				if (current != null) {
					update += current;
				} 
				this.scores.put(agentName, update);
					
			}
		}
	}
	
	/**
	 * Add a human agent's game handler to this configuration.
	 * @param id
	 * @param handler
	 */
	public void addHandler(String id, GameHandler handler) {
		
		String agentName = this.getNextVacancy();
		if (agentName == null) {
			if (!this.canAddAgent()) {
				return;
			}
			agentName = this.generateAgentId();
			synchronized(this.orderedAgents) {
				this.orderedAgents.add(agentName);
			}
		}
		
		synchronized(this.networkAgents) {
			this.networkAgents.put(agentName, handler);
		}
		synchronized(this.handlerLookup) {
			this.handlerLookup.put(id, handler);
		}
	}
	
	/**
	 * Checks if a human agent's slot has been added but not filled and if so returns that id
	 * @return
	 */
	private String getNextVacancy() {
		synchronized(this.orderedAgents) {
			for (int i = 0; i < this.orderedAgents.size(); i++) {
				String id = this.orderedAgents.get(i);
				if (this.isVacancy(id)) {
					return id;
				}
			}
		}
		return null;
	}
	
	/**
	 * Checks if specific id has been filled by an agent.
	 * @param id
	 * @return
	 */
	private boolean isVacancy(String id) {
		synchronized(this.regeneratedAgents) {
			if (this.regeneratedAgents.containsKey(id)) {
				return false;
			}
		}
		
		synchronized(this.repeatedAgents) {
			if (this.repeatedAgents.containsKey(id)) {
				return false;
			}
		}
		
		synchronized(this.networkAgents) {
			if (this.networkAgents.containsKey(id)) {
				return false;
			}
		}
		return true;
	}
	
	/** 
	 * Adds an empty spot in the ordered list, specifically to be filled by a human.
	 */
	public void addHumanVacancy() {
		if (!this.canAddAgent()) {
			return;
		}
		String agentName = this.generateAgentId();
		synchronized(this.orderedAgents) {
			this.orderedAgents.add(agentName);
		}
	}
	
	/**
	 * Get the game hendler with the specified client id.
	 * @param id
	 * @return
	 */
	public GameHandler getHandler(String id) {
		synchronized(this.handlerLookup) {
			return this.handlerLookup.get(id);
		}
	}
	
	/**
	 * Remove the game handler from this configuration. Not well supported.
	 * @param id
	 * @return
	 */
	public GameHandler removeHandler(String id) {
		GameHandler handler = null;
		synchronized(this.handlerLookup){ 
			handler = this.handlerLookup.remove(id);
		}
		synchronized(this.networkAgents){ 
			this.networkAgents.values().remove(handler);
		}
		return handler;
	}
	
	/**
	 * Get the agents name in the state from given handler.
	 * @param handler
	 * @return
	 */
	public String getAgentName(GameHandler handler) {
		World world = this.getWorldWithAgents();
		State startingState = world.startingState();
		
		int pos = -1;
		synchronized(this.networkAgents) {
			for (Map.Entry<String, GameHandler> entry : this.networkAgents.entrySet()) {
				if (handler == entry.getValue()) {
					pos = this.orderedAgents.indexOf(entry.getKey());
				}
			}
		}
		List<ObjectInstance> agents = startingState.getObjectsOfClass(GridGame.CLASSAGENT);
		return (pos == -1) ? null : agents.get(pos).getName();
	}

	/**
	 * Get all agent descriptions
	 * @return
	 */
	public Map<String, String> getAgentTypes() {
		Map<String, String> agentTypes = new LinkedHashMap<String, String>();
		for (String agentName : this.orderedAgents) {
			String agentType = null;
			if (this.repeatedAgents.containsKey(agentName)) {
				Agent agent = this.repeatedAgents.get(agentName);
				agentType = agent.getClass().getSimpleName();
				
			} else if (this.regeneratedAgents.containsKey(agentName)) {
				agentType = this.regeneratedAgents.get(agentName);
				
			} else {
				agentType = "human";
			}
			agentTypes.put(agentName, agentType);
		}
		return agentTypes;
	}

	/**
	 * Get the maximum number of turns agents are allowed in a game.
	 * @return
	 */
	public int getMaxTurns() {
		return this.maxTurns.get();
	}
	
	/**
	 * Sets the maximum number of turns agents are allowed in a game. -1 is unlimited, and would be a bad idea because games
	 * could possibly run forever on the server.
	 * @param maxTurns
	 */
	public void setMaxTurns(int maxTurns) {
		this.maxTurns.set(maxTurns);
	}
}
