package burlap.mdp.stochasticgames.agent;

import java.util.Arrays;
import java.util.List;

import burlap.behavior.stochasticgames.agents.RandomSGAgent;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.mdp.stochasticgames.SGDomain;
import burlap.mdp.stochasticgames.world.World;

public class SimpleSGAgentGenerator implements SGAgentGenerator {
	public static final String RANDOM_AGENT = "random";
	
	public static final List<String> ALLOWED_AGENTS = Arrays.asList(RANDOM_AGENT);
	public SimpleSGAgentGenerator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public SGAgent generateAgent(SGDomain domain, String agentName, String agentType, String[] params) {
		switch(agentType) {
		case RANDOM_AGENT:
			return SimpleSGAgentGenerator.getNewRandomAgent(domain, agentName);
		}
		return null;
	}
	
	@Override
	public SGAgent generateAgent(SGDomain domain, String agentName, String agentType, String paramsFile) {
		switch(agentType) {
		case RANDOM_AGENT:
			return SimpleSGAgentGenerator.getNewRandomAgent(domain, agentName);
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
