package Analysis;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.stochasticgames.auxiliary.jointmdp.CentralizedDomainGenerator.GroundedJointActionWrapper;
import burlap.behavior.stochasticgames.auxiliary.jointmdp.DecentralizedPoliciesToJointPolicies;
import burlap.behavior.stochasticgames.auxiliary.jointmdp.JointPolicyToCentralizedPolicy;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.states.MutableState;
import burlap.oomdp.core.states.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.common.UniformCostRF;

public class SampledNormFollowingRatio {

	Policy jointPolicy;
	Policy centralizedPolicy;
	State startState;
	int optTrajLen;
	TerminalFunction tf;
	Domain cmdp;

	public SampledNormFollowingRatio(Policy trueAgentPolicy, Policy learnedAgentPolicy, State startState, Domain cmdp, TerminalFunction tf, int optimalTrajectoryLength){
		List<Policy> decentralizedPolicyList = new ArrayList<Policy>();
		
		decentralizedPolicyList.add(0,learnedAgentPolicy);
		decentralizedPolicyList.add(1,trueAgentPolicy);
		
		
		this.jointPolicy = new DecentralizedPoliciesToJointPolicies(decentralizedPolicyList);
		
		this.centralizedPolicy = new JointPolicyToCentralizedPolicy(jointPolicy, cmdp);
		this.startState = startState;
		this.tf = tf;
		optTrajLen = optimalTrajectoryLength;
	}

	public double runPolicyComparison(int numGames){
		double pass = 0.;
		double fail = 0.;
		for(int i =0;i<numGames;i++){
//			System.out.println(i);
//			State s = startState.copy();
//			for(int j =0;j<5;j++){
//				Action a = centralizedPolicy.getAction(s);
//			}
			
			EpisodeAnalysis ea = centralizedPolicy.evaluateBehavior(this.startState, new UniformCostRF(), tf, 10);
			
//			for(GroundedAction a:ea.actionSequence){
//				System.out.println(a.actionName());
//				System.out.println(((GroundedJointActionWrapper)a).jointAction.actionName());
//				System.out.println(((GroundedJointActionWrapper)a).jointAction.actions.size());
//				System.out.println("_________________");
//			}
			
			if(ea.actionSequence.size() <= optTrajLen){
				pass++;
			}
			else{
				fail++;
			}
		}
		double ratio = pass/(pass + fail);
		return ratio;
	}


}
