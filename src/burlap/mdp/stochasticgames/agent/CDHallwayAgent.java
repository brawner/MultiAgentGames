package burlap.mdp.stochasticgames.agent;

import java.util.List;

import burlap.behavior.policy.Policy;
import burlap.behavior.stochasticgames.agents.SetStrategySGAgent;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.state.GGAgent;
import burlap.domain.stochasticgames.gridgame.state.GGGoal;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.SimpleAction;
import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.oo.state.generic.GenericOOState;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.SGDomain;

public class CDHallwayAgent{
	public static SetStrategySGAgent getAgent(SGDomain domain, String agentName) {
		int agentNum = Integer.parseInt(agentName);
		Policy policy = new CDHallwayPolicy(agentNum);
		return new SetStrategySGAgent(domain, policy, agentName, GridGame.getStandardGridGameAgentType(domain));
	}
	
	public static class CDHallwayPolicy implements Policy {
		private Integer agentNum;
		public CDHallwayPolicy(int agentNum) {
			this.agentNum = agentNum;
		}
		
		private GGAgent getPosition(State s, int playerNum) {
			GenericOOState ooState = (GenericOOState)s;
			List<ObjectInstance> agents = ooState.objectsOfClass(GridGame.CLASS_AGENT);
			return (GGAgent)agents.get(playerNum);
		}
		
		private GGGoal getGoal(State s, int playerNum) {
			GenericOOState ooState = (GenericOOState)s;
			List<ObjectInstance> goals = ooState.objectsOfClass(GridGame.CLASS_GOAL);
			for (ObjectInstance goalObj : goals) {
				GGGoal goal = (GGGoal)goalObj;
				if (goal.type == playerNum + 1) {
					return goal;
				}
			}
			return null;
		}
		
		private GGAgent getOtherAgentPosition(State s) {
			return this.getPosition(s, 1 - this.agentNum);
		}
		
		private GGAgent getAgentPosition(State s) {
			return this.getPosition(s, this.agentNum);
		}

		private GGGoal getOtherGoal(State s) {
			return this.getGoal(s, 1 - this.agentNum);
		}
		
		private GGGoal getAgentGoal(State s) {
			return this.getGoal(s, this.agentNum);
		}
		
		private Action proceedToGoal(boolean flipDirection) {
			return (flipDirection) ? new SimpleAction(GridGame.ACTION_WEST) :
				new SimpleAction(GridGame.ACTION_EAST);
		}
		
		@Override
		public Action action(State s) {
			
			GGAgent aPos = this.getAgentPosition(s);
			GGAgent oPos = this.getOtherAgentPosition(s);
			
			GGGoal aGoal = this.getAgentGoal(s);
			GGGoal oGoal = this.getOtherGoal(s);
			
			int aStepsToGoal = Math.abs(aPos.x - aGoal.x) + Math.abs(aPos.y - aGoal.y);
			int oStepsToGoal = Math.abs(oPos.x - oGoal.x) + Math.abs(oPos.y - oGoal.y);
			
			boolean flipDirection = (aGoal.x < oGoal.x);
			boolean sameRow = (aPos.y == oPos.y);
			boolean agentsHavePassed = (aPos.x >= oPos.x ^ flipDirection);
			boolean onGoalCol = (aPos.x == aGoal.x);
			boolean changeRow = (sameRow && !agentsHavePassed) || onGoalCol ;
			
			boolean needToBlock = (aStepsToGoal > oStepsToGoal);
			
			if (needToBlock) {
				switch(aPos.y){ 
				case 0: return new SimpleAction(GridGame.ACTION_NORTH);
				case 1: return this.proceedToGoal(flipDirection);
				case 2: return new SimpleAction(GridGame.ACTION_SOUTH);
				default: return new SimpleAction(GridGame.ACTION_NOOP);
				}
			}
			
			boolean needToWait = (aStepsToGoal == 1 && oStepsToGoal > 1);
			if (needToWait) {
				return new SimpleAction(GridGame.ACTION_NOOP);
			}
			if (changeRow) {
				switch(aPos.y){ 
				case 0: return new SimpleAction(GridGame.ACTION_NORTH);
				case 1: return new SimpleAction(GridGame.ACTION_NORTH);
				case 2: return new SimpleAction(GridGame.ACTION_SOUTH);
				default: return new SimpleAction(GridGame.ACTION_NOOP);
				}
			} else {
				return this.proceedToGoal(flipDirection);
			}
		}

		@Override
		public double actionProb(State s, Action a) {
			if (a.actionName() == this.action(s).actionName()) {
				return 1.0;
			} else {
				return 0.0;
			}
		}

		@Override
		public boolean definedFor(State s) {
			return true;
		}
		
	}

}
