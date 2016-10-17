package burlap.oomdp.stochasticgames.Callables;

import java.util.List;

import burlap.domain.stochasticdomain.world.NetworkWorld;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.parallel.Parallel.ForCallable;

public class GameStartingCallable extends ForCallable<Boolean> {
	// call to agents need to be threaded, and timed out
	private final NetworkWorld world;
	private final List<SGAgent> agents;
	public GameStartingCallable(NetworkWorld world, List<SGAgent> agents){
		this.world = world;
		this.agents = agents;
	}		
	
	@Override
	public ForCallable<Boolean> copy() {
		return new GameStartingCallable(this.world, this.agents);
	}

	@Override
	public Boolean perform(int start, int current, int end, int increment) {
		this.agents.get(current).gameStarting(this.world, current);
		return true;
	}
				
}
