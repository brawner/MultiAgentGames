//package domain;
//
//import java.util.List;
//import java.util.Random;
//
//import burlap.domain.stochasticgames.gridgame.GridGame;
//import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
//import burlap.mdp.auxiliary.StateGenerator;
//import burlap.mdp.core.Domain;
//import burlap.mdp.core.TerminalFunction;
//import burlap.mdp.core.state.State;
//import burlap.mdp.stochasticgames.SGDomain;
//import burlap.mdp.stochasticgames.agent.SGAgent;
//import burlap.mdp.stochasticgames.model.JointModel;
//import burlap.mdp.stochasticgames.model.JointRewardFunction;
//import burlap.mdp.stochasticgames.world.World;
//
//public class ChickenOneGoalSemiWalls {
//
//	public static SGDomain generateDomain(GridGame gridGame) {
//		SGDomain domain = (SGDomain) gridGame.generateDomain();
//		return domain;
//	}
//	
//	public static JointModel generateJointActionModel(Domain domain) {
//		return new GridGameStandardMechanics(domain);
//	}
//	
//	public static TerminalFunction generateTerminalFunction(SGDomain domain) {
//		return new GridGame.GGTerminalFunction(domain);
//	}
//	
//	public static JointRewardFunction generateJointReward(SGDomain domain) {
//		return new GridGame.GGJointRewardFunction(domain);
//	};
//	
//	public static StateGenerator generateStateGenerator(final Domain domain, final int startA, final int startB, final boolean randomize) {
//		
//		return new StateGenerator() {
//			private final Random random = new Random();
//			@Override
//			public State generateState(List<SGAgent> agents) {
//				State state = GridGame.getCleanState(domain, agents, 2, 1, 4, 2, 3, 3);
//				int startOne = startA;
//				int startTwo = startB;
//				if (randomize && this.random.nextBoolean()) {
//					startOne = startB;
//					startTwo = startA;
//				}
//				GridGame.setAgent(state, 0, startOne, 0, 0);
//				GridGame.setAgent(state, 1, startTwo, 0, 1);
//				GridGame.setHorizontalWall(state, 2, 1, 0, 0, 1);
//				GridGame.setHorizontalWall(state, 3, 1, 2, 2, 3);
//				GridGame.setGoal(state, 0, 1, 2, 0);
//				
//				return state;
//			}
//			
//		};
//	}
//	public static World generateWorld(int startA, int startB, boolean randomize) {
//		GridGame gridGame = new GridGame();
//		SGDomain domain = ChickenOneGoalSemiWalls.generateDomain(gridGame);
//		JointModel jointActionModel = ChickenOneGoalSemiWalls.generateJointActionModel(domain);
//		TerminalFunction terminalFunction = ChickenOneGoalSemiWalls.generateTerminalFunction(domain);
//		JointRewardFunction jointReward = new GridGame.GGJointRewardFunction(domain);
//		
//		StateGenerator stateGenerator = ChickenOneGoalSemiWalls.generateStateGenerator(domain, startA, startB, randomize);
//		World world = new World((SGDomain)domain, jointReward, terminalFunction, stateGenerator);
//		world.setDescription("Chicken, one goal A:" + startA + " B:" + startB + " randomize: " + randomize);
//		
//		return world;
//	}
//	
//
//}
