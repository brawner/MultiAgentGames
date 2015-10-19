package Analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Analysis.ObservedPolicy.TieBreaker;
import burlap.behavior.policy.Policy;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.objects.ObjectInstance;
import burlap.oomdp.core.states.State;
import burlap.oomdp.statehashing.HashableState;
import burlap.oomdp.statehashing.HashableStateFactory;
import burlap.oomdp.statehashing.SimpleHashableStateFactory;

public class ObservedPolicy extends Policy {
	public enum TieBreaker{
		MAJORITY,
		LAST,
		SAMPLED,
	};
	private Map<HashableState, List<ActionProb>> actionLookup;
	private TieBreaker tieBreaker;
	private HashableStateFactory hashingFactory;
	private String abstractedAgent;
	
	public ObservedPolicy(String abstractedAgent) {
		this.actionLookup = new HashMap<HashableState, List<ActionProb>>();
		this.tieBreaker = TieBreaker.SAMPLED;
		this.hashingFactory = new SimpleHashableStateFactory(false);
		this.abstractedAgent = abstractedAgent;
	}
	
	public static ObservedPolicy getCondensed(String abstractedAgent, List<ObservedPolicy> policies) {
		ObservedPolicy condensed = new ObservedPolicy(abstractedAgent);
		for (ObservedPolicy p : policies) {
			for (Map.Entry<HashableState, List<ActionProb>> entry : p.actionLookup.entrySet()) {
				List<ActionProb> actions = entry.getValue();
				for (ActionProb action : actions) {
					condensed.addStateActionObservation(entry.getKey().getSourceState(), action.ga);
				}
			}
		}
		return condensed;
	}
	
	public void addStateActionObservation(State state, AbstractGroundedAction action) {
		HashableState tuple = this.getWoAbstractedAgent(state);
				
		List<ActionProb> actions = actionLookup.get(tuple);
		if (actions == null) {
			actions = new ArrayList<ActionProb>();
			actionLookup.put(tuple, actions);
		}
		actions.add(new ActionProb(action, 1.0));
		for (ActionProb prob : actions){
			prob.pSelection = 1.0 / actions.size();
		}
	}

	private HashableState getWoAbstractedAgent(State state) {
		List<ObjectInstance> agents = state.getObjectsOfClass(GridGame.CLASSAGENT);
		State woOtherAgents = state.copy();
		woOtherAgents.removeObject(this.abstractedAgent);
		return this.hashingFactory.hashState(woOtherAgents);
	}
	
	@Override
	public AbstractGroundedAction getAction(State s) {
		HashableState tuple = this.getWoAbstractedAgent(s);
		
		List<ActionProb> actions = this.actionLookup.get(tuple);
		if (actions == null) {
			return null;
		}
		switch(this.tieBreaker) {
		case MAJORITY:
			Map<AbstractGroundedAction, Integer> actionCounts = new HashMap<AbstractGroundedAction, Integer>();
			int bestCount = 0;
			AbstractGroundedAction bestAction = null;
			for (ActionProb action : actions) {
				AbstractGroundedAction ga = action.ga;
				Integer count = actionCounts.get(ga);
				if (count == null) {
					count = 0;
				}
				count++;
				if (count > bestCount) {
					bestAction = ga;
					bestCount = count;
				}
				actionCounts.put(ga, count);
			}
			return bestAction;
		case LAST:
			return actions.get(actions.size() - 1).ga;
		case SAMPLED:
			return this.sampleFromActionDistribution(s);
		}
	
		return null;
	}

	@Override
	public List<ActionProb> getActionDistributionForState(State s) {
		HashableState tuple = this.getWoAbstractedAgent(s);
		
		List<ActionProb> actions = this.actionLookup.get(tuple);
		
		if (actions == null) {
			actions = new ArrayList<ActionProb>();
		}
		if (actions.size() > 1) {
			System.out.print("");
		}
		
		switch(this.tieBreaker) {
		case MAJORITY:
			Map<AbstractGroundedAction, Integer> actionCounts = new HashMap<AbstractGroundedAction, Integer>();
			int bestCount = 0;
			AbstractGroundedAction bestAction = null;
			for (ActionProb action : actions) {
				AbstractGroundedAction ga = action.ga;
				Integer count = actionCounts.get(ga);
				if (count == null) {
					count = 0;
				}
				count++;
				if (count > bestCount) {
					bestAction = ga;
					bestCount = count;
				}
				actionCounts.put(ga, count);
			}
			actions = Arrays.asList(new ActionProb(bestAction, 1.0));
			break;
		case LAST:
			return actions.subList(actions.size() - 1 , actions.size());
		default:
			break;
		}
		
		return actions;
	}

	@Override
	public boolean isStochastic() {
		return true;
	}

	@Override
	public boolean isDefinedFor(State s) {
		return this.actionLookup.containsKey(s);
	}

	public void setTieBreaker(TieBreaker tieBreaker) {
		this.tieBreaker = tieBreaker;
	}

	public TieBreaker getTieBreaker() {
		return this.tieBreaker;
	}

}
