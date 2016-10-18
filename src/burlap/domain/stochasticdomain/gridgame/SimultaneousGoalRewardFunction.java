package burlap.domain.stochasticdomain.gridgame;

import java.util.List;

import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.state.GGAgent;
import burlap.domain.stochasticgames.gridgame.state.GGGoal;
import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.oo.state.generic.GenericOOState;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.model.JointRewardFunction;

// TODO, I think this only works if there is a single goal for each agent
public class SimultaneousGoalRewardFunction implements JointRewardFunction {
	private final double goalReward;
	private final double stepCost;
	public SimultaneousGoalRewardFunction(double goalReward, double stepCost) {
		this.goalReward = goalReward;
		this.stepCost = stepCost;
	}

	@Override
	public double[] reward(State s, JointAction ja, State sp) {
		GenericOOState ooState = (GenericOOState)sp;
		List<ObjectInstance> agents = ooState.objectsOfClass(GridGame.CLASS_AGENT);
		List<ObjectInstance> goals = ooState.objectsOfClass(GridGame.CLASS_GOAL);
		
		double[] reward = new double[agents.size()];
		
		boolean allPlayersInGoals = true;
		for (ObjectInstance agent : agents) {
			GGAgent ggAgent = (GGAgent)agent;
			for (ObjectInstance goal : goals) {
				GGGoal ggGoal = (GGGoal)goal;
				if (ggAgent.player + 1 == ggGoal.type) {
					if (ggAgent.x != ggGoal.x || ggAgent.y != ggGoal.y) {
						allPlayersInGoals = false;
						break;
					}
				}
			}
			if (!allPlayersInGoals) {
				break;
			}
		}
		double rewardValue = (allPlayersInGoals) ? goalReward : stepCost;
		for (int i = 0; i < agents.size(); i++) {
			reward[i] = rewardValue;
		}
		return reward;
	}

}
