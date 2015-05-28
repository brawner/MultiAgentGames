package networking.server;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import networking.common.GridGameServerToken;
import networking.common.GridGameWorldLoader;
import networking.common.TokenCastException;
import networking.common.messages.WorldFile;

import org.eclipse.jetty.websocket.api.Session;

import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.stochasticgame.GameAnalysis;
import burlap.behavior.stochasticgame.PolicyFromJointPolicy;
import burlap.behavior.stochasticgame.agents.RandomAgent;
import burlap.behavior.stochasticgame.agents.mavf.MultiAgentVFPlanningAgent;
import burlap.behavior.stochasticgame.mavaluefunction.backupOperators.MaxQ;
import burlap.behavior.stochasticgame.mavaluefunction.policies.EGreedyMaxWellfare;
import burlap.behavior.stochasticgame.mavaluefunction.vfplanners.MAValueIteration;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;

public class GridGameServer {
	private static GridGameServer singleton;
	public static final String MSG_TYPE = "msg_type";
	public static final String CLIENT_ID = "client_id";
	public static final String WORLD_ID = "world_id";
	public static final String WORLDS = "worlds";
	public static final String ACTIVE = "active";
	public static final String WORLD_TYPE = "world_type";
	public static final String AGENT_TYPE = "agent_type";
	public static final String GAME_MESSAGE = "game_message";
	public static final String HELLO_MESSAGE = "hello";
	public static final String WHY_ERROR = "why_error";
	public static final String STATUS = "status";
	public static final String RANDOM_AGENT = "random";
	public static final String COOPERATIVE_AGENT = "cooperative";
	public static final String HUMAN_AGENT = "human";
	
	private final ExecutorService gameExecutor;
	private final String gameDirectory;
	private final String analysisDirectory;
	private final GameMonitor monitor;
	private Future<Boolean> monitorFuture;
	private final GridGameServerCollections collections;
	
	
	public GridGameServer(String gameDirectory, String analysisDirectory) {
		this.collections = new GridGameServerCollections();
		this.gameExecutor = Executors.newCachedThreadPool();
		this.gameDirectory = gameDirectory;
		this.analysisDirectory = analysisDirectory;
		
		this.collections.addWorldTokens(this.loadWorlds(null));
		
		this.monitor = new GameMonitor(this);
		this.monitorFuture = this.gameExecutor.submit(this.monitor);
		
	}
	
	public static GridGameServer connect() {
		if (GridGameServer.singleton == null) {
			throw new RuntimeException("The grid game server was not properly initialized");
		}
		return GridGameServer.singleton;
	}
	
	public static GridGameServer connect(String gameDirectory, String outputDirectory) {
		if (GridGameServer.singleton == null) {
			GridGameServer singleton = new GridGameServer(gameDirectory, outputDirectory);
			GridGameServer.singleton = singleton;
		}
		return GridGameServer.singleton;
	}
	
	public void submitCallable(String id, Callable<GameAnalysis> callable) {
		Future<GameAnalysis> future = this.gameExecutor.submit(callable);
		this.collections.addFuture(id, future);
		this.monitor.addFuture(future);
	}
	
	
	public void onConnect(Session session) {
        System.out.println("Connect: " + session.getRemoteAddress().getAddress());
        GridGameServerToken token = new GridGameServerToken();
    	String id = this.collections.getNewCollectionID();
    	token.setString(CLIENT_ID, id);
    	this.addCurrentState(token);
    	token.setString(GridGameServer.MSG_TYPE, HELLO_MESSAGE);
    	session.getRemote().sendStringByFuture(token.toJSONString());
        this.collections.addSession(id, session); 
    }
	
	public void onWebSocketClose(Session session) {
		this.collections.removeSession(session);
	}
	
