package networking.server;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
	
	private List<GridGameServerToken> worldTokens;
	private final Map<String, World> worldLookup;
	private final Map<String, World> activeGameWorlds;
	private final Map<String, World> currentlyRunningWorlds;
	
	private final Map<String, Session> sessionLookup;
	private final Map<String, GameHandler> gameLookup;
	private final Map<String, Future<GameAnalysis>> futures;
	private final Map<String, List<GameHandler>> handlersAssociatedWithGames;
	private final ExecutorService gameExecutor;
	private final String gameDirectory;
	private final String analysisDirectory;
	private final GameMonitor monitor;
	private long threadIdCounter;
	
	public GridGameServer(String gameDirectory, String analysisDirectory) {
		this.threadIdCounter = 0;
		this.worldLookup = new HashMap<String, World>();
		this.sessionLookup = new HashMap<String, Session>();
		this.gameLookup = new HashMap<String, GameHandler>();
		this.gameExecutor = Executors.newCachedThreadPool();
		this.futures = new HashMap<String, Future<GameAnalysis>>();
		this.activeGameWorlds = new HashMap<String, World>();
		this.currentlyRunningWorlds = new HashMap<String, World>();
		this.handlersAssociatedWithGames = new HashMap<String, List<GameHandler>>();
		this.gameDirectory = gameDirectory;
		this.analysisDirectory = analysisDirectory;
		this.worldTokens = this.loadWorlds(null);
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
			GridGameServer.singleton = new GridGameServer(gameDirectory, outputDirectory);
		}
		return GridGameServer.singleton;
	}
	
	public String getNewCollectionID()
	{
		Random random = new Random();
		String id = Long.toString(random.nextLong());
		while (this.sessionLookup.containsKey(id) || this.gameLookup.containsKey(id))
		{
			id = Long.toString(random.nextLong());
		}
		return id;
	}
	
	private String getUniqueThreadId() {
		return Long.toString(this.threadIdCounter++);
	}
	
	public String getNewActiveWorldID() {
		long count = 0;
		String id = Long.toString(count++);
		while (this.activeGameWorlds.containsKey(id)) {
			id = Long.toString(count++);
		}
		return id;
	}

	public void onConnect(Session session) {
        System.out.println("Connect: " + session.getRemoteAddress().getAddress());
        try {
        	GridGameServerToken token = new GridGameServerToken();
        	String id = this.getNewCollectionID();
        	token.setString(CLIENT_ID, id);
        	this.addCurrentState(token);
        	token.setString(GameHandler.MSG_TYPE, HELLO_MESSAGE);
            session.getRemote().sendString(token.toJSONString());
            this.sessionLookup.put(id, session);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	public void addCurrentState(GridGameServerToken token) {
		token.setTokenList(WORLDS, this.worldTokens);
    	
    	Map<String, String> activeGames = new HashMap<String, String>();
    	for (Map.Entry<String, World> entry : this.activeGameWorlds.entrySet()) {
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
			Session session = this.sessionLookup.get(id);
			
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
				this.worldTokens = this.loadWorlds(response);
				break;
			}
			
			if (response.getError()) {
				return response;
			}
			
			GameHandler handler = this.gameLookup.get(id);
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
		for (Session session : this.sessionLookup.values()) {
			
			try {
				session.getRemote().sendString(token.toJSONString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void initializeGame(GridGameServerToken token, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameServer.WORLD_ID);
		
		if (this.worldLookup.containsKey(worldId)) {
			World world = this.worldLookup.get(worldId).copy();
			String activeId = this.getUniqueThreadId();
			this.activeGameWorlds.put(activeId, world);
			response.setString(STATUS, "Game " + activeId + " has been initialized");
			this.updateConnected();
		} else {
			response.setError(true);
			response.setString(WHY_ERROR, "The desired world id does not exist");
		}
	}
	
	private void joinGame(GridGameServerToken token, String clientId, Session session, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameServer.WORLD_ID);
		
		
		if (this.activeGameWorlds.containsKey(worldId)) {
			World world = this.activeGameWorlds.get(worldId);
			SGDomain domain = world.getDomain();
			GameHandler handler = new GameHandler(this, session, world, domain, worldId);
			List<GameHandler> handlers = this.handlersAssociatedWithGames.get(worldId);
			if (handlers == null) {
				handlers = Collections.synchronizedList(new ArrayList<GameHandler>());
				this.handlersAssociatedWithGames.put(worldId, handlers);
			}
			handlers.add(handler);
			this.gameLookup.put(clientId, handler);
			response.setString(STATUS, "Client " + clientId + " has been added to game " + worldId);
			this.updateConnected();
		} else {
			response.setError(true);
			response.setString(WHY_ERROR, "The desired world id does not exist");
		}
		
	}
	
	private void addAgent(GridGameServerToken token, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameServer.WORLD_ID);
		World world = this.activeGameWorlds.get(worldId);
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
		agent.joinWorld(world, agentType);
		this.updateConnected();
	}
	
	private void runGame(GridGameServerToken token, String clientId, GridGameServerToken response) throws TokenCastException {
		String activeId = token.getString(GridGameServer.WORLD_ID);
		
		if (!this.activeGameWorlds.containsKey(activeId)) {
			response.setError(true);
			response.setString(WHY_ERROR, "The desired active game id does not exist");
			return;
		}
		
		Future<GameAnalysis> future = this.futures.get(activeId);
		
		if (future == null) {
			World activeWorld = this.activeGameWorlds.remove(activeId);
			this.currentlyRunningWorlds.put(activeId, activeWorld);
			Callable<GameAnalysis> callable = this.generateCallable(activeWorld);
			future = this.gameExecutor.submit(callable);
			this.futures.put(activeId,  future);
			this.monitor.addFuture(future);
			response.setString(STATUS, "Game " + activeId + " has now been started with " + activeWorld.getRegisteredAgents().size() + " agents");
			this.updateConnected();
		}
		
	}
	
	private void removeGame(GridGameServerToken token) throws TokenCastException {
		String activeId = token.getString(GridGameServer.WORLD_ID);
		this.activeGameWorlds.remove(activeId);
		this.updateConnected();
	}
	
	public void closeGame(Future<GameAnalysis> future, GameAnalysis result) {
		String futureId = null;
		
		for (Map.Entry<String, Future<GameAnalysis>> entry : this.futures.entrySet()) {
			if (entry.getValue().equals(future)) {
				futureId = entry.getKey();
				break;
			}
		}
		
		if (futureId != null) {
			World world = this.currentlyRunningWorlds.remove(futureId);
			List<GameHandler> handlers = this.handlersAssociatedWithGames.remove(futureId);
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
		Future<GameAnalysis> future = this.futures.remove(futureId);
		World world = this.currentlyRunningWorlds.remove(futureId);
		List<GameHandler> handlers = this.handlersAssociatedWithGames.remove(futureId);
		
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
		this.gameLookup.remove(clientId);
	}
	
	private List<GridGameServerToken> loadWorlds(GridGameServerToken response){
		List<GridGameServerToken> tokens = GridGameWorldLoader.loadWorldTokens(this.gameDirectory);
		this.worldLookup.clear();
		this.sessionLookup.clear();
		this.gameLookup.clear();
		this.futures.clear();
		
		
		try {
			for (GridGameServerToken token : tokens) {
				String name = token.getString(WorldFile.LABEL);
				World world = GridGameWorldLoader.loadWorld(token);
				this.worldLookup.put(name, world);
			}
		} catch (TokenCastException e) {
			throw new RuntimeException(e);
		}
		
		this.activeGameWorlds.clear();
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
		MultiAgentVFPlanningAgent a0 = new MultiAgentVFPlanningAgent((SGDomain) domain, vi, new PolicyFromJointPolicy(ja0));
		AgentType agentType0 = new AgentType(GridGame.CLASSAGENT, domain.getObjectClass(GridGame.CLASSAGENT), domain.getSingleActions());
		return new MultiAgentVFPlanningAgent((SGDomain) domain, vi, new PolicyFromJointPolicy(ja0));
		
	}
	
	private Callable<GameAnalysis> generateCallable(final World world) {
		return new Callable<GameAnalysis>() {
			@Override
			public GameAnalysis call() throws Exception {
				return world.runGame();
			}
		};
	}
}
