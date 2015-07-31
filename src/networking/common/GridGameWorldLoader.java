package networking.common;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import networking.common.messages.WorldFile;
import networking.server.GridGameManager;
import behavior.SpecifyNoopCostRewardFunction;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.auxiliary.StateAbstraction;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;

/**
 * Handles loading of a World from a file, a directory, or message tokens.
 * @author brawner
 *
 */
public class GridGameWorldLoader {


	private static GridGameServerToken loadText(String filename) {
		return GridGameServerToken.tokenFromFile(filename);
	}

	private static WorldLoadingStateGenerator generateStateGenerator(GridGameServerToken fileToken, final SGDomain domain, int goalsPerAgent) throws TokenCastException {
		return WorldLoadingStateGenerator.stateGenerator(fileToken, domain, goalsPerAgent);
	}

	public static World loadWorld(GridGameServerToken token) {
		GridGame gridGame = new GridGame();

		SGDomain domain = GridGameExtreme.generateDomain(gridGame);
		TerminalFunction terminalFunction = GridGameExtreme.generateTerminalFunction(domain);
		JointReward jointReward = GridGameExtreme.generateJointReward(domain);


		World world = null;
		try {
			Integer goalsPerAgent = token.getInt(WorldFile.GOALS_PER_AGENT);
			goalsPerAgent = (goalsPerAgent == null ) ? 0 : goalsPerAgent;

			WorldLoadingStateGenerator stateGenerator = GridGameWorldLoader.generateStateGenerator(token, domain, goalsPerAgent);
			StateAbstraction abstraction = new GoalAbstraction(stateGenerator.generateAbstractedState());
			int numAgents = token.getTokenList(WorldFile.AGENTS).size();
			world = new World((SGDomain)domain, jointReward, terminalFunction, stateGenerator, abstraction, numAgents);

			String description = token.getString(WorldFile.DESCRIPTION);
			world.setDescription(description);

		} catch (TokenCastException e) {
			e.printStackTrace();
			return null;
		}

		return world;
	}
	
	public static World loadWorld(String filename, double stepCost, double reward, boolean incurCostOnNoop){
		GridGame gridGame = new GridGame();
		GridGameServerToken token = GridGameWorldLoader.loadText(filename);
		SGDomain domain = GridGameExtreme.generateDomain(gridGame);
		TerminalFunction terminalFunction = GridGameExtreme.generateTerminalFunction(domain);
		JointReward jointReward = GridGameExtreme.generateJointReward(domain, stepCost, reward, incurCostOnNoop);


		World world = null;
		try {
			Integer goalsPerAgent = token.getInt(WorldFile.GOALS_PER_AGENT);
			goalsPerAgent = (goalsPerAgent == null ) ? 0 : goalsPerAgent;

			WorldLoadingStateGenerator stateGenerator = GridGameWorldLoader.generateStateGenerator(token, domain, goalsPerAgent);
			StateAbstraction abstraction = new GoalAbstraction(stateGenerator.generateAbstractedState());
			int numAgents = token.getTokenList(WorldFile.AGENTS).size();
			world = new World((SGDomain)domain, jointReward, terminalFunction, stateGenerator, abstraction, numAgents);

			String description = token.getString(WorldFile.DESCRIPTION);
			world.setDescription(description);

		} catch (TokenCastException e) {
			e.printStackTrace();
			return null;
		}

		return world;
	}
	
	public static World loadWorld(String filename, double stepCost, double reward, boolean incurCostOnNoop, double noopCost){
		GridGame gridGame = new GridGame();
		GridGameServerToken token = GridGameWorldLoader.loadText(filename);
		SGDomain domain = GridGameExtreme.generateDomain(gridGame);
		TerminalFunction terminalFunction = GridGameExtreme.generateTerminalFunction(domain);
		JointReward jointReward = new SpecifyNoopCostRewardFunction(domain, stepCost, reward, reward, incurCostOnNoop, noopCost);


		World world = null;
		try {
			Integer goalsPerAgent = token.getInt(WorldFile.GOALS_PER_AGENT);
			goalsPerAgent = (goalsPerAgent == null ) ? 0 : goalsPerAgent;

			WorldLoadingStateGenerator stateGenerator = GridGameWorldLoader.generateStateGenerator(token, domain, goalsPerAgent);
			StateAbstraction abstraction = new GoalAbstraction(stateGenerator.generateAbstractedState());
			int numAgents = token.getTokenList(WorldFile.AGENTS).size();
			world = new World((SGDomain)domain, jointReward, terminalFunction, stateGenerator, abstraction, numAgents);

			String description = token.getString(WorldFile.DESCRIPTION);
			world.setDescription(description);

		} catch (TokenCastException e) {
			e.printStackTrace();
			return null;
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

		int count = 0;
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
			List<GridGameServerToken> worldTokens = token.getTokenList(GridGameManager.WORLDS);
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
