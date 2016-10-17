package burlap.mdp.stochasticgames.agent;

import java.util.Arrays;
import java.util.List;

import burlap.behavior.stochasticgames.agents.RandomSGAgent;
import burlap.domain.stochasticdomain.world.NetworkWorld;
import burlap.mdp.stochasticgames.world.World;

public class SimpleSGAgentGenerator implements SGAgentGenerator {
	public static final String RANDOM_AGENT = "random";
	
	public static final List<String> ALLOWED_AGENTS = Arrays.asList(RANDOM_AGENT);
	public SimpleSGAgentGenerator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public SGAgent generateAgent(String agentType, String[] params) {
		switch(agentType) {
		case RANDOM_AGENT:
			return SimpleSGAgentGenerator.getNewRandomAgent();
		}
		return null;
	}
	
	@Override
	public SGAgent generateAgent(String agentType, String paramsFile) {
		switch(agentType) {
		case RANDOM_AGENT:
			return SimpleSGAgentGenerator.getNewRandomAgent();
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
