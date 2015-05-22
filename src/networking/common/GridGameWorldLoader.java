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
import networking.server.GridGameServer;
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

public class GridGameWorldLoader {
	
	
	private static GridGameServerToken loadText(String filename) {
		return GridGameServerToken.tokenFromFile(filename);
	}
	
	private static List<List<Integer>> selectGoals(List<List<Integer>> goals, List<Agent> agents, State state) {
		if (agents == null || agents.isEmpty()) {
			return goals;
		}
		List<List<Integer>> res = new ArrayList<List<Integer>>();
		Map<Agent, List<List<Integer>>> goalsByAgent = new HashMap<Agent, List<List<Integer>>>();
		for (List<Integer> goal : goals) {
			if (goal.get(2) == 0) {
				res.add(goal);
				continue;
			}
			
			for (Agent agent : agents) {
				ObjectInstance agentObj = state.getObject(agent.getAgentName());
				if (goal.get(2) == agentObj.getDiscValForAttribute(GridGame.ATTPN) + 1) {
					List<List<Integer>> agentsGoals = goalsByAgent.get(agent.getAgentName());
					if (agentsGoals == null) {
						agentsGoals = new ArrayList<List<Integer>>();
					}
					agentsGoals.add(goal);
				}
			}
		}
		
		for (Map.Entry<Agent, List<List<Integer>>> entry : goalsByAgent.entrySet()) {
			List<List<Integer>> list = entry.getValue();
			Collections.shuffle(list);
			res.add(list.get(0));
		}
		
		return res;
	}
	
	private static WorldLoadingStateGenerator generateStateGenerator(GridGameServerToken fileToken, final SGDomain domain, int goalsPerAgent) throws TokenCastException {
		return WorldLoadingStateGenerator.stateGenerator(fileToken, domain, goalsPerAgent);
	}
	
	public static World loadWorld(GridGameServerToken token) {
		GridGame gridGame = new GridGame();
		
		SGDomain domain = GridGameExtreme.generateDomain(gridGame);
		JointActionModel jointActionModel = GridGameExtreme.generateJointActionModel(domain);
		TerminalFunction terminalFunction = GridGameExtreme.generateTerminalFunction(domain);
		JointReward jointReward = GridGameExtreme.generateJointReward(domain);
		
		
		World world = null;
		try {
			Integer goalsPerAgent = token.getInt(WorldFile.GOALS_PER_AGENT);
			goalsPerAgent = (goalsPerAgent == null ) ? 0 : goalsPerAgent;
			
			WorldLoadingStateGenerator stateGenerator = GridGameWorldLoader.generateStateGenerator(token, domain, goalsPerAgent);
			StateAbstraction abstraction = new GoalAbstraction(stateGenerator.generateAbstractedState());
			
			world = new World((SGDomain)domain, jointActionModel, jointReward, terminalFunction, stateGenerator, abstraction);
			
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
				world.setString(WorldFile.LABEL, "world" + count++);
	    		worlds.add(world);
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
			List<GridGameServerToken> worldTokens = token.getTokenList(GridGameServer.WORLDS);
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
