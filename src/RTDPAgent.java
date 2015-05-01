import java.util.Map;

import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;


public class RTDPAgent extends Agent {
	public RTDPAgent() {
		super();
		
	}

	@Override
	public void gameStarting() {
		// TODO Auto-generated method stub

	}

	@Override
	public GroundedSingleAction getAction(State s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void observeOutcome(State s, JointAction jointAction,
			Map<String, Double> jointReward, State sprime, boolean isTerminal) {
		// TODO Auto-generated method stub

	}

	@Override
	public void gameTerminated() {
		// TODO Auto-generated method stub

	}

}
