package networking.server;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import networking.common.Token;
import networking.common.TokenCastException;
import networking.common.messages.WorldFile;

import org.eclipse.jetty.websocket.api.Session;

import Analysis.Analysis;
import burlap.behavior.stochasticgames.GameAnalysis;
import burlap.behavior.stochasticgames.agents.normlearning.ForeverNormLearningAgent;
import burlap.oomdp.core.states.State;
import burlap.oomdp.stochasticgames.SGAgent;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;




/**
 * Manages the initializing, configuring and running of games, as well as the connections to clients.
 * @author brawner
 *
 */
public class GridGameManager {
	private static GridGameManager singleton;
	public static final String MSG_TYPE = "msg_type";
	public static final String CLIENT_ID = "client_id";
	public static final String WORLD_ID = "world_id";
	public static final String URL_ID = "url_id";
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
	public static final String QLEARNER_AGENT = "qlearner";
	public static final String HUMAN_AGENT = "human";
	public static final String EXP_NAME = "exp_name";
	public static final String 	AGENT_NAME = "agent_name";
	public static final String REACTION_TIME = "reaction_time";
	public static final String ACTION_NUMBER = "action_number";
	public static final String GAME_NUMBER = "game_number";
	public static final String IS_READY = "is_ready";
	public static final String COMMA_DELIMITER = ",";
	public static final String CLOSED_PLAYER_LEAVING = "closed_by_player_leaving";
	public static final String DEBUG = "debug";
	public static final String OTHER_VARS = "other_vars";
	public static final String NORM_LEARNING_AGENT = "norm_learning";
	public static final String NORM_LEARNING_AGENT_IGNORE = "norm_learning_ignore";
	public static final String CONTINUOUS_NORM_LEARNING = "continuous_norm_learning";
	public static final String EXPLORING_NORM_LEARNING = "exploring_norm_learning";
	public static final String TEAM_EXPLORING_NORM_LEARNING = "team_exploring_norm_learning";
	public static final String TEAM_EXPLORING_NORM_LEARNING_IGNORE = "team_exploring_norm_learning_ignore";
	public static final String UNIFORM_EXPLORING_NORM_LEARNING = "uniform_exploring_norm_learning";
	public static final String WAIT_NORM_LEARNING = "wait_norm_learning";
	
	public static final List<String> ALLOWED_AGENTS = 
			Arrays.asList(GridGameManager.QLEARNER_AGENT, 
						  GridGameManager.COOPERATIVE_AGENT, 
						  GridGameManager.RANDOM_AGENT, 
						  GridGameManager.HUMAN_AGENT,
						  GridGameManager.NORM_LEARNING_AGENT,
						  GridGameManager.NORM_LEARNING_AGENT_IGNORE,
						  GridGameManager.CONTINUOUS_NORM_LEARNING,
						  GridGameManager.EXPLORING_NORM_LEARNING,
						  GridGameManager.WAIT_NORM_LEARNING,
						  GridGameManager.UNIFORM_EXPLORING_NORM_LEARNING,
						  GridGameManager.TEAM_EXPLORING_NORM_LEARNING,
						  GridGameManager.TEAM_EXPLORING_NORM_LEARNING_IGNORE);
	
	public static final List<String> REPEATED_AGENTS = 
			Arrays.asList(GridGameManager.NORM_LEARNING_AGENT,
						  GridGameManager.NORM_LEARNING_AGENT_IGNORE,
						  GridGameManager.EXPLORING_NORM_LEARNING,
						  GridGameManager.UNIFORM_EXPLORING_NORM_LEARNING,
						  GridGameManager.TEAM_EXPLORING_NORM_LEARNING,
						  GridGameManager.TEAM_EXPLORING_NORM_LEARNING_IGNORE,
						  GridGameManager.CONTINUOUS_NORM_LEARNING,
						  GridGameManager.WAIT_NORM_LEARNING);
	
	public static final List<String> FOREVER_AGENTS = 
			Arrays.asList(GridGameManager.CONTINUOUS_NORM_LEARNING);
	
	
	public HashMap<String,List<String>> gameTypesForIds = new HashMap<String,List< String>>();

	//add a logging string here that is pushed at end of game with episode data??


	/**
	 * The Executor service which runs the games in threads
	 */
	private final ExecutorService gameExecutor;


	private final String gameDirectory;
	private final String analysisDirectory;
	private final String summariesDirectory;
	private final String experimentDirectory;

