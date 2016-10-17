//package domain;
//
//import java.util.List;
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
//public class SixGoalTwoAgents {
//
//	private static SGDomain generateDomain(GridGame gridGame) {
//		SGDomain domain = (SGDomain) gridGame.generateDomain();
//		return domain;
//	}
//	
//	private static JointModel generateJointActionModel(Domain domain) {
//		return new GridGameStandardMechanics(domain);
//	}
//	
//	private static TerminalFunction generateTerminalFunction(SGDomain domain) {
//		return new GridGame.GGTerminalFunction(domain);
//	}
//	
//	public static JointRewardFunction generateJointReward(SGDomain domain) {
//		return new GridGame.GGJointRewardFunction(domain);
//	};
//	
//	private static StateGenerator generateStateGenerator(final Domain domain, final int width, final int height) {
//		
//		return new StateGenerator() {
//			@Override
//			public State generateState(List<SGAgent> agents) {
//				int numberGoals = (width + 1);
//				State state = GridGame.getCleanState(domain, agents, 2, numberGoals, 2, 2, width, height);
//				int startX = (width - 1) / 2;
//				GridGame.setAgent(state, 0, startX, height - 1, 0);
//				GridGame.setAgent(state, 1, startX, 0, 1);
//				
//				int goalsPerAgent = numberGoals / 2;
//				for (int i = 0; i < numberGoals; i++) {
//					int agent = i / goalsPerAgent;
//					int x = (agent == 0) ? i * 2 : (i - goalsPerAgent) * 2;
//					int y = (agent == 0) ? 0 : height - 1;
//					GridGame.setGoal(state, i, x, y, agent + 1);
//				}
//				
//				return state;
//			}
//			
//		};
//	}
//	public static World generateWorld(int width, int height) {
//		GridGame gridGame = new GridGame();
//		SGDomain domain = SixGoalTwoAgents.generateDomain(gridGame);
//		JointModel jointActionModel = SixGoalTwoAgents.generateJointActionModel(domain);
//		TerminalFunction terminalFunction = SixGoalTwoAgents.generateTerminalFunction(domain);
//		JointRewardFunction jointReward = new GridGame.GGJointRewardFunction(domain); //this.generateJointReward();
//		
//		StateGenerator stateGenerator = SixGoalTwoAgents.generateStateGenerator(domain, width, height);
//		World world = new World((SGDomain)domain, jointReward, terminalFunction, stateGenerator);
//		world.setDescription("Two agents, many goals. Width:" + width + " Height:" + height);
//		
//		return world;
//	}
//
//}
