//package Analysis;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import burlap.behavior.policy.Policy;
//import burlap.behavior.policy.support.ActionProb;
//import burlap.domain.stochasticgames.gridgame.GridGame;
//import burlap.mdp.core.action.Action;
//import burlap.mdp.core.oo.state.ObjectInstance;
//import burlap.mdp.core.state.State;
//import burlap.statehashing.HashableState;
//import burlap.statehashing.HashableStateFactory;
//import burlap.statehashing.simple.SimpleHashableStateFactory;
//
//public class ObservedPolicy implements Policy {
//	public enum TieBreaker{
//		MAJORITY,
//		LAST,
//		SAMPLED,
//	};
//	private Map<HashableState, List<ActionProb>> actionLookup;
//	private TieBreaker tieBreaker;
//	private HashableStateFactory hashingFactory;
//	private String abstractedAgent;
//	
//	public ObservedPolicy(String abstractedAgent) {
//		this.actionLookup = new HashMap<HashableState, List<ActionProb>>();
//		this.tieBreaker = TieBreaker.SAMPLED;
//		this.hashingFactory = new SimpleHashableStateFactory(false);
//		this.abstractedAgent = abstractedAgent;
//	}
//	
//	public static ObservedPolicy getCondensed(String abstractedAgent, List<ObservedPolicy> policies) {
//		ObservedPolicy condensed = new ObservedPolicy(abstractedAgent);
//		for (ObservedPolicy p : policies) {
//			for (Map.Entry<HashableState, List<ActionProb>> entry : p.actionLookup.entrySet()) {
//				List<ActionProb> actions = entry.getValue();
//				for (ActionProb action : actions) {
//					condensed.addStateActionObservation(entry.getKey().getSourceState(), action.ga);
//				}
//			}
//		}
//		return condensed;
//	}
//	
//	public void addStateActionObservation(State state, Action action) {
//		HashableState tuple = this.getWoAbstractedAgent(state);
//				
//		List<ActionProb> actions = actionLookup.get(tuple);
//		if (actions == null) {
//			actions = new ArrayList<ActionProb>();
//			actionLookup.put(tuple, actions);
//		}
//		actions.add(new ActionProb(action, 1.0));
//		for (ActionProb prob : actions){
//			prob.pSelection = 1.0 / actions.size();
//		}
//	}
//
//	private HashableState getWoAbstractedAgent(State state) {
//		List<ObjectInstance> agents = state.getObjectsOfClass(GridGame.CLASS_AGENT);
//		State woOtherAgents = state.copy();
//		woOtherAgents.removeObject(this.abstractedAgent);
//		return this.hashingFactory.hashState(woOtherAgents);
//	}
//
//	public List<ActionProb> getActionDistributionForState(State s) {
//		HashableState tuple = this.getWoAbstractedAgent(s);
//		
//		List<ActionProb> actions = this.actionLookup.get(tuple);
//		
//		if (actions == null) {
//			actions = new ArrayList<ActionProb>();
//		}
//		if (actions.size() > 1) {
//			System.out.print("");
//		}
//		
//		switch(this.tieBreaker) {
//		case MAJORITY:
//			Map<Action, Integer> actionCounts = new HashMap<Action, Integer>();
//			int bestCount = 0;
//			Action bestAction = null;
//			for (ActionProb action : actions) {
//				Action ga = action.ga;
//				Integer count = actionCounts.get(ga);
//				if (count == null) {
//					count = 0;
//				}
//				count++;
//				if (count > bestCount) {
//					bestAction = ga;
//					bestCount = count;
//				}
//				actionCounts.put(ga, count);
//			}
//			actions = Arrays.asList(new ActionProb(bestAction, 1.0));
//			break;
//		case LAST:
//			return actions.subList(actions.size() - 1 , actions.size());
//		default:
//			break;
//		}
//		
//		return actions;
//	}
//
//	public boolean isStochastic() {
//		return true;
//	}
//
//	public void setTieBreaker(TieBreaker tieBreaker) {
//		this.tieBreaker = tieBreaker;
//	}
//
//	public TieBreaker getTieBreaker() {
//		return this.tieBreaker;
//	}
//
//	@Override
//	public Action action(State s) {
//HashableState tuple = this.getWoAbstractedAgent(s);
//		
//		List<ActionProb> actions = this.actionLookup.get(tuple);
//		if (actions == null) {
//			return null;
//		}
//		switch(this.tieBreaker) {
//		case MAJORITY:
//			Map<Action, Integer> actionCounts = new HashMap<Action, Integer>();
//			int bestCount = 0;
//			Action bestAction = null;
//			for (ActionProb action : actions) {
//				Action ga = action.ga;
//				Integer count = actionCounts.get(ga);
//				if (count == null) {
//					count = 0;
//				}
//				count++;
//				if (count > bestCount) {
//					bestAction = ga;
//					bestCount = count;
//				}
//				actionCounts.put(ga, count);
//			}
//			return bestAction;
//		case LAST:
//			return actions.get(actions.size() - 1).ga;
//		case SAMPLED:
//			return this.sampleFromActionDistribution(s);
//		}
//	
//		return null;
//	}
//
//	@Override
//	public double actionProb(State s, Action a) {
//
//	}
//
//	@Override
//	public boolean definedFor(State s) {
//		return this.actionLookup.containsKey(s);
//	}
//
//}
