package burlap.mdp.stochasticgames.agent;

import java.util.Arrays;
import java.util.List;

import burlap.behavior.stochasticgames.PolicyFromJointPolicy;
import burlap.behavior.stochasticgames.agents.RandomSGAgent;
import burlap.behavior.stochasticgames.agents.madp.MADPPlanAgentFactory;
import burlap.behavior.stochasticgames.agents.madp.MADPPlannerFactory.MAVIPlannerFactory;
import burlap.behavior.stochasticgames.agents.naiveq.SGNaiveQFactory;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.mdp.stochasticgames.SGDomain;
import burlap.mdp.stochasticgames.world.World;
import burlap.statehashing.simple.SimpleHashableStateFactory;

public class SimpleSGAgentGenerator implements SGAgentGenerator {
	public static final String RANDOM_AGENT = "random";
	public static final String NAIVE_Q = "naiveq";
	public static final List<String> ALLOWED_AGENTS = Arrays.asList(RANDOM_AGENT, NAIVE_Q);
	public SimpleSGAgentGenerator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public SGAgent generateAgent(SGDomain domain, String agentName, String agentType, String[] params) {
		switch(agentType) {
		case RANDOM_AGENT:
			return CDHallwayAgent.getAgent(domain, agentName);
			//return SimpleSGAgentGenerator.getNewRandomAgent(domain, agentName);
		case NAIVE_Q:
			return SimpleSGAgentGenerator.getNewNaiveQAgent(domain, agentName);
		}
		return null;
	}
	
	@Override
	public SGAgent generateAgent(SGDomain domain, String agentName, String agentType, String paramsFile) {
		switch(agentType) {
		case RANDOM_AGENT:
			return CDHallwayAgent.getAgent(domain, agentName);
			
			//return SimpleSGAgentGenerator.getNewRandomAgent(domain, agentName);
		case NAIVE_Q:
			return SimpleSGAgentGenerator.getNewNaiveQAgent(domain, agentName);
		
		}
		return null;
	}

	/**
	 * Constructs a new Random agent
	 * @return
	 */
	public static SGAgent getNewRandomAgent(SGDomain domain, String agentName) {
		RandomSGAgent agent = new RandomSGAgent();
		SGAgentType agentType = GridGame.getStandardGridGameAgentType(domain);
		agent.init(domain, agentName, agentType);
	
		return agent;
	}
	
	public static SGAgent getNewNaiveQAgent(SGDomain domain, String agentName) {
		SimpleHashableStateFactory hashFactory = new SimpleHashableStateFactory(false);
		SGNaiveQFactory agentFactory = new SGNaiveQFactory(domain, 0.99, 0.05, 10.0, hashFactory);
		return agentFactory.generateAgent(agentName, GridGame.getStandardGridGameAgentType(domain));
	}

//	public static SGAgent getNewMADPPlanAgent(SGDomain domain, String agentName) {
//		MAVIPlannerFactory plannerFactory = new MAVIPlannerFactory(domain, , );
//		
//		PolicyFromJointPolicy policyFromJoint = new PolicyFromJointPolicy();
//		MADPPlanAgentFactory agentFactory = new MADPPlanAgentFactory(domain, plannerFactory, policyFromJoint);
//	}
	
	@Override
	public boolean isValidAgent(World world, SGAgent agent) {
		return SimpleSGAgentGenerator.ALLOWED_AGENTS.contains(agent.agentType().typeName);
	}
	
	@Override
	public boolean isValidAgentType(String worldId, String agentType) {
		return SimpleSGAgentGenerator.ALLOWED_AGENTS.contains(agentType);
	}

	@Override
	public boolean isRepeatedAgent(SGAgent agent) {
		return false;
	}
	
	@Override
	public boolean isRepeatedAgentType(String agentTypeStr) {
		return false;
	}
}
