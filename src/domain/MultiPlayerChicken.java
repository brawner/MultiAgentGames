//package domain;
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
//
//public class MultiPlayerChicken {
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
//	public static StateGenerator generateStateGenerator(final Domain domain, final int numberAgents, final int width, final int height) {
//		
//		return new StateGenerator() {
//			@Override
//			public State generateState(List<SGAgent> agents) {
//				State state = GridGame.getCleanState(domain, agents, numberAgents, numberAgents, 2, 2, width, height);
//				int maxX = width - 1;
//				int maxY = height - 1;
//				for (int i = 0; i < numberAgents; i++) {
//					int wall = i % 4;
//					int wallPosition = i / 4;
//					int startX = (wall < 2) ? 2 + 2*wallPosition : (wall - 2) * maxX;
//					int startY = (wall < 2) ? wall * maxY : 2 + 2*wallPosition;
//					int endX = maxX - startX;
//					int endY = maxY - startY;
//					GridGame.setAgent(state, i, startX, startY, i);
//					GridGame.setGoal(state, i, endX, endY, i+1);
//					
//				}
//				return state;
//			}
//			
//		};
//	}
//	public World generateWorld(int numberAgents, int width, int height) {
//		GridGame gridGame = new GridGame();
//		SGDomain domain = MultiPlayerChicken.generateDomain(gridGame);
//		JointModel jointActionModel = MultiPlayerChicken.generateJointActionModel(domain);
//		TerminalFunction terminalFunction = MultiPlayerChicken.generateTerminalFunction(domain);
//		JointRewardFunction jointReward = new GridGame.GGJointRewardFunction(domain); //this.generateJointReward();
//		
//		StateGenerator stateGenerator = MultiPlayerChicken.generateStateGenerator(domain, numberAgents, width, height);
//		World world = new World((SGDomain)domain, jointReward, terminalFunction, stateGenerator);
//		world.setDescription(numberAgents + " player chicken");
//		return world;
//	}
//	
//	
//	
//	public static World generateWorld() {
//		int numberAgents = 4;
//		int width = 2 * (int)((numberAgents - 2) / 4) + 5;
//		int height = 2 * (int)((numberAgents - 4) / 4) + 5;
//		
//		MultiPlayerChicken chicken = new MultiPlayerChicken();
//		World world = chicken.generateWorld(numberAgents, width, height);
//		world.setDescription("Four player chicken");
//		return world;
//	}
//}
