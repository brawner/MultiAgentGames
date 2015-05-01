package networking.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import networking.common.messages.WorldFile;
import networking.server.GridGameServer;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;

public class GridGameWorldLoader {

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
	
	private static GridGameServerToken loadText(String filename) {
		return GridGameServerToken.tokenFromFile(filename);
	}
	
	private static SGStateGenerator generateStateGenerator(GridGameServerToken fileToken, final SGDomain domain) throws TokenCastException {
		final Integer width = fileToken.getInt(WorldFile.WIDTH);
		final Integer height = fileToken.getInt(WorldFile.HEIGHT);
		List<GridGameServerToken> agentObjects = fileToken.getTokenList(WorldFile.AGENTS);
		List<GridGameServerToken> goalObjects = fileToken.getTokenList(WorldFile.GOALS);
		List<GridGameServerToken> horizontalWallObjects = fileToken.getTokenList(WorldFile.HORIZONTAL_WALLS);
		List<GridGameServerToken> verticalWallObjects = fileToken.getTokenList(WorldFile.VERTICAL_WALLS);
		
		if (agentObjects == null) {
			throw new WorldLoaderException("Agents are not properly specified");
		}
		if (goalObjects == null) {
			throw new WorldLoaderException("Goals are not properly specified");
		}
		
		final List<List<Integer>> agentPositions = new ArrayList<List<Integer>>();
		for (GridGameServerToken agentObj : agentObjects) {
			Integer startX = agentObj.getInt(GridGame.ATTX);
			Integer startY = agentObj.getInt(GridGame.ATTY);
			if (startX == null || startY == null) {
				throw new RuntimeException("Parsing file token failed\n" + fileToken.toJSONString());
			}
			agentPositions.add(Arrays.asList(startX, startY));
		}
		
		final List<List<Integer>> goalPositions = new ArrayList<List<Integer>>();
		for (GridGameServerToken goalObj : goalObjects) {
			Integer startX = goalObj.getInt(GridGame.ATTX);
			Integer startY = goalObj.getInt(GridGame.ATTY);
			if (startX == null || startY == null) {
				throw new RuntimeException("Parsing file token failed\n" + fileToken.toJSONString());
			}
			Integer goalType = goalObj.getInt(GridGame.ATTGT);
			if (goalType == null) {
				goalType = 0;
			}
			goalPositions.add(Arrays.asList(startX, startY, goalType));
		}
		
		final int numAgents = agentObjects.size();
		final int numGoals = goalObjects.size();
		
		final int numHorizontalWalls = (horizontalWallObjects == null) ? 2 : horizontalWallObjects.size() + 2;
		final int numVerticalWalls = (verticalWallObjects == null) ? 2 : verticalWallObjects.size() + 2;
		
		final List<List<Integer>> horizontalWallPositions = new ArrayList<List<Integer>>();
		if (horizontalWallObjects != null) {
			for (GridGameServerToken wallObject : horizontalWallObjects) {
				Integer startX = wallObject.getInt(GridGame.ATTE1);
				Integer endX = wallObject.getInt(GridGame.ATTE2);
				Integer startY = wallObject.getInt(GridGame.ATTP);
				Integer wallType = wallObject.getInt(GridGame.ATTWT);
				if (startX == null || endX == null || startY == null) {
					throw new RuntimeException("Parsing file token failed\n" + fileToken.toJSONString());
				}
				if (wallType == null) {
					wallType = 0;
				}
				horizontalWallPositions.add(Arrays.asList(startX, endX, startY, wallType));
			}
		}
		
		
		final List<List<Integer>> verticalWallPositions = new ArrayList<List<Integer>>();
		if (verticalWallObjects != null) {
			for (GridGameServerToken wallObject : verticalWallObjects) {
				Integer startX = wallObject.getInt(GridGame.ATTP);
				Integer startY = wallObject.getInt(GridGame.ATTE1);
				Integer endY = wallObject.getInt(GridGame.ATTE2);
				if (startX == null || startY == null || endY == null) {
					throw new RuntimeException("Parsing file token failed\n" + fileToken.toJSONString());
				}
				Integer wallType = wallObject.getInt(GridGame.ATTWT);
				if (wallType == null) {
					wallType = 0;
				}
				verticalWallPositions.add(Arrays.asList(startX, startY, endY, wallType));
			}
		}
		
		return new SGStateGenerator() {
			@Override
			public State generateState(List<Agent> agents) {
				State state = GridGame.getCleanState(domain, agents, numAgents, numGoals, numHorizontalWalls, numVerticalWalls, width, height);
				
				for (int i = 0; i < agentPositions.size(); i++) {
					List<Integer> positions = agentPositions.get(i);
					int x = positions.get(0);
					int y = positions.get(1);
					GridGame.setAgent(state, i, x, y, i);
				}
				
				for (int i = 0; i < goalPositions.size(); i++) {
					List<Integer> goal = goalPositions.get(i);
					int x = goal.get(0);
					int y = goal.get(1);
					int goalType = goal.get(2);
					GridGame.setGoal(state, i, x, y, goalType);
				}
				
				for (int i = 0 ; i < horizontalWallPositions.size(); i++) {
					List<Integer> wall = horizontalWallPositions.get(i);
					int x1 = wall.get(0);
					int x2 = wall.get(1);
					int y = wall.get(2);
					int wallType = wall.get(3);
					GridGame.setHorizontalWall(state, i + 2, y, x1, x2, wallType);
				}
				
				for (int i = 0 ; i < verticalWallPositions.size(); i++) {
					List<Integer> wall = verticalWallPositions.get(i);
					int x = wall.get(0);
					int y1 = wall.get(1);
					int y2 = wall.get(2);
					int wallType = wall.get(3);

					GridGame.setVerticalWall(state, i + 2, x, y1, y2, wallType);
				}
				
				return state;
			}
			
		};
	}
	
	public static World loadWorld(GridGameServerToken token) {
		GridGame gridGame = new GridGame();
		
		SGDomain domain = GridGameWorldLoader.generateDomain(gridGame);
		JointActionModel jointActionModel = GridGameWorldLoader.generateJointActionModel(domain);
		TerminalFunction terminalFunction = GridGameWorldLoader.generateTerminalFunction(domain);
		JointReward jointReward = GridGameWorldLoader.generateJointReward(domain);
		
		
		World world = null;
		try {
			SGStateGenerator stateGenerator = GridGameWorldLoader.generateStateGenerator(token, domain);
			world = new World((SGDomain)domain, jointActionModel, jointReward, terminalFunction, stateGenerator);
			
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
	
	public static List<GridGameServerToken> loadWorldTokens(String directory) {
		Enumeration<URL> urls;
		List<File> files = new ArrayList<File>();
		try {
			urls = GridGameWorldLoader.class.getClassLoader().getResources(directory);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		
		try {
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				File file = new File(url.toURI()); 
				files.addAll(Arrays.asList(file.listFiles()));
			} 
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
		
		List<GridGameServerToken> worlds = new ArrayList<GridGameServerToken>();
	    
		int count = 0;
		for (File file : files) {
			if (file.isFile()) {
				GridGameServerToken world = GridGameWorldLoader.loadText(file.getAbsolutePath());
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
