package networking.server;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;

public class GridGameServer {
	private static GridGameServer singleton;
	public static final String MSGTYPE_STRING = "msgtype";
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
	public static final String MAVI_AGENT = "mavi";
	
	private final ExecutorService gameExecutor;
	private final String gameDirectory;
	private final String analysisDirectory;
	private final GameMonitor monitor;
	private final GridGameServerCollections collections;
	
	
	public GridGameServer(String gameDirectory, String analysisDirectory) {
		this.collections = new GridGameServerCollections();
		this.gameExecutor = Executors.newCachedThreadPool();
		this.gameDirectory = gameDirectory;
		this.analysisDirectory = analysisDirectory;
		
		this.collections.addWorldTokens(this.loadWorlds(null));
		
		this.monitor = new GameMonitor(this);
		this.gameExecutor.submit(this.monitor);
		
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
        try {
        	GridGameServerToken token = new GridGameServerToken();
        	String id = this.collections.getNewCollectionID();
        	token.setString(CLIENT_ID, id);
        	this.addCurrentState(token);
        	token.setString(GameHandler.MSG_TYPE, HELLO_MESSAGE);
            session.getRemote().sendString(token.toJSONString());
            this.collections.addSession(id, session);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	public void onWebSocketClose(Session session) {
		this.collections.removeSession(session);
	}
	
	public void addCurrentState(GridGameServerToken token) {
		List<GridGameServerToken> worldTokens = this.collections.getWorldTokens();
		token.setTokenList(WORLDS, worldTokens);
    	
    	Map<String, String> activeGames = new HashMap<String, String>();
    	Map<String, World> activeGameWorlds = this.collections.getActiveWorlds();
    	for (Map.Entry<String, World> entry : activeGameWorlds.entrySet()) {
    		World world = entry.getValue();
    		activeGames.put(entry.getKey(), world.toString() + " " + world.getRegisteredAgents().size() + " registered agents");
    	}
    	token.setObject(ACTIVE, activeGames);
    	
	}

	public GridGameServerToken onMessage(GridGameServerToken token)
	{		
		GridGameServerToken response = new GridGameServerToken();
		try {
			String id = token.getString(CLIENT_ID);
			Session session = this.collections.getSession(id);
			String msgType = token.getString(GameHandler.MSG_TYPE);
			
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
				response.setToken(GameHandler.HANDLER_RESPONSE, handler.onMessage(token));
			}
			
			if (session != null) {
				session.getRemote().sendString(response.toJSONString());
			}
			
		} catch (TokenCastException e) {
			response.setString(WHY_ERROR, "Message was not properly parsed");
			response.setError(true);
		} catch (IOException e) {
			e.printStackTrace();
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
			try {
				session.getRemote().sendString(token.toJSONString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void initializeGame(GridGameServerToken token, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameServer.WORLD_ID);
		World world = this.collections.getWorld(worldId);
		
		if (world != null) {
			World copy = world.copy();
			String activeId = this.collections.getUniqueThreadId();
			this.collections.addActiveWorld(activeId, copy);
			response.setString(STATUS, "Game " + activeId + " has been initialized");
			this.updateConnected();
		} else {
			response.setError(true);
			response.setString(WHY_ERROR, "The desired world id does not exist");
		}
	}
	
	private void joinGame(GridGameServerToken token, String clientId, Session session, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameServer.WORLD_ID);
		World world = this.collections.getActiveWorld(worldId);
		
		if (world != null) {
			SGDomain domain = world.getDomain();
			GameHandler handler = new GameHandler(this, session, world, domain, worldId);
			
			this.collections.addHandler(clientId, worldId, handler);
			response.setString(STATUS, "Client " + clientId + " has been added to game " + worldId);
			this.updateConnected();
		} else {
			response.setError(true);
			response.setString(WHY_ERROR, "The desired world id does not exist");
		}
		
	}
	
	private void addAgent(GridGameServerToken token, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameServer.WORLD_ID);
		World world = this.collections.getActiveWorld(worldId);
		String agentTypeStr = token.getString(GameHandler.AGENT_TYPE);
		
		
		if (world == null) {
			response.setError(true);
			response.setString(WHY_ERROR, "The desired world id does not exist");
			return;
		}
		
		Agent agent = this.getNewAgentForWorld(world, agentTypeStr);
		if (agent == null) {
			response.setError(true);
			response.setString(WHY_ERROR, "An agent of type " + agentTypeStr + " cannot be added to this game");
			return;
		}
		SGDomain domain = world.getDomain();
		AgentType agentType = new AgentType(agentTypeStr, domain.getObjectClass(GridGame.CLASSAGENT), domain.getSingleActions());
		
		synchronized(world) {
			agent.joinWorld(world, agentType);
		}
		
		this.updateConnected();
	}
	
	private void runGame(GridGameServerToken token, String clientId, GridGameServerToken response) throws TokenCastException {
		String activeId = token.getString(GridGameServer.WORLD_ID);
		World world = this.collections.getActiveWorld(activeId);
		
		if (world == null) {
			response.setError(true);
			response.setString(WHY_ERROR, "The desired active game id does not exist");
			return;
		}
		
		Future<GameAnalysis> future = this.collections.getFuture(activeId);
		
		if (future == null) {
			World activeWorld = this.collections.removeActiveWorld(activeId);
			this.collections.addRunningWorld(activeId, activeWorld);
			
			Callable<GameAnalysis> callable = this.generateCallable(activeWorld);
			this.submitCallable(activeId, callable);
			
			response.setString(STATUS, "Game " + activeId + " has now been started with " + activeWorld.getRegisteredAgents().size() + " agents");
			this.updateConnected();
		}
		
	}
	
	private void removeGame(GridGameServerToken token) throws TokenCastException {
		
		String activeId = token.getString(GridGameServer.WORLD_ID);
		this.collections.removeActiveWorld(activeId);
		this.updateConnected();
	}
	
	public void closeGame(Future<GameAnalysis> future, GameAnalysis result) {
		String futureId = this.collections.getFutureId(future);
		
		if (futureId != null) {
			World world = this.collections.removeRunningWorld(futureId);
			this.collections.removeFuture(futureId);
			List<GameHandler> handlers = this.collections.removeHandlers(futureId);
			String path = this.analysisDirectory + "/episode" + futureId;
			System.out.println("Writing to " + path);
			StateJSONParser parser = new StateJSONParser(world.getDomain());
			result.writeToFile(path, parser);
			
			for (GameHandler handler : handlers) {
				handler.shutdown();
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
	
	private Agent getNewAgentForWorld(World world, String agentType) {
		if (agentType.equalsIgnoreCase(GridGameServer.RANDOM_AGENT)) {
			return this.getNewRandomAgent();
		} else if (agentType.equalsIgnoreCase(GridGameServer.MAVI_AGENT)) {
			return this.getNewMAVIAgent(world);
		}
		return null;
	}
	
	private Agent getNewRandomAgent() {
		return new RandomAgent();
	}
	
	private Agent getNewMAVIAgent(World world) {
		
		SGDomain domain = world.getDomain();
		EGreedyMaxWellfare ja0 = new EGreedyMaxWellfare(0.0);
		ja0.setBreakTiesRandomly(false);
		JointReward rf = world.getRewardModel();
		StateHashFactory hashingFactory = new NameDependentStateHashFactory();
		MAValueIteration vi = 
				new MAValueIteration((SGDomain) domain, world.getActionModel(), rf, world.getTF(), 
						0.95, hashingFactory, 0., new MaxQ(), 0.00015, 50);
		return new MultiAgentVFPlanningAgent((SGDomain) domain, vi, new PolicyFromJointPolicy(ja0));
		
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
