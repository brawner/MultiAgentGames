package networking.server;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import networking.common.GridGameExtreme;
import burlap.behavior.policy.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.stochasticgames.PolicyFromJointPolicy;
import burlap.behavior.stochasticgames.agents.RandomSGAgent;
import burlap.behavior.stochasticgames.agents.madp.MultiAgentDPPlanningAgent;
import burlap.behavior.stochasticgames.agents.naiveq.SGNaiveQLAgent;
import burlap.behavior.stochasticgames.agents.normlearning.ForeverNormLearningAgent;
import burlap.behavior.stochasticgames.agents.normlearning.NormLearningAgent;
import burlap.behavior.stochasticgames.agents.normlearning.NormLearningAgentFactory;
import burlap.behavior.stochasticgames.auxiliary.jointmdp.CentralizedDomainGenerator;
import burlap.behavior.stochasticgames.auxiliary.jointmdp.TotalWelfare;
import burlap.behavior.stochasticgames.madynamicprogramming.backupOperators.MaxQ;
import burlap.behavior.stochasticgames.madynamicprogramming.dpplanners.MAValueIteration;
import burlap.behavior.stochasticgames.madynamicprogramming.policies.EGreedyMaxWellfare;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.statehashing.HashableStateFactory;
import burlap.oomdp.statehashing.SimpleHashableStateFactory;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGAgent;
import burlap.oomdp.stochasticgames.SGAgentType;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;
import examples.GridGameNormRF2;

public class AgentFactory {

	public AgentFactory() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Constructs a new Agent object of the given type.
	 * @param world
	 * @param agentType
	 * @return
	 */
	public static SGAgent getNewAgentForWorld(World world, String agentType, String params) {
		switch(agentType) {
		case GridGameManager.RANDOM_AGENT:
			return AgentFactory.getNewRandomAgent();
		case GridGameManager.COOPERATIVE_AGENT:
			return AgentFactory.getNewMAVIAgent(world);
		case GridGameManager.QLEARNER_AGENT:
			return AgentFactory.getNewQAgent(world);
		case GridGameManager.NORM_LEARNING_AGENT:
			return AgentFactory.getNormLearningAgent(world, params);
			//return NormLearningAgentFactory.getNormLearningAgent(parametersFile, experiment);
		case GridGameManager.CONTINUOUS_NORM_LEARNING:
			return AgentFactory.getContinuousNormLearningAgent(world);
		}
		return null;
	}
	
	

	/**
	 * Constructs a new Random agent
	 * @return
	 */
	public static SGAgent getNewRandomAgent() {
		return new RandomSGAgent();
	}
	
	/**
	 * Constructs a default multi agent value iteration agent. It's cooperative, and breaks ties randomly.
	 * @param world
	 * @return
	 */
	public static SGAgent getNewMAVIAgent(World world) {
		
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
	public static SGAgent getNewQAgent(World world) {
		
		SGDomain domain = world.getDomain();
		
		JointReward rf = world.getRewardModel();
		HashableStateFactory hashingFactory = new SimpleHashableStateFactory(false);
		
		SGAgent agent = new SGNaiveQLAgent(domain, .95, .9, 0.0, hashingFactory);
		
		return agent;
		
	}
	
	public static SGAgent getDefaultNormLearningAgent(World world) {
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
		agent1RF.randomizeParameters(-0.0001, 0.0001, new Random());
		//create agents
		return new NormLearningAgent(domain, agent1RF, -1, 
				agent1RF.createCorresponingDiffVInit(jplanner), true);
	}
	
	public static SGAgent getNormLearningAgent(World world, String params) {
		if (params == null) {
			return AgentFactory.getDefaultNormLearningAgent(world);
		}
		
		SGDomain domain = world.getDomain();
		List<SGAgentType> types = Arrays.asList(GridGame.getStandardGridGameAgentType(domain));
		CentralizedDomainGenerator mdpdg = new CentralizedDomainGenerator(domain, types);
		Domain cmdp = mdpdg.generateDomain();
		
		TerminalFunction tf = new GridGame.GGTerminalFunction(domain);
		JointReward jr = GridGameExtreme.getSimultaneousGoalRewardFunction(1.0, 0.0);
		
		
		
		return NormLearningAgentFactory.getNormLearningAgent(params, domain, types, jr, tf);
		
	}
	
	
	
	public static SGAgent getContinuousNormLearningAgent(World world) {
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
		return new ForeverNormLearningAgent(domain, agent1RF, -1, agent1RF.createCorresponingDiffVInit(jplanner));
		
	}

}
