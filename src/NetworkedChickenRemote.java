//import Domain.MultiPlayerChicken;
//import burlap.domain.stochasticgames.gridgame.GGVisualizer;
//import burlap.oomdp.core.State;
//import burlap.oomdp.stochasticgames.JointActionModel;
//import burlap.oomdp.stochasticgames.SGDomain;
//import burlap.oomdp.stochasticgames.World;
//import burlap.oomdp.visualizer.Visualizer;
//
//
//public class NetworkedChickenRemote {
//
//	public static void main(String[] args) {
//		int numberAgents = 4;
//		int width = 2 * (int)((numberAgents - 2) / 4) + 5;
//		int height = 2 * (int)((numberAgents - 4) / 4) + 5;
//		
//		MultiPlayerChicken chicken = new MultiPlayerChicken();
//		World world = chicken.generateWorld(numberAgents, width, height);
//		SGDomain domain = (SGDomain)chicken.generateDomain();
//		chicken.addAgents(world, domain, numberAgents-1, 0, numberAgents-1);
//		//chicken.addHumanAgent(world, domain, server);
//		
//		Visualizer visualizer = GGVisualizer.getVisualizer(width, height);
//		
//		
//		world.generateNewCurrentState();
//		State startState = world.getCurrentWorldState();
//		JointActionModel jam = world.getActionModel();
//		
//		/*SGVisualExplorerClient client = new SGVisualExplorerClient(domain, visualizer, startState, jam);
//		client.attemptConnect("ws://localhost:8080/events/");
//		client.initGUI();
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.out.println("Connecting to world0");
//		client.connectToWorld("world0");*/
//	}
//
//}
