package networking.server;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import networking.common.GridGameExtreme;
import examples.GridGameNormRF2;
import burlap.behavior.policy.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.stochasticgames.PolicyFromJointPolicy;
import burlap.behavior.stochasticgames.agents.RandomSGAgent;
import burlap.behavior.stochasticgames.agents.madp.MultiAgentDPPlanningAgent;
import burlap.behavior.stochasticgames.agents.naiveq.SGNaiveQLAgent;
import burlap.behavior.stochasticgames.agents.normlearning.NormLearningAgent;
import burlap.behavior.stochasticgames.auxiliary.jointmdp.CentralizedDomainGenerator;
import burlap.behavior.stochasticgames.auxiliary.jointmdp.TotalWelfare;
import burlap.behavior.stochasticgames.madynamicprogramming.backupOperators.MaxQ;
import burlap.behavior.stochasticgames.madynamicprogramming.dpplanners.MAValueIteration;
import burlap.behavior.stochasticgames.madynamicprogramming.policies.EGreedyMaxWellfare;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.objects.ObjectInstance;
import burlap.oomdp.core.states.State;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.statehashing.HashableStateFactory;
import burlap.oomdp.statehashing.SimpleHashableStateFactory;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGAgent;
import burlap.oomdp.stochasticgames.SGAgentType;
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
	private final String uniqueGameID;

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
	 * The game scores that are tracked through an games run. This should be modified at the end of each run with how reward maps to overall score
	 */
	private final Map<String, Double> scores;
	
	/**
	 * Once all the agents have been assigned, the configuration is considered closed.
	 */
	private final AtomicBoolean isClosed;
	
	/**
	 * Determines how many times the game can be restarted. Currently, the setter method is not called anywhere.
	 */
	private final AtomicInteger maxIterations;
	
	/**
	 * Tracks the number of times the game has been restarted.
	 */
	private final AtomicInteger iterationCount;
	
	/**
	 * The maximum number of turns agents are allowed in a world. -1 is unlimited, and would be a bad idea because games could
	 * possibly run forever on a server. Default is 30;
	 */
	private final AtomicInteger maxTurns;
	
	private static final int DEFAULT_MAX_TURNS = 30;
	private static final int DEFAULT_MAX_ITERATIONS = 20;
	
	public GridGameConfiguration(World world) {
		this.baseWorld = world;
		this.uniqueGameID = generateUniqueID();
		this.orderedAgents = Collections.synchronizedList(new ArrayList<String>());
		this.regeneratedAgents = Collections.synchronizedMap(new HashMap<String, String>());
		this.repeatedAgents = Collections.synchronizedMap(new HashMap<String, SGAgent>());
		this.networkAgents = Collections.synchronizedMap(new HashMap<String, GameHandler>());
		this.handlerLookup = Collections.synchronizedMap(new HashMap<String, GameHandler>());
		this.scores = Collections.synchronizedMap(new HashMap<String, Double>());
		this.isClosed = new AtomicBoolean(false);
		this.maxIterations = new AtomicInteger(DEFAULT_MAX_ITERATIONS);
		this.iterationCount = new AtomicInteger();
		this.maxTurns = new AtomicInteger(DEFAULT_MAX_TURNS);
	}
	
	private String generateUniqueID() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SS");
		Date date = new Date();
		String dateStr = dateFormat.format(date);
		Random rand = new Random();
		String randVal = Integer.toString(rand.nextInt(Integer.MAX_VALUE));
		
		return dateStr+"_"+randVal;
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
			SGAgent agent = this.repeatedAgents.get(agentName);
			//SGAgentType agentType = agent.getAgentType();
			SGAgentType agentType = 
					new SGAgentType(agent.getClass().getSimpleName(), world.getDomain().getObjectClass(GridGame.CLASSAGENT), world.getDomain().getAgentActions());
			
			agent.joinWorld(world, agentType);
			
		} else if (this.regeneratedAgents.containsKey(agentName)) {
			String agentTypeStr = this.regeneratedAgents.get(agentName);
			SGAgent agent = this.getNewAgentForWorld(world, agentTypeStr);
			SGAgentType agentType = 
					new SGAgentType(agentTypeStr, world.getDomain().getObjectClass(GridGame.CLASSAGENT), world.getDomain().getAgentActions());
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
	private SGAgent getNewAgentForWorld(World world, String agentType) {
		switch(agentType) {
		case GridGameManager.RANDOM_AGENT:
			return this.getNewRandomAgent();
		case GridGameManager.COOPERATIVE_AGENT:
			return this.getNewMAVIAgent(world);
		case GridGameManager.QLEARNER_AGENT:
			return this.getNewQAgent(world);
		case GridGameManager.NORM_LEARNING_AGENT:
			return this.getNormLearningAgent(world);
		}
		return null;
	}
	
	/**
	 * Constructs a new Random agent
	 * @return
	 */
	private SGAgent getNewRandomAgent() {
		return new RandomSGAgent();
	}
	
	/**
	 * Constructs a default multi agent value iteration agent. It's cooperative, and breaks ties randomly.
	 * @param world
	 * @return
	 */
	private SGAgent getNewMAVIAgent(World world) {
		
		SGDomain domain = world.getDomain();
		EGreedyMaxWellfare ja0 = new EGreedyMaxWellfare(0.0);
		ja0.setBreakTiesRandomly(false);
		JointReward rf = world.getRewardModel();
		HashableStateFactory hashingFactory = new SimpleHashableStateFactory(false);
		MAValueIteration vi = 
				new MAValueIteration((SGDomain) domain, rf, world.getTF(), 
						0.95, hashingFactory, 0., new MaxQ(), 0.00015, 50);
		return new MultiAgentDPPlanningAgent((SGDomain) domain, vi, new PolicyFromJointPolicy(ja0));
		
	}
	
	/**
	 * Constructs a default multi agent value iteration agent. It's cooperative, and breaks ties randomly.
	 * @param world
	 * @return
	 */
	private SGAgent getNewQAgent(World world) {
		
		SGDomain domain = world.getDomain();
		
		JointReward rf = world.getRewardModel();
		HashableStateFactory hashingFactory = new SimpleHashableStateFactory(false);
		
		SGAgent agent = new SGNaiveQLAgent(domain, .95, .9, 0.0, hashingFactory);
		
		return agent;
		
	}
	
	private SGAgent getNormLearningAgent(World world) {
		SGDomain domain = world.getDomain();
		List<SGAgentType> types = Arrays.asList(GridGame.getStandardGridGameAgentType(domain));
		CentralizedDomainGenerator mdpdg = new CentralizedDomainGenerator(domain, types);
		Domain cmdp = mdpdg.generateDomain();
		
		TerminalFunction tf = new GridGame.GGTerminalFunction(domain);
		JointReward jr = GridGameExtreme.getSimultaneousGoalRewardFunction(1.0, 0.0);
		
		RewardFunction crf = new TotalWelfare(jr);

		//create joint task planner for social reward function and RHIRL leaf node values
		final SparseSampling jplanner = new SparseSampling(cmdp, crf, tf, 0.99, new SimpleHashableStateFactory(false), 20, -1);
		jplanner.toggleDebugPrinting(false);


		//create independent social reward functions to learn for each agent
		final GridGameNormRF2 agent1RF = new GridGameNormRF2(crf, new GreedyQPolicy(jplanner), domain);

		//create agents
		return new NormLearningAgent(domain, agent1RF, -1, agent1RF.createCorresponingDiffVInit(jplanner));
		
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
	
	public void addAgentType(String agentType, boolean repeated) {
		if (repeated) {
			SGAgent agent = getNewAgentForWorld(this.baseWorld, agentType);
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
		this.addAgentType(agentType, false);
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

	public String getUniqueGameId() {
		
		return uniqueGameID;
	}

	public AtomicInteger getGameNum() {
		
		return iterationCount;
	}

	public Map<String, GameHandler> getHandlerLookup() {
		
		return handlerLookup;
	}
	

}
