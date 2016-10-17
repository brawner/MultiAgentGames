//package Analysis;
//
//import java.awt.event.KeyEvent;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import burlap.behavior.singleagent.auxiliary.valuefunctionvis.PolicyRenderLayer;
//import burlap.mdp.core.state.State;
//import burlap.mdp.stochasticgames.SGDomain;
//import burlap.shell.visual.SGVisualExplorer;
//import burlap.statehashing.HashableState;
//import burlap.statehashing.HashableStateFactory;
//import burlap.statehashing.simple.SimpleHashableStateFactory;
//import burlap.visualizer.Visualizer;
//
//public class SGPolicyExplorer extends SGVisualExplorer {
//	/**
//	 * 
//	 */
//	private static final long serialVersionUID = 7939892572103377831L;
//	private String abstractedAgent;
//	private PolicyRenderLayer pLayer;
//	private List<Map<HashableState, Set<HashableState>>> abstractedStates;
//	private Map<HashableState, Set<HashableState>> condensedStates;
//	private ObservedPolicy condensedPolicy;
//	private HashableStateFactory hashingFactory;
//	private int currentPolicyIndex;
//	private ObservedPolicy currentPolicy;
//	
//	private Map<HashableState, Set<HashableState>> currentStateLookup;
//	private List<ObservedPolicy> policies;
//	
//	public SGPolicyExplorer(SGDomain domain, Visualizer painter, State baseState, List<List<State>> allStates, PolicyRenderLayer pLayer, List<ObservedPolicy> policies, String abstractedAgent) {
//		super(domain, painter, baseState);
//		this.abstractedAgent = abstractedAgent;
//		this.pLayer = pLayer;
//		this.policies = policies;
//		
//		this.hashingFactory = new SimpleHashableStateFactory(false);
//		this.abstractedStates = new ArrayList<Map<HashableState, Set<HashableState>>>();
//		this.condensedStates = new HashMap<HashableState, Set<HashableState>>();
//		
//		for (List<State> states : allStates) {
//			Map<HashableState, Set<HashableState>> map = new HashMap<HashableState, Set<HashableState>>();
//			this.abstractedStates.add(map);
//			
//			for (State state : states) {
//				HashableState hashed = this.hashingFactory.hashState(state);
//				HashableState abstracted = this.getWoAbstractedAgent(state);
//				Set<HashableState> list = map.get(abstracted);
//				if (list == null) {
//					list = new HashSet<HashableState>();
//					map.put(abstracted, list);
//				}
//				list.add(hashed);
//				
//				list = this.condensedStates.get(abstracted);
//				if (list == null) {
//					list = new HashSet<HashableState>();
//					this.condensedStates.put(abstracted, list);
//				}
//				list.add(hashed);
//			}
//		}
//		this.condensedPolicy = ObservedPolicy.getCondensed(abstractedAgent, policies);
//		this.currentPolicyIndex = 0;
//		this.currentPolicy = this.policies.get(this.currentPolicyIndex);
//		this.currentStateLookup = this.abstractedStates.get(this.currentPolicyIndex);
//	}
//	
//	private HashableState getWoAbstractedAgent(State state) {
//		State woOtherAgents = state.copy();
//		woOtherAgents.removeObject(this.abstractedAgent);
//		return this.hashingFactory.hashState(woOtherAgents);
//	}
//	
//	@Override
//	protected void handleKeyPressed(KeyEvent e){
//		char keyChar = e.getKeyChar();
//		int num = Character.getNumericValue(keyChar);
//		System.out.println("Code: " + num + " Char: " + keyChar);
//		ObservedPolicy prev = this.policies.get(this.currentPolicyIndex);
//		
//		if (num > 0 && num < 9) {
//			this.updatePolicy(num - 1);
//		} else if (num == 0) {
//			this.currentPolicy = this.condensedPolicy;
//			this.currentStateLookup = this.condensedStates;
//		} else if (num == 9) {
//			this.currentPolicy = this.policies.get(this.currentPolicyIndex);
//			this.currentStateLookup = this.abstractedStates.get(this.currentPolicyIndex);
//			
//		}
//		if (keyChar == '-' && this.currentPolicyIndex > 0) {
//			this.currentPolicyIndex--;
//		} else if (keyChar == '=' && this.currentPolicyIndex < this.policies.size()-1) {
//			this.currentPolicyIndex++;
//		}
//		
//		if (keyChar == '-' || keyChar == '=') {
//			this.currentPolicy = this.policies.get(currentPolicyIndex);
//			this.currentStateLookup = this.abstractedStates.get(this.currentPolicyIndex);
//		}
//		System.out.println("Current policy: " + this.currentPolicy);
//		this.currentPolicy.setTieBreaker(prev.getTieBreaker());
//		this.pLayer.setPolicy(this.currentPolicy);
//		
//		super.handleKeyPressed(e);
//		this.executeAction();
//	}
//	
//	private void updatePolicy(int value) {
//		ObservedPolicy.TieBreaker[] values = ObservedPolicy.TieBreaker.values();
//		if (value >= 0 && value < values.length) {
//			ObservedPolicy.TieBreaker tb = values[value];
//			System.out.println("Tie breaker: " + tb.toString());
//			this.currentPolicy.setTieBreaker(tb);
//		}
//	}
//	
//	@Override
//	public void updateState(State s){
//		HashableState abstracted = this.getWoAbstractedAgent(s);
//		super.updateState(abstracted);
//		Set<HashableState> states = this.currentStateLookup.get(abstracted);
//		if (states == null) {
//			states = new HashSet<HashableState>();
//		}
//		Set<State> hashed = new HashSet<State>(states);
//		this.pLayer.setStatesToVisualize(hashed);
//	}
//
//}