	public void addCurrentState(GridGameServerToken token) {
		List<GridGameServerToken> worldTokens = this.collections.getWorldTokens();
		token.setTokenList(WORLDS, worldTokens);
    	
		
    	Map<String, GridGameConfiguration> configurations = this.collections.getConfigurations();
    	
    	List<GridGameServerToken> activeGames = new ArrayList<GridGameServerToken>();
    	for (Map.Entry<String, GridGameConfiguration> entry : configurations.entrySet()) {
    		GridGameConfiguration config = entry.getValue();
    		if (config.isClosed()) {
    			continue;
    		}
    		
    		
    		World world = config.getBaseWorld();
			GridGameServerToken gameToken = new GridGameServerToken();
    		gameToken.setString(WorldFile.LABEL, entry.getKey());
			
			gameToken.setString(WorldFile.DESCRIPTION, world.toString() + " " + config.getNumberAgents() + " registered agents");
    		
    		Map<String, String> agentDescriptions = config.getAgentTypes();
    		gameToken.setObject(WorldFile.AGENTS, agentDescriptions);
    		gameToken.setInt(WorldFile.NUM_AGENTS, world.getMaximumAgentsCanJoin());
    		activeGames.add(gameToken);
    		
    	}
    	token.setObject(ACTIVE, activeGames);
    	
	}

	public GridGameServerToken onMessage(GridGameServerToken token)
	{		
		GridGameServerToken response = new GridGameServerToken();
		try {
			String id = token.getString(CLIENT_ID);
			Session session = this.collections.getSession(id);
			String msgType = token.getString(GridGameServer.MSG_TYPE);
			
			if (msgType == null) {
				return response;
			}
			switch(msgType) {
			
			case GameHandler.INITIALIZE_GAME:
				this.initializeGame(token, response);
				break;
			case GameHandler.JOIN_GAME:
				this.joinGame(token, id, session, response);
				break;
			case GameHandler.ADD_AGENT:
				this.addAgent(token, response);
				break;
			case GameHandler.CONFIG_GAME:
				this.configGame(token, response);
				break;
			case GameHandler.RUN_GAME:
				this.runGame(token, id, response);
				break;
			case GameHandler.REMOVE_GAME:
				this.removeGame(token);
				break;
			case GameHandler.EXIT_GAME:
				this.exitGame(id);
				break;
			case GameHandler.LOAD_WORLDS:
				this.collections.addWorldTokens(this.loadWorlds(response));
				break;
			}
			
			if (response.getError()) {
				return response;
			}
			
			GameHandler handler = this.collections.getHandler(id);
			if (handler != null) {
				handler.onMessage(token, response);
			}
			
			if (session != null) {
				session.getRemote().sendStringByFuture(response.toJSONString());
			}
			
		} catch (TokenCastException e) {
			response.setString(WHY_ERROR, "Message was not properly parsed");
			response.setError(true);
		} 
		return response;
    }
	
	private void updateConnected() {
		GridGameServerToken token = new GridGameServerToken();
		this.addCurrentState(token);
		this.broadcastMessage(token);
	}
	
	private void broadcastMessage(GridGameServerToken token) {
		List<Session> sessions = this.collections.getSessions();
		
		for (Session session : sessions) {
			System.out.println("Broadcasting to: " + session.toString());
			session.getRemote().sendStringByFuture(token.toJSONString());
		}
	}
	
