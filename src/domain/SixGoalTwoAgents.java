package domain;

import java.util.List;

import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.states.State;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGAgent;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;

public class SixGoalTwoAgents {

	private static SGDomain generateDomain(GridGame gridGame) {
		SGDomain domain = (SGDomain) gridGame.generateDomain();
		return domain;
	}
	
	private static JointActionModel generateJointActionModel(Domain domain) {
		return new GridGameStandardMechanics(domain);
	}
	
	private static TerminalFunction generateTerminalFunction(SGDomain domain) {
		return new GridGame.GGTerminalFunction(domain);
	}
	
	public static JointReward generateJointReward(SGDomain domain) {
		return new GridGame.GGJointRewardFunction(domain);
	};
	
	private static SGStateGenerator generateStateGenerator(final Domain domain, final int width, final int height) {
		
		return new SGStateGenerator() {
			@Override
			public State generateState(List<SGAgent> agents) {
				int numberGoals = (width + 1);
				State state = GridGame.getCleanState(domain, agents, 2, numberGoals, 2, 2, width, height);
				int startX = (width - 1) / 2;
				GridGame.setAgent(state, 0, startX, height - 1, 0);
				GridGame.setAgent(state, 1, startX, 0, 1);
				
				int goalsPerAgent = numberGoals / 2;
				for (int i = 0; i < numberGoals; i++) {
					int agent = i / goalsPerAgent;
					int x = (agent == 0) ? i * 2 : (i - goalsPerAgent) * 2;
					int y = (agent == 0) ? 0 : height - 1;
					GridGame.setGoal(state, i, x, y, agent + 1);
				}
				
				return state;
			}
			
		};
	}
	public static World generateWorld(int width, int height) {
		GridGame gridGame = new GridGame();
		SGDomain domain = SixGoalTwoAgents.generateDomain(gridGame);
		JointActionModel jointActionModel = SixGoalTwoAgents.generateJointActionModel(domain);
		TerminalFunction terminalFunction = SixGoalTwoAgents.generateTerminalFunction(domain);
		JointReward jointReward = new GridGame.GGJointRewardFunction(domain); //this.generateJointReward();
		
		SGStateGenerator stateGenerator = SixGoalTwoAgents.generateStateGenerator(domain, width, height);
		World world = new World((SGDomain)domain, jointReward, terminalFunction, stateGenerator);
		world.setDescription("Two agents, many goals. Width:" + width + " Height:" + height);
		
		return world;
	}

}
