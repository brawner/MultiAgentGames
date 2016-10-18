package networking.common;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import networking.common.messages.WorldFile;
import networking.server.GridGameManager;
import burlap.domain.stochasticdomain.gridgame.GridGameStandardMechanicsWithoutTieBreaking;
import burlap.domain.stochasticdomain.gridgame.NullStateMapping;
import burlap.domain.stochasticdomain.gridgame.SimultaneousGoalRewardFunction;
import burlap.domain.stochasticdomain.world.NetworkWorld;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.oo.OODomain;
import burlap.mdp.stochasticgames.SGDomain;
import burlap.mdp.stochasticgames.model.JointModel;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import burlap.mdp.stochasticgames.world.World;

/**
 * Handles loading of a World from a file, a directory, or message tokens.
 * @author brawner
 *
 */
public class GridGameWorldLoader {


	public static GridGameServerToken loadText(String filename) {
		return GridGameServerToken.tokenFromFile(filename);
	}

	private static WorldLoadingStateGenerator generateStateGenerator(GridGameServerToken fileToken, final SGDomain domain, int goalsPerAgent) throws TokenCastException {
		return WorldLoadingStateGenerator.stateGenerator(fileToken, domain, goalsPerAgent);
	}
	
	private static WorldLoadingStateGenerator generateStateGenerator(GridGameServerToken fileToken, final SGDomain domain, int goalsPerAgent, boolean useImmutableStates) throws TokenCastException {
		return WorldLoadingStateGenerator.stateGenerator(fileToken, domain, goalsPerAgent);
	}
	
	public static NetworkWorld loadWorld(GridGameServerToken token, boolean useImmutableStates) {
		GridGame gridGame = new GridGame();

		NetworkWorld world = null;
		try {
			Boolean randomlyBreakTies = token.getBoolean(WorldFile.RANDOM_TIE_BREAKS);
			int numAgents = token.getTokenList(WorldFile.AGENTS).size();
			gridGame.setMaxPlyrs(numAgents);
			SGDomain domain;
			
			if (randomlyBreakTies == null) {
				domain = gridGame.generateDomain();
			} else {
				domain = gridGame.generateDomain();
				JointModel noTieBreaks = new GridGameStandardMechanicsWithoutTieBreaking(domain);
				domain.setJointActionModel(noTieBreaks);
			}
			
			TerminalFunction terminalFunction = new GridGame.GGTerminalFunction((OODomain) domain);
			JointRewardFunction jointReward = new SimultaneousGoalRewardFunction(1.0, 0.0);
			
			Integer goalsPerAgent = token.getInt(WorldFile.GOALS_PER_AGENT);
			goalsPerAgent = (goalsPerAgent == null ) ? 0 : goalsPerAgent;
			
			String description = token.getString(WorldFile.DESCRIPTION);
			

			WorldLoadingStateGenerator stateGenerator = 
					GridGameWorldLoader.generateStateGenerator(token, domain, goalsPerAgent, useImmutableStates);
			world = new NetworkWorld((SGDomain)domain, jointReward, terminalFunction, stateGenerator, new NullStateMapping(), numAgents, description);
			

			
		} catch (TokenCastException e) {
			e.printStackTrace();
			return null;
		}

		return world;
	}

	public static NetworkWorld loadWorld(GridGameServerToken token) {
		return GridGameWorldLoader.loadWorld(token, false);
	}
	
	
	
//	public static World loadWorld(String filename, double stepCost, double reward, boolean incurCostOnNoop){
//		GridGame gridGame = new GridGame();
//		GridGameServerToken token = GridGameWorldLoader.loadText(filename);
//		SGDomain domain = gridGame.generateDomain();
//		JointRewardFunction jointReward = GridGameExtreme.getSimultaneousGoalRewardFunction(reward, stepCost);
//		
//		return GridGameWorldLoader.loadWorld(token, domain, jointReward, false);
//	}
	
