package burlap.oomdp.stochasticgames.Callables;

import java.util.ArrayList;
import java.util.List;


import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.parallel.Parallel.ForEachCallable;

public class GetActionCallable extends ForEachCallable<List<SGAgent>, List<Action>> {
	
	private final State abstractedCurrent;
	public GetActionCallable(State abstractedCurrent) {
		this.abstractedCurrent = abstractedCurrent;
	}
	
	@Override
	public List<Action> perform(List<SGAgent> current) {
		List<Action> actions = new ArrayList<Action>();
		for (SGAgent agent : current) {
			actions.add(agent.action(abstractedCurrent));
		}
		return actions;
	}

	@Override
	public ForEachCallable<List<SGAgent>, List<Action>> copy() {
		return new GetActionCallable(abstractedCurrent);
	}
}
