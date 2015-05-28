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

public class GridGameConfiguration {

	private final World baseWorld;
	private final List<String> orderedAgents;
	private final Map<String, Agent> repeatedAgents;
	private final Map<String, String> regeneratedAgents;
	private final Map<String, GameHandler> networkAgents;
	private final Map<String, GameHandler> handlerLookup;
	private final Map<String, Double> scores;
	private final AtomicBoolean isClosed;
	private final AtomicInteger maxIterations;
	private final AtomicInteger iterationCount;
	
	public GridGameConfiguration(World world) {
		this.baseWorld = world;
		this.orderedAgents = Collections.synchronizedList(new ArrayList<String>());
		this.regeneratedAgents = Collections.synchronizedMap(new HashMap<String, String>());
		this.repeatedAgents = Collections.synchronizedMap(new HashMap<String, Agent>());
		this.networkAgents = Collections.synchronizedMap(new HashMap<String, GameHandler>());
		this.handlerLookup = Collections.synchronizedMap(new HashMap<String, GameHandler>());
		this.scores = Collections.synchronizedMap(new HashMap<String, Double>());
		this.isClosed = new AtomicBoolean(false);
		this.maxIterations = new AtomicInteger(5);
		this.iterationCount = new AtomicInteger();
	}
	
	private String generateAgentId() {
		synchronized(this.orderedAgents) {
			return Integer.toString(this.orderedAgents.size());
		}
	}
	
	public World getBaseWorld() {
		return this.baseWorld.copy();
	}
	
	public boolean canAddAgent() {
		int maxAgentsCanJoin = this.baseWorld.getMaximumAgentsCanJoin();
		if (maxAgentsCanJoin == -1) {
			return true;
		}
		synchronized(this.orderedAgents) {
			return this.orderedAgents.size() < maxAgentsCanJoin;
		}
	}
	
	public World getWorldWithAgents() {
		World world = this.getBaseWorld();
		
		for (String agentName : this.orderedAgents) {
			this.addAgentToWorld(world, agentName);
		}
		
		return world;
	}
	
	public void setMaxIterations(int iterations) {
		this.maxIterations.set(iterations);
	}
	
	public int getMaxIterations() {
		return this.maxIterations.get();
	}
	
	public int incrementIterations() {
		return this.iterationCount.incrementAndGet();
	}
	
	public boolean hasReachedMaxIterations() {
		return (this.iterationCount.get() >= this.maxIterations.get());
	}
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
	
	private Agent getNewAgentForWorld(World world, String agentType) {
		switch(agentType) {
		case GridGameServer.RANDOM_AGENT:
			return this.getNewRandomAgent();
		case GridGameServer.COOPERATIVE_AGENT:
			return this.getNewMAVIAgent(world);
		}
		return null;
	}
	
	private Agent getNewRandomAgent() {
		return new RandomAgent();
	}
	
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
	
	public void close() {
		this.isClosed.set(true);
	}
	
	public boolean isClosed() {
		return this.isClosed.get();
	}
	
	public int getNumberAgents() {
		synchronized(this.orderedAgents) {
			return this.orderedAgents.size();
		}
	}
	
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
	
	public void addAgentType(String agentType) {
		if (!this.canAddAgent()) {
			return;
		}
		if (agentType.equals(GridGameServer.HUMAN_AGENT)) {
			this.addHumanVacancy();
			return;
		}
		String agentName = this.generateAgentId();
		synchronized(this.regeneratedAgents) {
			this.regeneratedAgents.put(agentName, agentType);
			this.orderedAgents.add(agentName);
		}
	}
	
	public Double getScore(String agentName) {
		synchronized(this.scores) {
			return this.scores.get(agentName);
		}
	}
	
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
	public void addHumanVacancy() {
		if (!this.canAddAgent()) {
			return;
		}
		String agentName = this.generateAgentId();
		synchronized(this.orderedAgents) {
			this.orderedAgents.add(agentName);
		}
	}
	
	public GameHandler getHandler(String id) {
		synchronized(this.handlerLookup) {
			return this.handlerLookup.get(id);
		}
	}
	
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
}