	/**
	 * The game monitor constantly polls the running worlds to find which ones finish, and report their completion
	 * to the manager
	 */
	private final GameMonitor monitor;
	private Future<Boolean> monitorFuture;

	/**
	 * All collections that need to be synchronized and protected from multi-threaded interactions should be kept here
	 */
	private final GridGameServerCollections collections;

	/**
	 * Initializes a new manager. Requires a game files directory and a results directory
	 * @param gameDirectory
	 * @param analysisDirectory
	 */
	private GridGameManager(String gameDirectory, String analysisDirectory, String summariesDirectory, String experimentDirectory) {
		this.collections = new GridGameServerCollections();
		this.gameExecutor = Executors.newCachedThreadPool();
		this.gameDirectory = gameDirectory;
		this.analysisDirectory = analysisDirectory;
		this.summariesDirectory = summariesDirectory;
		this.experimentDirectory = experimentDirectory;
		this.collections.addWorldTokens(this.loadWorlds(null));
		this.monitor = new GameMonitor(this);
		this.monitorFuture = this.gameExecutor.submit(this.monitor);

	}

	/** 
	 * Connect to the grid game manager. Any call to this method must come after the connect method below.
	 * @return
	 */
	public static GridGameManager connect() {
		if (GridGameManager.singleton == null) {
			throw new RuntimeException("The grid game server was not properly initialized");
		}
		return GridGameManager.singleton;
	}

	/**
	 * Connect to the grid game manager with a game files directory and results directory. If it has already been initialized, it 
	 * will just pass that instance.
	 * @param gameDirectory
	 * @param outputDirectory
	 * @return
	 */
	public static GridGameManager connect(String gameDirectory, String outputDirectory, String summariesDirectory, String experimentDirectory) {
		if (GridGameManager.singleton == null) {
			GridGameManager singleton = new GridGameManager(gameDirectory, outputDirectory, summariesDirectory, experimentDirectory);
			GridGameManager.singleton = singleton;
		}
		return GridGameManager.singleton;
	}

	/**
	 * Run a game and monitor its progress
	 * @param id
	 * @param callable
	 */
	public void submitCallable(String id, Callable<GameAnalysis> callable) {
		Future<GameAnalysis> future = this.gameExecutor.submit(callable);
		this.collections.addFuture(id, future);
		this.monitor.addFuture(future);
	}


	/**
	 * Called by the server whenever a new client connects. It also sends a initialization message to the client with the worlds available, 
	 * configurable games.
	 * @param session
	 */
	public void onConnect(Session session) {
		System.out.println("Connect: " + session.getRemoteAddress().getAddress());

		String id = this.collections.getNewCollectionID();

		GridGameServerToken token = this.constructConnectionToken(id);
		session.getRemote().sendStringByFuture(token.toJSONString());

		this.collections.addSession(id, session); 
	}

	/** 
	 * Constructs the message for a connection event. Includes the available worlds, active games, and allowable agents
	 * @param id
	 * @return
	 */
	private GridGameServerToken constructConnectionToken(String id) {
		GridGameServerToken token = new GridGameServerToken();
		token.setString(CLIENT_ID, id);
		this.addCurrentState(token);
		token.setString(GridGameManager.MSG_TYPE, HELLO_MESSAGE);
		token.setStringList(WorldFile.AGENTS, ALLOWED_AGENTS);
		return token;
	}

	/**
	 * Notified when a client closes the method. Games probably should be stopped when their session closes
	 * @param session
	 */
	public void onWebSocketClose(Session session) {
		String clientId = this.collections.getClientId(session);
		if (clientId != null) {
			this.processClientExiting(clientId);
		}		
	}
	
	private void processClientExiting(String clientId) {
		String activeId = this.collections.getClientsGame(clientId);
		if (activeId == null) {
			throw new RuntimeException("Your active Id is null");
		}
		GameHandler exitedHandler = this.collections.getHandler(clientId);
		
		ExperimentConfiguration configuration = this.collections.getConfiguration(activeId);
		MatchConfiguration matchConfig = configuration.getCurrentMatch();
		Collection<GameHandler> handlers = matchConfig.getHandlerLookup().values();
		handlers.remove(exitedHandler);
		this.informExperimentOver(configuration, activeId, handlers, true);
		
		this.collections.removeSession(clientId);
		this.collections.removeHandlers(activeId);
		this.collections.removeConfiguration(activeId);
		this.collections.removeRunningWorld(activeId);
		Future<GameAnalysis> future = this.collections.removeFuture(activeId);
		this.monitor.removeFuture(future);
	}

