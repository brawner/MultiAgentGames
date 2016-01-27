package Analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import burlap.behavior.policy.Policy;
import burlap.behavior.policy.Policy.ActionProb;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.states.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.statehashing.HashableState;
import burlap.oomdp.statehashing.HashableStateFactory;
import burlap.oomdp.statehashing.SimpleHashableStateFactory;


/** 
 * This metric measures the KL divergence between two learned policies. Some of the action probabilities in the learned 
 * policy might be zero, which is not ideal for KL divergence. Hence we use a simple prior of 10^-6 as base probabilities
 * of each action.
 * @author ngopalan
 */

public class PolicyComparisonWithKLDivergence {
	
	private Policy truePolicy;
	private Policy learnedPolicy;
	private State startState;
	private HashableStateFactory hsf = new SimpleHashableStateFactory();
	private double alpha = Math.pow(10, -6);
	
	public PolicyComparisonWithKLDivergence(Policy truePolicy, Policy learnedPolicy, State startState){
	
		this.truePolicy = truePolicy;
		this.learnedPolicy = learnedPolicy;
		this.startState = startState ;
		
	}
	
	public double runPolicyComparison(){
		double klDistance =0.;
		
		StateReachability sr = new StateReachability();
		Set<HashableState> stateSet = sr.getPolicyReachableHashedStates(truePolicy, startState, hsf);
		stateSet.addAll(sr.getPolicyReachableHashedStates(learnedPolicy, startState, hsf));
		
		
		
		for(HashableState hs : stateSet ){
			List<ActionProb> trueAP = truePolicy.getActionDistributionForState(hs.s);
			List<ActionProb> falseAP = learnedPolicy.getActionDistributionForState(hs.s);
			
			Map<AbstractGroundedAction, Double> actionProbabilityMap = new HashMap<AbstractGroundedAction, Double>();
			for(ActionProb ap : falseAP){
				AbstractGroundedAction ga = ap.ga;
				if(!actionProbabilityMap.containsKey(ga)){
					actionProbabilityMap.put(ga, ap.pSelection + alpha);
				}
				else{
					System.err.println("There is a repeated action within the learned policy: " + ga.actionName());
				}
			}
			
			for(ActionProb ap : trueAP){
				AbstractGroundedAction ga = ap.ga;
				if(!actionProbabilityMap.containsKey(ga)){
					// here if this action is not present in the map we need to add a base probability to it
					actionProbabilityMap.put(ga, alpha);
				}
				// there is no else as the grounded action is already present in the hashmap
			}
			// the set of actions we have now has both the actions from true and learned policy
			// normalizing the learned policy probabilities in the hashmap
			
			double sum = 0.;
			for(AbstractGroundedAction ga : actionProbabilityMap.keySet()){
				sum+=actionProbabilityMap.get(ga);
			}
			
			for(AbstractGroundedAction ga : actionProbabilityMap.keySet()){
				actionProbabilityMap.put(ga, actionProbabilityMap.get(ga)/sum);
			}
			
			// actual KL divergence calculation
			
			for(ActionProb ap : trueAP){
				klDistance += ap.pSelection * Math.log(ap.pSelection/actionProbabilityMap.get(ap.ga));
			}
				
		}
		
		klDistance = klDistance/stateSet.size(); 
		
		
		return klDistance;
	}
	
	

}