	public static NetworkWorld loadWorld(String filename, double stepCost, double reward, boolean incurCostOnNoop, double noopCost){
		return GridGameWorldLoader.loadWorld(filename, stepCost, reward, incurCostOnNoop, noopCost);
	}
	
//	public static World loadWorld(String filename, double stepCost, double reward, boolean incurCostOnNoop, double noopCost, boolean useImmutableStates){
//		GridGame gridGame = new GridGame();
//		GridGameServerToken token = GridGameWorldLoader.loadText(filename);
//		
//		SGDomain domain = gridGame.generateDomain();
//		JointRewardFunction jointReward = new SpecifyNoopCostRewardFunction(domain, stepCost, reward, reward, incurCostOnNoop, noopCost);
//		return GridGameWorldLoader.loadWorld(token, domain, jointReward, useImmutableStates);
//	}
//	
	public static NetworkWorld loadWorld(GridGameServerToken token, SGDomain domain, JointRewardFunction jointReward){
		NetworkWorld world = null;
		
		GridGame gridGame = new GridGame();
		try {
			int max = Integer.valueOf(token.getInt("width"));
			int maxh = Integer.valueOf(token.getInt("height"));
			if(maxh>max){
				max = maxh;
			}
			gridGame.setMaxDim(max);
			
			int numAgents = token.getTokenList(WorldFile.AGENTS).size();
			gridGame.setMaxPlyrs(numAgents);
			
			TerminalFunction terminalFunction = new GridGame.GGTerminalFunction((OODomain) domain);
			
			Integer goalsPerAgent = token.getInt(WorldFile.GOALS_PER_AGENT);
			goalsPerAgent = (goalsPerAgent == null ) ? 0 : goalsPerAgent;
			String description = token.getString(WorldFile.DESCRIPTION);
			
			WorldLoadingStateGenerator stateGenerator = GridGameWorldLoader.generateStateGenerator(token, domain, goalsPerAgent);
			world = new NetworkWorld((SGDomain)domain, jointReward, terminalFunction, stateGenerator, null, numAgents, description);

			
		} catch (TokenCastException e) {
			e.printStackTrace();
		}

		return world;
	
	}
		
	
	
	
	

	
	public static World loadWorld(String filename) {
		GridGameServerToken fileToken = GridGameWorldLoader.loadText(filename);
		return GridGameWorldLoader.loadWorld(fileToken);
	}

	//TODO add in environment variable expanding, and other good stuff
	public static Path expandDirectory(String directory) {
		return Paths.get(directory);
	}

	public static List<File> walkDirectory(File file) {
		List<File> files = new ArrayList<File>();
		if (file.isFile()) {
			files.add(file);
		} else if (file.isDirectory()){
			for (File f : file.listFiles()) {
				if (f.isDirectory()) {
					walkDirectory(file);
				} else if (f.isFile()) {
					files.add(f);
				}
			}
		}
		return files;	
	}

	public static List<GridGameServerToken> loadWorldTokens(String directory) {
		System.out.println("Lodaing worlds!!");
		Path path = GridGameWorldLoader.expandDirectory(directory);
		File file = path.toFile();
		if (!file.exists()) {
			System.err.println(directory + " could not be found. Check the path");
		}

		List<File> files = GridGameWorldLoader.walkDirectory(file);

		List<GridGameServerToken> worlds = new ArrayList<GridGameServerToken>();

		//int count = 0;
		for (File f : files) {
			if (f.isFile()) {
				String filename = f.getAbsolutePath();
				GridGameServerToken world = GridGameWorldLoader.loadText(filename);
				//world.setString(WorldFile.LABEL, "world" + count++);
				String[] temp = f.getName().split("\\.");
				if(temp.length>=2){
					String worldUniqueId = temp[temp.length-2];
					System.out.println("UID: "+worldUniqueId);
					world.setString(WorldFile.LABEL, worldUniqueId);
					worlds.add(world);
				}
			}
		}
		return worlds;
	}

	public static List<World> loadWorlds(String directory) {
		List<GridGameServerToken> worldTokens = GridGameWorldLoader.loadWorldTokens(directory);
		return GridGameWorldLoader.loadWorlds(worldTokens);
	}

	public static List<World> loadWorlds(List<GridGameServerToken> worldTokens) {
		List<World> worlds = new ArrayList<World>(worldTokens.size());
		for (GridGameServerToken token : worldTokens) {
			World world = GridGameWorldLoader.loadWorld(token);
			worlds.add(world);
		}
		return worlds;
	}

	public static List<World> loadWorlds(GridGameServerToken token) {
		try {
			List<GridGameServerToken> worldTokens = 
					token.getTokenList(GridGameManager.WORLDS);
			return GridGameWorldLoader.loadWorlds(worldTokens);
		} catch (TokenCastException e) {
			e.printStackTrace();
			return null;
		}
	}



	public static class WorldLoaderException extends RuntimeException {

		public WorldLoaderException(String string) {
			super(string);
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 3175358082484906501L;

	}





}
