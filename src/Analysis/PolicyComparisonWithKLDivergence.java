//package Analysis;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import burlap.behavior.policy.Policy;
//import burlap.behavior.policy.support.ActionProb;
//import burlap.behavior.singleagent.auxiliary.StateReachability;
//import burlap.mdp.core.Domain;
//import burlap.mdp.core.action.Action;
//import burlap.mdp.core.state.State;
//import burlap.statehashing.HashableState;
//import burlap.statehashing.HashableStateFactory;
//import burlap.statehashing.simple.SimpleHashableStateFactory;
//
//
///** 
// * This metric measures the KL divergence between two learned policies. Some of the action probabilities in the learned 
// * policy might be zero, which is not ideal for KL divergence. Hence we use a simple prior of 10^-6 as base probabilities
// * of each action.
// * @author ngopalan
// */
//
//public class PolicyComparisonWithKLDivergence {
//	
//	private Policy truePolicy;
//	private Policy learnedPolicy;
//	private State startState;
//	private HashableStateFactory hsf = new SimpleHashableStateFactory();
//	Domain cmdp;
//	private double alpha = Math.pow(10, -6);
//	
//	public PolicyComparisonWithKLDivergence(Policy truePolicy, Policy learnedPolicy, State startState, Domain cmdp){
//	
//		this.cmdp = cmdp;
//		this.truePolicy = new JointPolicyToCentralizedPolicy(truePolicy, cmdp);
//		this.learnedPolicy = new JointPolicyToCentralizedPolicy(learnedPolicy, cmdp);
//		this.startState = startState ;
//		
//		
//	}
//	
//	public double runPolicyComparison(){
//		double klDistance =0.;
//		
//		StateReachability sr = StateReachability.getPolicyReachableStates(domain, p, from, usingHashFactory)
//		Set<HashableState> stateSet = sr.getPolicyReachableHashedStates(truePolicy, startState, hsf);
//		stateSet.addAll(sr.getPolicyReachableHashedStates(learnedPolicy, startState, hsf));
//		
//		
//		
//		for(HashableState hs : stateSet ){
//			List<ActionProb> trueAP = truePolicy.getActionDistributionForState(hs.s);
//			List<ActionProb> falseAP = learnedPolicy.getActionDistributionForState(hs.s);
//			
//			Map<Action, Double> actionProbabilityMap = new HashMap<Action, Double>();
//			for(ActionProb ap : falseAP){
//				Action ga = ap.ga;
//				if(!actionProbabilityMap.containsKey(ga)){
//					actionProbabilityMap.put(ga, ap.pSelection + alpha);
//				}
//				else{
//					System.err.println("There is a repeated action within the learned policy: " + ga.actionName());
//				}
//			}
//			
//			for(ActionProb ap : trueAP){
//				Action ga = ap.ga;
//				if(!actionProbabilityMap.containsKey(ga)){
//					// here if this action is not present in the map we need to add a base probability to it
//					actionProbabilityMap.put(ga, alpha);
//				}
//				// there is no else as the grounded action is already present in the hashmap
//			}
//			// the set of actions we have now has both the actions from true and learned policy
//			// normalizing the learned policy probabilities in the hashmap
//			
//			double sum = 0.;
//			for(Action ga : actionProbabilityMap.keySet()){
//				sum+=actionProbabilityMap.get(ga);
//			}
//			
//			for(Action ga : actionProbabilityMap.keySet()){
//				actionProbabilityMap.put(ga, actionProbabilityMap.get(ga)/sum);
//			}
//			
//			// actual KL divergence calculation
//			
//			for(ActionProb ap : trueAP){
////				klDistance += ap.pSelection * Math.log(ap.pSelection/actionProbabilityMap.get(ap.ga));
//				double value = ap.pSelection/actionProbabilityMap.get(ap.ga);
//				if(value!=0.){
//					if(value<0){
//						System.err.println("action probabilities negative: true policy - "+ ap.pSelection + ", learned policy - " +actionProbabilityMap.get(ap.ga));
//					}
//					double logValue = Math.log(value);
//					klDistance += ap.pSelection * logValue;
//				}
//			}
//				
//		}
//		
//		klDistance = klDistance/stateSet.size(); 
//		
//		
//		return klDistance;
//	}
//	
//	
//
//}
