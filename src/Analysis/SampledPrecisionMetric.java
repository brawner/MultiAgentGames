package Analysis;

import java.util.HashMap;
import java.util.Map;

import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.stochasticgames.auxiliary.jointmdp.JointPolicyToCentralizedPolicy;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.states.State;
import burlap.oomdp.singleagent.common.UniformCostRF;

public class SampledPrecisionMetric {


	private Policy truePolicy;
	private Policy learnedPolicy;
	private State startState;
	Domain cmdp;
	TerminalFunction tf;
	private Map<String, Double> learnedTrajectoryMap;
	private Map<String, Double> trueTrajectoryMap;

	public SampledPrecisionMetric(Policy truePolicy, Policy learnedPolicy, State startState, Domain cmdp, TerminalFunction tf){
		this.tf = tf;
		this.cmdp = cmdp;
		this.truePolicy = new JointPolicyToCentralizedPolicy(truePolicy, cmdp);
		this.learnedPolicy = new JointPolicyToCentralizedPolicy(learnedPolicy, cmdp);
		this.startState = startState;
	}




	public double runPolicyComparison(int n){

		trueTrajectoryMap = getTrajectoryMap(n,truePolicy);
		learnedTrajectoryMap = getTrajectoryMap(n,learnedPolicy);
		

		double truePositive = 0.;
		double falsePositive =0.;
		for(String s:learnedTrajectoryMap.keySet()){
			if(trueTrajectoryMap.containsKey(s)){
				truePositive+=1.0;
			}
			else{
				falsePositive+=1.0;
			}
		}

		double ratio = truePositive/(truePositive+falsePositive);
			

		return ratio;
	}


	public Map<String, Double> getTrajectoryMap(int n, Policy p){
		Map<String, Double> trajectoryMap = new HashMap<String, Double>();

		for(int i=0;i<n;i++){
			EpisodeAnalysis ea = p.evaluateBehavior(this.startState, new UniformCostRF(), tf);
			String str = ea.getActionSequenceString();
			if(trajectoryMap.containsKey(str)){
				continue;
			}
			else{
				double value = 1.0;
				trajectoryMap.put(str, value);
			}
		}


		return trajectoryMap;
	}

}