	private void initializeGame(GridGameServerToken token, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameServer.WORLD_ID);
		World world = this.collections.getWorld(worldId);
		if (world != null) {
			GridGameConfiguration config = new GridGameConfiguration(world.copy());
			
			String activeId = this.collections.getUniqueThreadId();
			this.collections.addConfiguration(activeId, config);
			response.setString(STATUS, "Game " + activeId + " has been initialized");
			this.updateConnected();
		} else {
			response.setError(true);
			response.setString(WHY_ERROR, "The desired world id does not exist");
		}
	}
	
	private void joinGame(GridGameServerToken token, String clientId, Session session, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameServer.WORLD_ID);
		GridGameConfiguration configuration = this.collections.getConfiguration(worldId);
		
		if (configuration != null) {
			GameHandler handler = new GameHandler(this, session, worldId);
			
			this.collections.addHandler(clientId, worldId, handler);
			configuration.addHandler(clientId, handler);
			response.setString(STATUS, "Client " + clientId + " has been added to game " + worldId);
			
			World baseWorld = configuration.getWorldWithAgents();
			response.setString(GridGameServer.WORLD_TYPE, baseWorld.toString());
			SGDomain domain = baseWorld.getDomain();
			State startState = baseWorld.startingState();
			String agentName = configuration.getAgentName(handler);
			response.setString(GameHandler.AGENT, agentName);
			response.setState(GameHandler.STATE, startState, domain);
			this.updateConnected();
		} else {
			response.setError(true);
			response.setString(WHY_ERROR, "The desired world id does not exist");
		}	
	}
	
	private void addAgent(GridGameServerToken token, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameServer.WORLD_ID);
		GridGameConfiguration configuration = this.collections.getConfiguration(worldId);
		String agentTypeStr = token.getString(GameHandler.AGENT_TYPE);
		
		
		if (configuration == null) {
			response.setError(true);
			response.setString(WHY_ERROR, "The desired world id does not exist");
			return;
		}
		
		boolean isValidAgent = this.isValidAgent(worldId, agentTypeStr);
		if (!isValidAgent) {
			response.setError(true);
			response.setString(WHY_ERROR, "An agent of type " + agentTypeStr + " cannot be added to this game");
			return;
		}
		configuration.addAgentType(agentTypeStr);
		this.updateConnected();
	}
	
	private void configGame(GridGameServerToken token, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameServer.WORLD_ID);
		GridGameConfiguration configuration = this.collections.getConfiguration(worldId);
		
		if (configuration == null) {
			response.setError(true);
			response.setString(WHY_ERROR, "The desired world id does not exist");
			return;
		}
		
		List<String> agentTypes = token.getStringList(WorldFile.AGENTS);
		
		for (String agentTypeStr : agentTypes) {
			boolean isValidAgent = this.isValidAgent(worldId, agentTypeStr);
			
			if (!isValidAgent) {
				response.setError(true);
				response.setString(WHY_ERROR, "An agent of type " + agentTypeStr + " cannot be added to this game");
				return;
			}
			configuration.addAgentType(agentTypeStr);
		}
		
		this.updateConnected();
		
	}
	
	private boolean isValidAgent(String worldId, String agentTypeStr) {
		
		agentTypeStr = agentTypeStr.toLowerCase();
		return Arrays.asList(GridGameServer.COOPERATIVE_AGENT, GridGameServer.RANDOM_AGENT, GridGameServer.HUMAN_AGENT).contains(agentTypeStr);
	}
	
	private void runGame(GridGameServerToken token, String clientId, GridGameServerToken response) throws TokenCastException {
		String activeId = token.getString(GridGameServer.WORLD_ID);
		GridGameConfiguration configuration = this.collections.getConfiguration(activeId);
		
		if (configuration == null) {
			response.setError(true);
			response.setString(WHY_ERROR, "The desired active game id does not exist");
			return;
		}
				
		Future<GameAnalysis> future = this.collections.getFuture(activeId);
		
		if (future == null) {
			this.runGame(configuration, activeId, response);
		}
		
	}
	
	private void runGame(GridGameConfiguration configuration, String id, GridGameServerToken response) {
		try {
			if (this.monitorFuture.get(0, TimeUnit.SECONDS) != null) {
				System.err.println("Game monitor has exited, restarting");
				this.monitorFuture = this.gameExecutor.submit(this.monitor);
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			
		}
		configuration.close();
		configuration.incrementIterations();
		
		World world = configuration.getWorldWithAgents();
		
		this.collections.addRunningWorld(id, world);
		
		Callable<GameAnalysis> callable = this.generateCallable(world);
		this.submitCallable(id, callable);
		
		response.setString(STATUS, "Game " + id + " has now been started with " + world.getRegisteredAgents().size() + " agents");
		this.updateConnected();
	}
	
	private void restartGame(GridGameConfiguration configuration, String id, List<GameHandler> handlers) {
		GridGameServerToken message = new GridGameServerToken();
		this.runGame(configuration, id, message);
		for (GameHandler handler : handlers) {
			handler.updateClient(message);
		}
	}
	
	private void removeGame(GridGameServerToken token) throws TokenCastException {
		
		String activeId = token.getString(GridGameServer.WORLD_ID);
		this.collections.removeConfiguration(activeId);
		this.updateConnected();
	}
	
	public void closeGame(Future<GameAnalysis> future, GameAnalysis result) {
		String futureId = this.collections.getFutureId(future);
		
		if (futureId != null) {
			World world = this.collections.removeRunningWorld(futureId);
			this.collections.removeFuture(futureId);
			String path = this.analysisDirectory + "/episode" + futureId;
			System.out.println("Writing to " + path);
			StateJSONParser parser = new StateJSONParser(world.getDomain());
			result.writeToFile(path, parser);
			
			GridGameConfiguration configuration = this.collections.getConfiguration(futureId);
			if (!configuration.hasReachedMaxIterations()) {
				System.out.println("Continuing game");
				List<GameHandler> handlers = this.collections.getHandlersWithGame(futureId);
				this.restartGame(configuration, futureId, handlers);
			} else {
				List<GameHandler> handlers = this.collections.removeHandlers(futureId);
				
				for (GameHandler handler : handlers) {
					handler.shutdown();
				}
			}
		} else {
			System.err.println("Future was not found in the collection");
		}
	}
	
	public void closeGame(String futureId, boolean force) {
		System.out.println("Attempting to shutdown game " + futureId);
		Future<GameAnalysis> future = this.collections.removeFuture(futureId);
		World world = this.collections.removeRunningWorld(futureId);
		List<GameHandler> handlers = this.collections.removeHandlers(futureId);
		
		if (future == null) {
			System.out.println("This thread does not currently seem to be running");
			return;
		}
		
		if (world == null) {
			System.out.println("This world does not exist");
			return;
		}
		
		if (!force) {
			try {
				GameAnalysis analysis = future.get();
				String path = this.analysisDirectory + "/episode" + futureId;
				StateJSONParser parser = new StateJSONParser(world.getDomain());
				analysis.writeToFile(path, parser);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Using force to bring down the game");
			for (GameHandler handler : handlers) {
				handler.shutdown();
			}
		}
		
	}
	
	private void exitGame(String clientId) {
		this.collections.removeHandler(clientId);
	}
	
	private List<GridGameServerToken> loadWorlds(GridGameServerToken response){
		List<GridGameServerToken> tokens = GridGameWorldLoader.loadWorldTokens(this.gameDirectory);
		
		this.collections.clearWorlds();
		
		try {
			for (GridGameServerToken token : tokens) {
				String name = token.getString(WorldFile.LABEL);
				World world = GridGameWorldLoader.loadWorld(token);
				this.collections.addWorld(name, world);
			}
		} catch (TokenCastException e) {
			throw new RuntimeException(e);
		}
		
		if (response != null) {
			response.setTokenList(GridGameServer.WORLDS, tokens);
		}
		return tokens;
	}
	
	
	
	private Callable<GameAnalysis> generateCallable(final World world) {
		return new Callable<GameAnalysis>() {
			@Override
			public GameAnalysis call() throws Exception {
				GameAnalysis analysis = null;
				try {
				synchronized (world) {
					analysis = world.runGame();
				}
				} catch (Exception e) {
					e.printStackTrace();
					throw e;
				}
				return analysis;
			}
		};
	}
}
