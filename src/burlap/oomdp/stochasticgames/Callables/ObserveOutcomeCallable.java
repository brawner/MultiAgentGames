package burlap.oomdp.stochasticgames.Callables;

import burlap.mdp.auxiliary.StateMapping;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.parallel.Parallel.ForEachCallable;

public class ObserveOutcomeCallable extends ForEachCallable<SGAgent, Boolean> {
	
	private final JointAction ja;
	private final double[] jointReward;
	private final State currentState;
	private final State nextState;
	private final Boolean isTerminal;
	private final StateMapping abstraction;
	
	public ObserveOutcomeCallable(State currentState, JointAction ja, double[] jointReward, 
			State nextState, StateMapping abstraction, Boolean isTerminal) {
		this.currentState = currentState;
		this.nextState = nextState;
		this.ja = ja;
		this.jointReward = jointReward;
		this.isTerminal = isTerminal;
		this.abstraction = abstraction;
	}

	@Override
	public ObserveOutcomeCallable copy() {
		return new ObserveOutcomeCallable(currentState, ja, jointReward, nextState, abstraction, isTerminal);
	}
	
	@Override
	public Boolean perform(SGAgent agent) {
		State abstractedCurrent = abstraction.mapState(currentState);
		State abstractedNext = abstraction.mapState(nextState);
		agent.observeOutcome(abstractedCurrent, ja, jointReward, abstractedNext, isTerminal);
		return true;
	}
}
