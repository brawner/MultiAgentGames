package Analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import burlap.behavior.policy.Policy;
import burlap.behavior.policy.Policy.ActionProb;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.stochasticgames.auxiliary.jointmdp.JointPolicyToCentralizedPolicy;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.states.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.environment.Environment;
import burlap.oomdp.statehashing.HashableState;
import burlap.oomdp.statehashing.HashableStateFactory;
import burlap.oomdp.statehashing.SimpleHashableStateFactory;


/** 
 * This metric measures the KL divergence between two learned policies. Some of the action probabilities in the learned 
 * policy might be zero, which is not ideal for KL divergence. Hence we use a simple prior of 10^-6 as base probabilities
 * of each action.
 * @author ngopalan
 */

public class TrajectoryKLDivergenceMetric {
	
	private Policy truePolicy;
	private Policy learnedPolicy;
	private State startState;
//	private HashableStateFactory hsf = new SimpleHashableStateFactory();
	Domain cmdp;
	TerminalFunction tf;
	private double alpha = Math.pow(10, -6);
	private Map<String, Double> learnedTrajectoryMap;
	private Map<String, Double> trueTrajectoryMap;
	
	
	public TrajectoryKLDivergenceMetric(Policy truePolicy, Policy learnedPolicy, State startState, Domain cmdp,TerminalFunction tf){
		this.tf = tf;
		this.cmdp = cmdp;
		this.truePolicy = new JointPolicyToCentralizedPolicy(truePolicy, cmdp);
		this.learnedPolicy = new JointPolicyToCentralizedPolicy(learnedPolicy, cmdp);
		this.startState = startState;
	}
	
	public double runPolicyComparison(int n){
		
		trueTrajectoryMap = getTrajectoryMap(n,truePolicy);
		learnedTrajectoryMap = getTrajectoryMap(n,learnedPolicy);
		double sum =0.;
		for(String str : trueTrajectoryMap.keySet() ){
			if(learnedTrajectoryMap.containsKey(str)){
				learnedTrajectoryMap.put(str, learnedTrajectoryMap.get(str)+alpha);
			}
			else{
				learnedTrajectoryMap.put(str, alpha);
			}
		}

		for(String str : learnedTrajectoryMap.keySet() ){
			sum += learnedTrajectoryMap.get(str);
		}
		
		for(String str : learnedTrajectoryMap.keySet() ){
			if(learnedTrajectoryMap.containsKey(str)){
				learnedTrajectoryMap.put(str, learnedTrajectoryMap.get(str)/sum);
			}
			else{
				System.out.println("Something wrong this trajectory is missing : " + str);
			}
		}
		
		
		
		double klDistance =0.;
		
		for(String str : trueTrajectoryMap.keySet()){
			double value = trueTrajectoryMap.get(str)/learnedTrajectoryMap.get(str);
			if(value!=0.){
				if(value<0){
					System.err.println("action probabilities negative: true policy - "+ trueTrajectoryMap.get(str) + ", learned policy - " + learnedTrajectoryMap.get(str) );
					System.err.println("trajectory causing error: " + str);
				}
				double logValue = Math.log(value);
				klDistance += trueTrajectoryMap.get(str) * logValue;
			}
		}
		
		
		
		return klDistance;
	}
	
	
	public Map<String, Double> getTrajectoryMap(int n, Policy p){
		Map<String, Double> trajectoryMap = new HashMap<String, Double>();
		
		for(int i=0;i<n;i++){
			EpisodeAnalysis ea = p.evaluateBehavior(this.startState, new UniformCostRF(), tf);
			String str = ea.getActionSequenceString();
			if(trajectoryMap.containsKey(str)){
				trajectoryMap.put(str, trajectoryMap.get(str)+ (1.0/n));
			}
			else{
				trajectoryMap.put(str, 1.0/n);
			}
		}
		
		
		return trajectoryMap;
	}
	

}
