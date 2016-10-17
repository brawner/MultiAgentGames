package networking.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import networking.common.GridGameExperimentToken;
import networking.common.Token;
import networking.common.TokenCastException;
import burlap.domain.stochasticdomain.world.NetworkWorld;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.mdp.core.oo.state.OOState;
import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.agent.SGAgentGenerator;

public class MatchConfiguration {
	private static final String AGENTS = "agents";
	private static final String PARAMS = "params";
	private static final int DEFAULT_MAX_TURNS = 30;
	private static final int DEFAULT_MAX_ROUNDS = 20;
	
	/**
	 * The base type of world for this configuration.
	 */
	private NetworkWorld baseWorld;
	
	/**
	 * A list of the agents in their assigned order. I.e. the first agent added will be agent 0, regardless of what type it is.
	 */
	private final List<String> orderedAgents;
	
	/**
	 * If an Agent needs to precompute a policy and should be recycled from each run, then it should be a repeated agent. Becareful to make sure
	 * that any repeated agent is not shared with a different game, and all data members are fully copied when added to this configuration.
	 */
	private final Map<String, SGAgent> repeatedAgents;
	
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
	 * Once all the agents have been assigned, the configuration is considered closed.
	 */
	private final AtomicBoolean isClosed;
	
	/**
	 * Determines how many times the game can be restarted. Currently, the setter method is not called anywhere.
	 */
	private final AtomicInteger numTotalRounds;
	
	/**
	 * Tracks the number of times the game has been restarted.
	 */
	private final AtomicInteger currentRound;
	
	/**
	 * The maximum number of turns agents are allowed in a world. -1 is unlimited, and would be a bad idea because games could
	 * possibly run forever on a server. Default is 30;
	 */
	private final AtomicInteger maxTurns;
	
	private final SGAgentGenerator agentGenerator;

	public MatchConfiguration(NetworkWorld world, SGAgentGenerator agentGenerator) {
		this.baseWorld = world;
		this.orderedAgents = Collections.synchronizedList(new ArrayList<String>());
		this.regeneratedAgents = Collections.synchronizedMap(new HashMap<String, String>());
		this.repeatedAgents = Collections.synchronizedMap(new HashMap<String, SGAgent>());
		this.networkAgents = Collections.synchronizedMap(new HashMap<String, GameHandler>());
		this.handlerLookup = Collections.synchronizedMap(new HashMap<String, GameHandler>());
		this.isClosed = new AtomicBoolean(false);
		this.numTotalRounds = new AtomicInteger(DEFAULT_MAX_ROUNDS);
		this.currentRound = new AtomicInteger();
		this.maxTurns = new AtomicInteger(DEFAULT_MAX_TURNS);
		this.agentGenerator = agentGenerator;
	}
	public MatchConfiguration(NetworkWorld world, SGAgentGenerator agentGenerator, int maxTurns, int maxRounds) {
		this.baseWorld = world;
		this.orderedAgents = Collections.synchronizedList(new ArrayList<String>());
		this.regeneratedAgents = Collections.synchronizedMap(new HashMap<String, String>());
		this.repeatedAgents = Collections.synchronizedMap(new HashMap<String, SGAgent>());
		this.networkAgents = Collections.synchronizedMap(new HashMap<String, GameHandler>());
		this.handlerLookup = Collections.synchronizedMap(new HashMap<String, GameHandler>());
		this.isClosed = new AtomicBoolean(false);
		this.numTotalRounds = new AtomicInteger(maxRounds);
		this.currentRound = new AtomicInteger();
		this.maxTurns = new AtomicInteger(maxTurns);
		this.agentGenerator = agentGenerator;
	}


