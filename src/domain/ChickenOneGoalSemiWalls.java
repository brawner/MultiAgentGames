package domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;

public class ChickenOneGoalSemiWalls {

	public static SGDomain generateDomain(GridGame gridGame) {
		SGDomain domain = (SGDomain) gridGame.generateDomain();
		return domain;
	}
	
	public static JointActionModel generateJointActionModel(Domain domain) {
		return new GridGameStandardMechanics(domain);
	}
	
	public static TerminalFunction generateTerminalFunction(SGDomain domain) {
		return new GridGame.GGTerminalFunction(domain);
	}
	
	public static JointReward generateJointReward(SGDomain domain) {
		return new GridGame.GGJointRewardFunction(domain);
	};
	
	public static SGStateGenerator generateStateGenerator(final Domain domain, final int startA, final int startB, final boolean randomize) {
		
		return new SGStateGenerator() {
			private final Random random = new Random();
			@Override
			public State generateState(List<Agent> agents) {
				State state = GridGame.getCleanState(domain, agents, 2, 1, 4, 2, 3, 3);
				int startOne = startA;
				int startTwo = startB;
				if (randomize && this.random.nextBoolean()) {
					startOne = startB;
					startTwo = startA;
				}
				GridGame.setAgent(state, 0, startOne, 0, 0);
				GridGame.setAgent(state, 1, startTwo, 0, 1);
				GridGame.setHorizontalWall(state, 2, 1, 0, 0, 1);
				GridGame.setHorizontalWall(state, 3, 1, 2, 2, 3);
				GridGame.setGoal(state, 0, 1, 2, 0);
				
				return state;
			}
			
		};
	}
	public static World generateWorld(int startA, int startB, boolean randomize) {
		GridGame gridGame = new GridGame();
		SGDomain domain = ChickenOneGoalSemiWalls.generateDomain(gridGame);
		JointActionModel jointActionModel = ChickenOneGoalSemiWalls.generateJointActionModel(domain);
		TerminalFunction terminalFunction = ChickenOneGoalSemiWalls.generateTerminalFunction(domain);
		JointReward jointReward = new GridGame.GGJointRewardFunction(domain);
		
		SGStateGenerator stateGenerator = ChickenOneGoalSemiWalls.generateStateGenerator(domain, startA, startB, randomize);
		World world = new World((SGDomain)domain, jointActionModel, jointReward, terminalFunction, stateGenerator);
		world.setDescription("Chicken, one goal A:" + startA + " B:" + startB + " randomize: " + randomize);
		
		return world;
	}
	

}