	/**
	 * Adds the worlds and configurable games to the message token.
	 * @param token
	 */
	private void addCurrentState(GridGameServerToken token) {
		List<GridGameServerToken> worldTokens = this.collections.getWorldTokens();
		token.setTokenList(WORLDS, worldTokens);

		Map<String, ExperimentConfiguration> configurations = this.collections.getConfigurations();

		List<GridGameServerToken> activeGames = new ArrayList<GridGameServerToken>();
		for (Map.Entry<String, ExperimentConfiguration> entry : configurations.entrySet()) {
			ExperimentConfiguration config = entry.getValue();
			if (config.isClosed() || config.getNumMatches() != 1) {
				continue;
			}

			MatchConfiguration matchConfig = config.getCurrentMatch();
			World world = matchConfig.getBaseWorld();
			GridGameServerToken gameToken = new GridGameServerToken();
			gameToken.setString(WorldFile.LABEL, entry.getKey());

			gameToken.setString(WorldFile.DESCRIPTION, world.toString() + " " + matchConfig.getNumberAgents() + " registered agents");

			Map<String, String> agentDescriptions = matchConfig.getAgentTypes();
			gameToken.setObject(WorldFile.AGENTS, agentDescriptions);
			gameToken.setInt(WorldFile.NUM_AGENTS, world.getMaximumAgentsCanJoin());
			activeGames.add(gameToken);

		}
		token.setObject(ACTIVE, activeGames);

	}