	public static MatchConfiguration getConfigurationFromToken(GridGameExperimentToken token, GridGameServerCollections collections, SGAgentGenerator agentGenerator, String paramsDirectory) {
		try {
			String worldId = token.getString(GridGameManager.WORLD_ID);
			if (worldId == null) {
				System.err.println("World id " + worldId + " was not specified");
				return null;
			}
			NetworkWorld world = collections.getWorld(worldId);
			if (world == null) {
				System.err.println("World " + worldId + " does not exist");
				return null;
			}
		
			Integer maxTurns = token.getInt(GridGameExperimentToken.MAX_TURNS);
			if (maxTurns == null) {
				maxTurns = DEFAULT_MAX_TURNS;
			}
			
			Integer maxRounds = token.getInt(GridGameExperimentToken.MAX_ROUNDS);
			if (maxRounds == null) {
				maxRounds = DEFAULT_MAX_ROUNDS; 
			}
			
			MatchConfiguration configuration = new MatchConfiguration(world, agentGenerator, maxTurns, maxRounds);
			List<GridGameExperimentToken> agentTokens = token.getTokenList(AGENTS);
			if (agentTokens == null) {
				System.err.println("Agent tokens were not specified");
				return null;
			}
			for (Token agentToken : agentTokens) {
				String agentTypeStr = agentToken.getString(GridGameManager.AGENT_TYPE);
				if (agentTypeStr == null) {
					System.err.println("Agent's type was not specified");
					return null;
				}
				String params = agentToken.getString(PARAMS);
				if (params != null) {
					Path path = Paths.get(paramsDirectory, params + ".properties");
					if (!Files.exists(path)) {
						System.err.println(path.toString() + " does not exist");
						return null;
					}
					params = Paths.get(paramsDirectory, params).toString();
				}
				configuration.addAgentType(agentTypeStr,  agentGenerator.isRepeatedAgentType(agentTypeStr), params);
			}
			return configuration;
		
		} catch (TokenCastException e) {
			e.printStackTrace();
			return null;
		}
			
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
	public NetworkWorld getBaseWorld() {
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
	 * Checks if there are any more items that can be modified for this configuration. 
	 * Currently, it just checks for all agents being assigned.
	 * @return
	 */
	public boolean isFullyConfigured() {
		if(this.getNextVacancy()==null){
		
			return !this.canAddAgent();
		}else{
			return false;
		}
	}
	
	/**
	 * Returns a copy of the baseworld with all the added agents (that have been added so far).
	 * @return
	 */
	public NetworkWorld getWorldWithAgents() {
		NetworkWorld world = this.getBaseWorld().copy();
		
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
	private void addAgentToWorld(NetworkWorld world, String agentName) {
		if (this.repeatedAgents.containsKey(agentName)) {
			SGAgent agent = this.repeatedAgents.get(agentName);
			//SGAgentType agentType = agent.getAgentType();
//			SGAgentType agentType = 
//					new SGAgentType(agent.getClass().getSimpleName(), world.getDomain().getActionTypes());
			if (!this.agentGenerator.isValidAgent(world, agent)) {
				world.join(agent);
			}
		} else if (this.regeneratedAgents.containsKey(agentName)) {
			String agentTypeStr = this.regeneratedAgents.get(agentName);
			SGAgent agent = this.agentGenerator.generateAgent(agentTypeStr, "");
//			SGAgentType agentType = 
//					new SGAgentType(agentTypeStr, world.getDomain().getActionTypes());
			if (!this.agentGenerator.isValidAgent(world, agent)) {
				world.join(agent);
			}
			
		} else if (this.networkAgents.containsKey(agentName)) {
			GameHandler handler = this.networkAgents.get(agentName);
			handler.addNetworkAgent(world);
		}
	}
	
	/**
	 * Set the max number of iterations the game will be restarted.
	 * @param iterations
	 */
	public void setMaxIterations(int iterations) {
		this.numTotalRounds.set(iterations);
	}
	
	/**
	 * Get the max number of iterations the game will be restarted.
	 * @return
	 */
	public int getMaxIterations() {
		return this.numTotalRounds.get();
	}
	
	/**
	 * Increments the current iterations.
	 * @return
	 */
	public int incrementIterations() {
		return this.currentRound.incrementAndGet();
	}
	
	/**
	 * Checks if the iteration count has reached the maximum.
	 * @return
	 */
	public boolean hasReachedMaxIterations() {
		return (this.currentRound.get() >= this.numTotalRounds.get());
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
	
	public List<SGAgent> getRepeatedAgents() {
		synchronized(this.repeatedAgents) {
			return new ArrayList<SGAgent>(this.repeatedAgents.values());
		}
	}
	
	/**
	 * Adds an agent object that will be reused with each restart. This is helpful if the agent already has a policy computed. Be
	 * sure that an agent that is added to this configuration does not share any mutable data members with other agents running in different
	 * worlds.
	 * @param agent
	 */
	public void addAgent(SGAgent agent) {
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
	
	public void addAgentType(String agentType, boolean repeated, String params) {
		if (repeated) {
			SGAgent agent = this.agentGenerator.generateAgent(agentType, params);
			this.addAgent(agent);
		}

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
	 * Adds a new agent of a specific type. Because each agent is regenerated at each restart, there is no concern about thread interaction with
	 * other agents.
	 * @param agentType
	 */
	public void addAgentType(String agentType) {
		this.addAgentType(agentType, false, null);
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
	 * Get the game handler with the specified client id.
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
		NetworkWorld world = this.getWorldWithAgents();
		OOState startingState = (OOState) world.startingState();
		
		int pos = -1;
		synchronized(this.networkAgents) {
			for (Map.Entry<String, GameHandler> entry : this.networkAgents.entrySet()) {
				if (handler == entry.getValue()) {
					pos = this.orderedAgents.indexOf(entry.getKey());
				}
			}
		}
		List<ObjectInstance> agents = startingState.objectsOfClass(GridGame.CLASS_AGENT);
		return (pos == -1) ? null : agents.get(pos).name();
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
				SGAgent agent = this.repeatedAgents.get(agentName);
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
	
	public int getRoundNumber() {
		
		return currentRound.get();
	}

	public Map<String, GameHandler> getHandlerLookup() {
		
		return handlerLookup;
	}
}
