package behavior;

import burlap.domain.stochasticgames.gridgame.GridGame.GGJointRewardFunction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.stochasticgames.JointAction;

public class SpecifyNoopCostRewardFunction extends GGJointRewardFunction {

	double noopCost;

	public SpecifyNoopCostRewardFunction(Domain ggDomain, double stepCost,
			double goalReward, double goalReward2, boolean noopIncursStepCost,
			double noopCost) {
		super(ggDomain, stepCost, goalReward, goalReward2, noopIncursStepCost);
		this.noopCost = noopCost;
	}

	@Override
	protected double defaultCost(String aname, JointAction ja) {
		double cost = super.defaultCost(aname, ja);
		if ((ja.action(aname)
				.actionName()
				.equals("noop")) 
				&& (cost != 0))
			return noopCost;
		else
			return cost;
	}

}
