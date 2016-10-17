//package experiments;
//
//import burlap.domain.stochasticgames.gridgame.GridGame;
//import burlap.mdp.stochasticgames.SGDomain;
//import burlap.mdp.stochasticgames.world.World;
//
//public class Testing {
//	
//	public static void main (String[] args){
//		
//		GridGame test = new GridGame();
//		SGDomain domain = (SGDomain)test.generateDomain();
//		
//		World w = new World(domain, new GridGame.GGJointRewardFunction(domain), 
//				new GridGame.GGTerminalFunction(domain), 
//				new ConstantSGStateGenerator(GridGame.getPrisonersDilemmaInitialState(domain)), new NullAbstraction(), 2);
//		
//
//	}
//
//}
