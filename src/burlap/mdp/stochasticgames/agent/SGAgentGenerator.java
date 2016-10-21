package burlap.mdp.stochasticgames.agent;

import burlap.mdp.stochasticgames.SGDomain;
import burlap.mdp.stochasticgames.world.World;

public interface SGAgentGenerator {
	boolean isValidAgent(World worldId, SGAgent agent);
	boolean isValidAgentType(String worldId, String agentTypeStr);
	boolean isRepeatedAgent(SGAgent agent);
	boolean isRepeatedAgentType(String agentType);
	SGAgent generateAgent(SGDomain domain, String agentName, String agentType, String[] params);
	SGAgent generateAgent(SGDomain domain, String agentName, String agentType, String paramsFile);
	
}