	/**
	 * Whenever a message is received by the server, it calls this method. This handles the message parsing, and the calls to
	 * the appropiate handling messages.
	 * @param token
	 * @return
	 */
	public GridGameServerToken onMessage(GridGameServerToken token)
	{		
		GridGameServerToken response = new GridGameServerToken();
		try {
			String id = token.getString(CLIENT_ID);
			Session session = this.collections.getSession(id);
			String msgType = token.getString(GridGameManager.MSG_TYPE);

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
				List<GridGameServerToken> tokens = 
					new ArrayList<GridGameServerToken>(this.loadWorlds(response));
				this.collections.addWorldTokens(tokens);
				break;
			case GameHandler.RUN_URL_GAME:
				this.runUrlGame(token,id,session,response);
				break;
			}

			if (response.getError()) {
				return response;
			}

			GameHandler handler = this.collections.getHandler(id);
			if (handler != null) {
				handler.onMessage(token, response);
			}

			if (session != null && session.isOpen()) {
				session.getRemote().sendStringByFuture(response.toJSONString());
			}

		} catch (TokenCastException e) {
			response.setString(WHY_ERROR, "Message was not properly parsed");
			response.setError(true);
		} 
		return response;
	}

	

	/**
	 * Whenever a new game is initialized, or configured the clients should be updated so they can see and modify it.
	 */
	private void updateConnected() {
		GridGameServerToken token = new GridGameServerToken();
		this.addCurrentState(token);
		this.broadcastMessage(token);
	}

	/**
	 * Broadcasts a message to all connected clients.
	 * @param token
	 */
	private void broadcastMessage(GridGameServerToken token) {
		List<Session> sessions = this.collections.getSessions();

		for (Session session : sessions) {
			if (session.isOpen()) {
				session.getRemote().sendStringByFuture(token.toJSONString());
			}
		}
	}

	/**
	 * Parses the message token and initializes a game with a specified world.
	 * @param token
	 * @param response
	 * @throws TokenCastException
	 */
	private void initializeGame(GridGameServerToken token, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameManager.WORLD_ID);
		World world = this.collections.getWorld(worldId);
		if (world != null) {
			MatchConfiguration matchConfig = new MatchConfiguration(world.copy());
			ExperimentConfiguration expConfiguration = new ExperimentConfiguration();
			expConfiguration.addMatchConfig(matchConfig);
			String activeId = this.collections.getUniqueThreadId();
			this.collections.addConfiguration(activeId, expConfiguration);
			//this.collections.addConfiguration(worldId, config);
			response.setString(STATUS, "Game " + worldId + " has been initialized");
			this.updateConnected();
		} else {
			response.setError(true);
			response.setString(WHY_ERROR, "Init: The desired world id does not exist");
			return;
		}
	}

	/**
	 * Parses the message token and connects a client with their desired game
	 * @param token
	 * @param clientId
	 * @param session
	 * @param response
	 * @throws TokenCastException
	 */
	private void joinGame(GridGameServerToken token, String clientId, Session session, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameManager.WORLD_ID);
		ExperimentConfiguration configuration = this.collections.getConfiguration(worldId);
		if (configuration == null) {
			response.setError(true);
			response.setString(WHY_ERROR, "Join: The desired world id does not exist");
			this.updateConnected();
			return;
		}
		
		if (configuration.getNumMatches() != 1) {
			response.setError(true);
			response.setString(WHY_ERROR, "Join: Cannot join a configuration with more than one match");
			this.updateConnected();
			return;
		}
		
		MatchConfiguration matchConfiguration = configuration.getAllMatches().get(0);
		GameHandler handler = new GameHandler(this, session, worldId, "mememe");

		this.collections.addHandler(clientId, worldId, handler);
		matchConfiguration.addHandler(clientId, handler);
		response.setString(STATUS, "Client " + clientId + " has been added to game " + worldId);

		World baseWorld = matchConfiguration.getWorldWithAgents();
		response.setString(GridGameManager.WORLD_TYPE, baseWorld.toString());
		SGDomain domain = baseWorld.getDomain();
		State startState = baseWorld.startingState();
		String agentName = matchConfiguration.getAgentName(handler);
		response.setString(GameHandler.AGENT, agentName);
		response.setState(GameHandler.STATE, startState, domain);
		this.updateConnected();
			
	}

	/** 
	 * Adds an agent to a configurable game. Only agents of type human/random/cooperative are allowed currently.
	 * @param token
	 * @param response
	 * @throws TokenCastException
	 */
	private void addAgent(GridGameServerToken token, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameManager.WORLD_ID);
		ExperimentConfiguration configuration = this.collections.getConfiguration(worldId);
		String agentTypeStr = token.getString(GameHandler.AGENT_TYPE);


		if (configuration == null) {
			response.setError(true);
			response.setString(WHY_ERROR, "Add agent: The desired world id does not exist");
			return;
		}
		
		if (configuration.getNumMatches() != 1) {
			response.setError(true);
			response.setString(WHY_ERROR, "Join: Cannot add an agent to a configuration with more than one match");
			this.updateConnected();
			return;
		}
		
		MatchConfiguration matchConfiguration = configuration.getAllMatches().get(0);

		boolean isValidAgent = this.isValidAgent(worldId, agentTypeStr);
		if (!isValidAgent) {
			response.setError(true);
			response.setString(WHY_ERROR, "An agent of type " + agentTypeStr + " cannot be added to this game");
			return;
		}
		matchConfiguration.addAgentType(agentTypeStr);
		this.updateConnected();
	}

	/**
	 * Submit a configuration for a configurable game. One configured, a game cannot be reconfigured.
	 * @param token
	 * @param response
	 * @throws TokenCastException
	 */
	private void configGame(GridGameServerToken token, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameManager.WORLD_ID);
		ExperimentConfiguration configuration = this.collections.getConfiguration(worldId);

		if (configuration == null) {
			response.setError(true);
			response.setString(WHY_ERROR, "Config: The desired world id does not exist");
			return;
		}
		
		if (configuration.getNumMatches() != 1) {
			response.setError(true);
			response.setString(WHY_ERROR, "Join: Cannot config a configuration with more than one match");
			this.updateConnected();
			return;
		}
		
		MatchConfiguration matchConfiguration = configuration.getAllMatches().get(0);

		List<String> agentTypes = token.getStringList(WorldFile.AGENTS);

		for (String agentTypeStr : agentTypes) {
			boolean isValidAgent = this.isValidAgent(worldId, agentTypeStr);

			if (!isValidAgent) {
				response.setError(true);
				response.setString(WHY_ERROR, "An agent of type " + agentTypeStr + " cannot be added to this game");
				return;
			}
			matchConfiguration.addAgentType(agentTypeStr);
		}

		this.updateConnected();

	}

	/**
	 * Checks if an agentType can be added to this world. Right now, it just limits the agent types to Cooperative,Random, and Human.
	 * @param worldId
	 * @param agentTypeStr
	 * @return
	 */
	private boolean isValidAgent(String worldId, String agentTypeStr) {

		agentTypeStr = agentTypeStr.toLowerCase();
		return ALLOWED_AGENTS.contains(agentTypeStr);
	}
	
	private boolean isRepeatedAgent(String worldId, String agentTypeStr) {
		return REPEATED_AGENTS.contains(agentTypeStr);
	}
	
	private boolean isForeverAgent(String worldId, String agentTypeStr) {
		return FOREVER_AGENTS.contains(agentTypeStr);
	}
	

	/**
	 * Takes a configured world and runs it. If the configuration hasn't been fully configured, it won't run the game.
	 * @param token
	 * @param clientId
	 * @param response
	 * @throws TokenCastException
	 */
	private void runGame(GridGameServerToken token, String clientId, GridGameServerToken response) throws TokenCastException {
		String activeId = token.getString(GridGameManager.WORLD_ID);
		ExperimentConfiguration configuration = this.collections.getConfiguration(activeId);

		if (configuration == null) {
			response.setError(true);
			response.setString(WHY_ERROR, "Run: The desired active game id does not exist");
			return;
		}

		if (!configuration.isFullyConfigured()) {
			response.setError(true);
			response.setString(WHY_ERROR, "This game has not been fully connected, or it is awaiting humans to join");
			return;
		}

		Future<GameAnalysis> future = this.collections.getFuture(activeId);

		if (future == null) {
			this.runGame(configuration, activeId, response);
		}

	}

	/**
	 * Runs a game from a configuration. This may be called multiple times in a configurations life, as it can be restarted.
	 * @param configuration
	 * @param id
	 * @param response
	 */
	private void runGame(ExperimentConfiguration configuration, String activeId, GridGameServerToken response) {
		try {
			if (this.monitorFuture.get(0, TimeUnit.SECONDS) != null) {
				System.err.println("Game monitor has exited, restarting");
				this.monitorFuture = this.gameExecutor.submit(this.monitor);
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
		}
		
		MatchConfiguration matchConfig = configuration.getCurrentMatch();
		matchConfig.close();
		matchConfig.incrementIterations();

		World world = matchConfig.getWorldWithAgents();

		this.collections.addRunningWorld(activeId, world);

		Callable<GameAnalysis> callable = this.generateCallable(world, matchConfig.getMaxTurns());
		this.submitCallable(activeId, callable);
		response.setString(STATUS, "Game " + activeId + " has now been started with " + world.getRegisteredAgents().size() + " agents");
	}

	/**
	 * Restarts a game when it has finished and runs it.
	 * @param configuration
	 * @param activeId
	 * @param handlers
	 */
	private void restartGame(ExperimentConfiguration configuration, String activeId, Collection<GameHandler> handlers) {
		System.out.println("Game " + activeId + ": Restarting game: ");
		GridGameServerToken message = new GridGameServerToken();
		this.runGame(configuration, activeId, message);
		for (GameHandler handler : handlers) {
			handler.updateClient(message);
		}
	}

	/**
	 * Removes a configurable game from the servers list.
	 * @param token
	 * @throws TokenCastException
	 */
	private void removeGame(GridGameServerToken token) throws TokenCastException {

		String activeId = token.getString(GridGameManager.WORLD_ID);
		this.collections.removeConfiguration(activeId);
		this.updateConnected();
	}

	private void runUrlGame(GridGameServerToken token, String clientId, Session session,
			GridGameServerToken response) throws TokenCastException{
		//if a game of the type exists, join that game

		String worldId = token.getString(GridGameManager.WORLD_ID);
		String experimentType = token.getString(EXP_NAME);
		if (worldId == null && experimentType == null) {
			response.setError(true);
			response.setString(WHY_ERROR, "The world or experiment must be specified");
			return;
		}
		
		String turkId = token.getString(GridGameManager.URL_ID);
		String agentTypeStr = token.getString(GameHandler.AGENT_TYPE);
		GridGameServerToken otherVars = token.getToken(OTHER_VARS);
		List<String> agentTypes = token.getStringList(WorldFile.AGENTS);

		Boolean runDebug = false;
		if (otherVars != null) {
			String runDebugStr = otherVars.getString(GridGameManager.DEBUG);
			runDebug = (runDebugStr == null) ? false : Boolean.parseBoolean(runDebugStr);
		}
		
		if (experimentType != null) {
			agentTypes = new ArrayList<String>();
			agentTypes.add(GridGameManager.HUMAN_AGENT);
			List<String> expParams = this.loadExperimentType(experimentType, response);
			if (response.getError()) {
				return;
			}
			worldId = expParams.get(0);
			agentTypes.add(expParams.get(1));
		}
		
		System.out.println("Client " + clientId + " with turk id: " + turkId + " is starting a game. " +
				"With world: " + worldId + " experiment type: " + experimentType + " and agent type: " + agentTypeStr);
		this.runGame(worldId, turkId, agentTypeStr, agentTypes, runDebug, clientId, session, response);
	}
	
	
	private void runGame(String worldId, String turkId, String agentTypeStr, 
			List<String> agentTypes, boolean runDebug,
			String clientId, Session session,
			GridGameServerToken response) {

		World world = this.collections.getWorld(worldId);
		if (world == null) {
			response.setError(true);
			response.setString(WHY_ERROR, "The desired world id does not exist");
			return;
		}

		ExperimentConfiguration configuration = null;
		String activeId = null;

		boolean initGame = true;

		Map<String,ExperimentConfiguration> configs = collections.getConfigurations();
		for (Map.Entry<String, ExperimentConfiguration> entry : configs.entrySet()) {
			String id = entry.getKey();
			ExperimentConfiguration config = entry.getValue();
			System.out.println("GTFI size: "+gameTypesForIds.size());
			
			if (config.isFullyConfigured()) {
				continue;
			}
			
			if(gameTypesForIds.get(worldId).contains(id)){
				initGame = false;
				activeId = id;
				configuration = configs.get(id);
				break;
			}
		}

		if(initGame){
			configuration = new ExperimentConfiguration(world.copy());
			
			if (runDebug) {
				configuration.setMaxIterations(2);
			}
			
			List<String> gameTypes = gameTypesForIds.get(worldId);
			if(gameTypes == null){
				gameTypes = new ArrayList<String>();
				gameTypesForIds.put(worldId, gameTypes);

			}
			activeId = this.collections.getUniqueThreadId();
			gameTypes.add(activeId);
			this.collections.addConfiguration(activeId, configuration);
			response.setString(STATUS, "Game " + worldId + " has been initialized");

			int count = 1;
			for (String agentTypeToAdd : agentTypes) {
				boolean isValidAgent = this.isValidAgent(worldId, agentTypeToAdd);

				if (!isValidAgent) {
					response.setError(true);
					response.setString(WHY_ERROR, "An agent of type " + agentTypeToAdd + " cannot be added to this game");
					return;
				}
				
				if (this.isForeverAgent(worldId, agentTypeToAdd)) {
					ForeverNormLearningAgent agent =
							(ForeverNormLearningAgent)this.collections.getContinousLearningAgent(agentTypeToAdd, world);
					if (agent == null) {
						World baseWorld = configuration.getBaseWorld();
						agent = (ForeverNormLearningAgent)
								configuration.getNewAgentForWorld(baseWorld, agentTypeToAdd);
						this.collections.addContinuousLearningAgent(agentTypeToAdd, baseWorld, agent);
					} 
					configuration.addAgent(agent.copy());
				}else {
					boolean isRepeated = this.isRepeatedAgent(worldId, agentTypeToAdd);
					configuration.addAgentType(agentTypeToAdd, isRepeated);
				}
				
				System.out.println("Adding agent " + count + " to world " + worldId + " of type " + agentTypeToAdd);
				
				
				
			}
		}else{
			int count = configuration.getNumberAgents() + 1;
			System.out.println("Adding agent " + count + " to world " + worldId + " of type " + agentTypeStr);
			boolean isValidAgent = this.isValidAgent(worldId, agentTypeStr);
			if (!isValidAgent) {
				response.setError(true);
				response.setString(WHY_ERROR, "An agent of type " + agentTypeStr + " cannot be added to this game");
				return;
			}
			configuration.addAgentType(agentTypeStr);
		}

		String agentName = "unset";
		GameHandler handler = new GameHandler(this, session, worldId, turkId);

		this.collections.addHandler(clientId, activeId, handler);
		configuration.addHandler(clientId, handler);
		response.setString(STATUS, "Client " + clientId + " has been added to game " + activeId);

		World baseWorld = configuration.getWorldWithAgents();
		response.setString(GridGameManager.WORLD_TYPE, baseWorld.toString());
		SGDomain domain = baseWorld.getDomain();
		State startState = baseWorld.startingState();
		agentName = configuration.getAgentName(handler);
		response.setString(GameHandler.AGENT, agentName);
		response.setState(GameHandler.STATE, startState, domain);
		response.setString(GridGameManager.MSG_TYPE, GameHandler.INITIALIZE);
		response.setString(GridGameManager.IS_READY,"true");
		response.setString(GridGameManager.WORLD_ID, activeId);

		response.setString(GameHandler.RESULT, GameHandler.SUCCESS);
		
		try {
			FileWriter pw = new FileWriter(this.analysisDirectory +"/IDMap.csv", true);

			pw.append(clientId).append(",").append(turkId).append(",").append(agentName);
			pw.append(",").append(agentTypeStr).append(",").append(configuration.getUniqueGameId());
			pw.append("\n");
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("CID: "+clientId+" TID: "+turkId+"AN: "+agentName+" AT: "+agentTypeStr+" GID: "+configuration.getUniqueGameId());

		if (!configuration.isFullyConfigured()) {
			response.setString(GridGameManager.IS_READY,"false");
			return;
		}
		
		Future<GameAnalysis> future = this.collections.getFuture(activeId);
		//map from futureId to agent name and turk id and experiment name

		if (future == null) {
			System.out.println("Game " + activeId + ": starting game");
			this.runGame(configuration, activeId, response);
		}
		if (this.collections.getHandlersWithGame(activeId).size() == 0) {
			throw new RuntimeException("We've started very poorly here");
		}
	}
	
	private List<String> loadExperimentType(String experimentType, GridGameServerToken response) {
		Path fileName = Paths.get(this.experimentDirectory, experimentType + ".csv").toAbsolutePath();
		List<String> params = new ArrayList<String>();
		if (!Files.exists(fileName)) {
			response.setError(true);
			response.setString(WHY_ERROR, "The experiment " + experimentType + " does not exist on this server");
			return params;
		}
		
		BufferedReader reader;
		FileReader fileReader;
		try {
			fileReader = 
					new FileReader(fileName.toString());
		} catch (FileNotFoundException e) {
			throw new RuntimeException("The file " + fileName.toString() + " somehow does not exist");
		}

		String opponentType = "random";
		String worldId = "TwoAgentsHall_3by5_noWalls";

		try {
			String line = "";

			reader = new BufferedReader(fileReader);

			while ((line = reader.readLine()) != null) {
				//Get all tokens available in line
				String[] tokens = line.split(COMMA_DELIMITER);
				if (tokens.length >=2) {
					worldId = tokens[0];
					opponentType = tokens[1];
				}
			}
			reader.close();
		} catch (IOException e) {
			response.setError(true);
			response.setString(WHY_ERROR, "Reading the experiment " + experimentType + " file failed");
			return params;
		} 
		return Arrays.asList(worldId, opponentType);		
	}

	/**
	 * When the game monitor notices a game has finished, it attempts to close it or restart it. 
	 * @param future
	 * @param result
	 */
	public void processGameCompletion(Future<GameAnalysis> future, GameAnalysis result) {
		String futureId = this.collections.getFutureId(future);
		if (futureId == null) {
			System.err.println("Future " + future.toString() + " was not found in the collection");
			return;
		}
		
		System.out.println("Game " + futureId + ": processing for completion");
		Integer beforeSize = this.collections.getHandlersWithGame(futureId).size();
		
		if (beforeSize == 0) {
			throw new RuntimeException("We've lost them, Jim " + futureId);
		}
		
		ExperimentConfiguration configuration = this.collections.getConfiguration(futureId);
		Collection<GameHandler> handlers = configuration.getCurrentMatch().getHandlerLookup().values();
		String rt_path = analysisDirectory+"/"+configuration.getUniqueGameId()+"_reactionTimes_episode" + futureId+"_"+configuration.getGameNum()+".csv";
		try {
			FileWriter writer = new FileWriter(rt_path);

			for (GameHandler handler : handlers) {
				writer.write(handler.getActionRecord());
				handler.clearActionRecord();
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.collections.removeFuture(futureId);

		String path = this.analysisDirectory + "/"+configuration.getUniqueGameId()+"_trial" + futureId+"_match_0_round"+configuration.getGameNum();
		System.out.println("Game " + futureId + ": Writing game result to " + path);
		result.writeToFile(path);
		
		String names = "";
		for (GameHandler handler : handlers) {
			String participantId = handler.getParticipantId();
			if (participantId != null) {
				names += "_" + participantId ;
			}
		}
		String condensedPath = this.summariesDirectory + "/"+configuration.getUniqueGameId()+"_episode" + futureId + names + ".csv";
		System.out.println("Game " + futureId + ": Writing summarized game result to " + condensedPath);
		Analysis.writeGameToFile(configuration, result, condensedPath);

		//print map here
		//file name, agent names and url_client_ids, experiment name

		configuration = this.collections.getConfiguration(futureId);
		MatchConfiguration currentMatch = configuration.getCurrentMatch();
		if (!currentMatch.hasReachedMaxIterations())
		{
			this.restartGame(configuration, futureId, handlers);
			if (this.collections.getHandlersWithGame(futureId).size() != beforeSize) {
				throw new RuntimeException("These were not reset. Whoops");
			}
		} else if (configuration.hasNextMatch()) {
			configuration.advanceToNextMatch();
			this.restartGame(configuration, futureId, handlers);
			if (this.collections.getHandlersWithGame(futureId).size() != beforeSize) {
				throw new RuntimeException("These were not reset. Whoops");
			}
		} else {
			this.informExperimentOver(configuration, futureId, handlers, false);
			this.processForeverAgents(configuration);
			for (GameHandler handler : handlers) {
				this.collections.removeHandler(handler.getThreadId());	
			}
			handlers = this.collections.removeHandlers(futureId);
		}
		
	
	}

	private void informExperimentOver(ExperimentConfiguration configuration,
			String futureId, Collection<GameHandler> handlers, boolean closedByPlayerLeaving) {
		GridGameServerToken msg = new GridGameServerToken();
		
		msg.setString(GridGameManager.MSG_TYPE, GameHandler.EXPERIMENT_COMPLETED);
		msg.setBoolean(GridGameManager.CLOSED_PLAYER_LEAVING, closedByPlayerLeaving);
		
		for (GameHandler handler : handlers) {
			handler.updateClient(msg);
		}
	}
	
	private void processForeverAgents(ExperimentConfiguration configuration) {
		MatchConfiguration currentMatch = configuration.getCurrentMatch();
		List<SGAgent> agents = currentMatch.getRepeatedAgents();
		World world = currentMatch.getBaseWorld();
		
		for (SGAgent agent : agents) {
			if (agent instanceof ForeverNormLearningAgent) {
				ForeverNormLearningAgent forever = (ForeverNormLearningAgent)agent;
				ForeverNormLearningAgent baseForever = 
						(ForeverNormLearningAgent)this.collections.getContinousLearningAgent(CONTINUOUS_NORM_LEARNING, world);
				baseForever.addGamesFromAgent(forever);
			}
		}
	}

	/**
	 * Attempts to kill a currently running game.
	 * @param futureId
	 * @param force
	 */
	public void closeGame(String futureId, boolean force) {
		System.out.println("Game " + futureId + ": Attempting to shutdown game ");
		Future<GameAnalysis> future = this.collections.removeFuture(futureId);
		World world = this.collections.removeRunningWorld(futureId);
		List<GameHandler> handlers = this.collections.removeHandlers(futureId);

		if (future == null) {
			System.out.println("Game " + futureId + ": This thread does not currently seem to be running");
			return;
		}

		if (world == null) {
			System.out.println("Game " + futureId + ": This world does not exist");
			return;
		}

		if (!force) {
			try {
				GameAnalysis analysis = future.get();
				String path = this.analysisDirectory + "/episode" + futureId;
				analysis.writeToFile(path);


			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Game " + futureId + ": Using force to bring down the game");
			for (GameHandler handler : handlers) {
				handler.shutdown();
			}
		}

	}

	/**
	 * Removes a client from a configurable game.
	 * @param clientId
	 */
	private void exitGame(String clientId) {
		this.collections.removeHandler(clientId);
	}

	/**
	 * Loads all available worlds from this managers game directory. May be triggered during the server's running.
	 * @param response
	 * @return
	 */
	private List<GridGameServerToken> loadWorlds(GridGameServerToken response){
		List<GridGameServerToken> tokens = GridGameWorldLoader.loadWorldTokens(this.gameDirectory);

		this.collections.clearWorlds();

		try {
			for (Token token : tokens) {
				String name = token.getString(WorldFile.LABEL);
				World world = GridGameWorldLoader.loadWorld((GridGameServerToken)token);
				this.collections.addWorld(name, world);
			}
		} catch (TokenCastException e) {
			throw new RuntimeException(e);
		}

		if (response != null) {
			response.setTokenList(GridGameManager.WORLDS, tokens);
		}
		return tokens;
	}


	/**
	 * Creates a Callable object that can be run in a separated thread for this world object.
	 * @param world
	 * @return
	 */
	private Callable<GameAnalysis> generateCallable(final World world, final int maxIterations) {
		return new Callable<GameAnalysis>() {
			@Override
			public GameAnalysis call() throws Exception {
				GameAnalysis analysis = null;
				try {
					synchronized (world) {
						analysis = world.runGame(maxIterations);
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
