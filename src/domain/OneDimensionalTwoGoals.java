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

public class OneDimensionalTwoGoals {

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
	
	private static JointReward generateJointReward(SGDomain domain) {
		return new GridGame.GGJointRewardFunction(domain);
	};
	
	private static SGStateGenerator generateStateGenerator(final Domain domain, final int width) {
		
		return new SGStateGenerator() {
			@Override
			public State generateState(List<SGAgent> agents) {
				State state = GridGame.getCleanState(domain, agents, 2, 3, 2, 2, width, 1);
				int startX1 = width - 4;
				int startX2 = width - 3;
				int goalX1 = width - 2;
				
				GridGame.setAgent(state, 0, startX1, 0, 0);
				GridGame.setAgent(state, 1, startX2, 0, 1);
				GridGame.setGoal(state, 0, goalX1, 0, 1);
				GridGame.setGoal(state, 1, 0, 0, 1);
				GridGame.setGoal(state, 2, 2, 0, 2);

				return state;
			}
			
		};
	}
	public static World generateWorld(int width) {
		GridGame gridGame = new GridGame();
		SGDomain domain = OneDimensionalTwoGoals.generateDomain(gridGame);
		JointActionModel jointActionModel = OneDimensionalTwoGoals.generateJointActionModel(domain);
		TerminalFunction terminalFunction = OneDimensionalTwoGoals.generateTerminalFunction(domain);
		JointReward jointReward = OneDimensionalTwoGoals.generateJointReward(domain);
		
		SGStateGenerator stateGenerator = OneDimensionalTwoGoals.generateStateGenerator(domain, width);
		World world = new World((SGDomain)domain, jointReward, terminalFunction, stateGenerator);
		world.setDescription("Two agents, One dimension, Three goals. Width:" + width);
		
		return world;
	}

}
