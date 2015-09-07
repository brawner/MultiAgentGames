package networking.server;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	public static final List<String> ALLOWED_AGENTS = Arrays.asList(GridGameManager.QLEARNER_AGENT, GridGameManager.COOPERATIVE_AGENT, GridGameManager.RANDOM_AGENT, GridGameManager.HUMAN_AGENT);

	public HashMap<String,ArrayList<String>> gameTypesForIds = new HashMap<String,ArrayList< String>>();

	//add a logging string here that is pushed at end of game with episode data??


	/**
	 * The Executor service which runs the games in threads
	 */
	private final ExecutorService gameExecutor;


	private final String gameDirectory;
	private final String analysisDirectory;

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
	private GridGameManager(String gameDirectory, String analysisDirectory) {
		this.collections = new GridGameServerCollections();
		this.gameExecutor = Executors.newCachedThreadPool();
		this.gameDirectory = gameDirectory;
		this.analysisDirectory = analysisDirectory;

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
	public static GridGameManager connect(String gameDirectory, String outputDirectory) {
		if (GridGameManager.singleton == null) {
			GridGameManager singleton = new GridGameManager(gameDirectory, outputDirectory);
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
		this.collections.removeSession(session);
	}

	/**
	 * Adds the worlds and configurable games to the message token.
	 * @param token
	 */
	private void addCurrentState(GridGameServerToken token) {
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
				this.collections.addWorldTokens(this.loadWorlds(response));
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

			if (session != null) {
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
			System.out.println("Broadcasting to: " + session.toString());
			session.getRemote().sendStringByFuture(token.toJSONString());
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
			GridGameConfiguration config = new GridGameConfiguration(world.copy());

			String activeId = this.collections.getUniqueThreadId();
			this.collections.addConfiguration(activeId, config);
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
		GridGameConfiguration configuration = this.collections.getConfiguration(worldId);

		if (configuration != null) {
			GameHandler handler = new GameHandler(this, session, worldId);

			this.collections.addHandler(clientId, worldId, handler);
			configuration.addHandler(clientId, handler);
			response.setString(STATUS, "Client " + clientId + " has been added to game " + worldId);

			World baseWorld = configuration.getWorldWithAgents();
			response.setString(GridGameManager.WORLD_TYPE, baseWorld.toString());
			SGDomain domain = baseWorld.getDomain();
			State startState = baseWorld.startingState();
			String agentName = configuration.getAgentName(handler);
			response.setString(GameHandler.AGENT, agentName);
			response.setState(GameHandler.STATE, startState, domain);
			this.updateConnected();
		} else {
			response.setError(true);
			response.setString(WHY_ERROR, "Join: The desired world id does not exist");
		}	this.updateConnected();
	}

	/** 
	 * Adds an agent to a configurable game. Only agents of type human/random/cooperative are allowed currently.
	 * @param token
	 * @param response
	 * @throws TokenCastException
	 */
	private void addAgent(GridGameServerToken token, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameManager.WORLD_ID);
		GridGameConfiguration configuration = this.collections.getConfiguration(worldId);
		String agentTypeStr = token.getString(GameHandler.AGENT_TYPE);


		if (configuration == null) {
			response.setError(true);
			response.setString(WHY_ERROR, "Add agent: The desired world id does not exist");
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

	/**
	 * Submit a configuration for a configurable game. One configured, a game cannot be reconfigured.
	 * @param token
	 * @param response
	 * @throws TokenCastException
	 */
	private void configGame(GridGameServerToken token, GridGameServerToken response) throws TokenCastException {
		String worldId = token.getString(GridGameManager.WORLD_ID);
		GridGameConfiguration configuration = this.collections.getConfiguration(worldId);

		if (configuration == null) {
			response.setError(true);
			response.setString(WHY_ERROR, "Config: The desired world id does not exist");
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

	/**
	 * Takes a configured world and runs it. If the configuration hasn't been fully configured, it won't run the game.
	 * @param token
	 * @param clientId
	 * @param response
	 * @throws TokenCastException
	 */
	private void runGame(GridGameServerToken token, String clientId, GridGameServerToken response) throws TokenCastException {
		String activeId = token.getString(GridGameManager.WORLD_ID);
		GridGameConfiguration configuration = this.collections.getConfiguration(activeId);

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
	private void runGame(GridGameConfiguration configuration, String id, GridGameServerToken response) {
		try {
			if (this.monitorFuture.get(0, TimeUnit.SECONDS) != null) {
				System.err.println("Game monitor has exited, restarting");
				this.monitorFuture = this.gameExecutor.submit(this.monitor);
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {

		}
		System.out.println("In RunGame at 1");
		configuration.close();
		configuration.incrementIterations();

		World world = configuration.getWorldWithAgents();

		this.collections.addRunningWorld(id, world);

		Callable<GameAnalysis> callable = this.generateCallable(world, configuration.getMaxTurns());
		this.submitCallable(id, callable);
		System.out.println("In RunGame at 2");
		response.setString(STATUS, "Game " + id + " has now been started with " + world.getRegisteredAgents().size() + " agents");
		this.updateConnected();
		System.out.println("In RunGame at 3");
	}

	/**
	 * Restarts a game when it has finished and runs it.
	 * @param configuration
	 * @param id
	 * @param handlers
	 */
	private void restartGame(GridGameConfiguration configuration, String id, List<GameHandler> handlers) {
		GridGameServerToken message = new GridGameServerToken();
		System.out.println("Trying to restart game 509 GGM");
		this.runGame(configuration, id, message);
		try {
			System.out.println("Msg: "+message.getString(MSG_TYPE));
		} catch (TokenCastException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		System.out.println("***Running URL Code***");

		//if a game of the type exists, join that game

		String turk_id = token.getString(GridGameManager.URL_ID);
		String agentTypeStr = token.getString(GameHandler.AGENT_TYPE);
		String worldId = token.getString(GridGameManager.WORLD_ID);
		System.out.println("worldId: "+worldId);
		//config_game message received
		List<String> agentTypes = token.getStringList(WorldFile.AGENTS);


		if(worldId == null){
			agentTypes = new ArrayList<String>();
			agentTypes.add(HUMAN_AGENT);
			agentTypeStr = HUMAN_AGENT;

			String fileName = "/home/betsy/cognitive_hierarchy/MultiAgentGames/"+token.getString(EXP_NAME)+".csv";
			// read these two strings from a human written json file here??


			BufferedReader fileReader = null;

			String oponentType = "random";
			worldId = "TwoAgentsHall_3by5_noWalls";

			try {

				//Create a new list of student to be filled by CSV file data


				String line = "";

				fileReader = new BufferedReader(new FileReader(fileName));

				while ((line = fileReader.readLine()) != null) {
					//Get all tokens available in line
					String[] tokens = line.split(COMMA_DELIMITER);
					if (tokens.length >=2) {
						worldId = tokens[0];
						oponentType = tokens[1];
					}
				}
			}catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//System.out.println("oponentType: "+oponentType);
			agentTypes.add(oponentType);
		}else{
			//System.out.println("Agents sm url: "+agentTypes.toString());
		}
		//System.out.println("Agent Types: "+agentTypes.toString());


		World world = this.collections.getWorld(worldId);

		GridGameConfiguration configuration = null;
		String activeId = null;

		boolean joinOnly = false;

		Map<String,GridGameConfiguration>configs = collections.getConfigurations();
		for(String id : configs.keySet()){
			System.out.println("GTFI size: "+gameTypesForIds.size());
			if(!configs.get(id).isFullyConfigured() && gameTypesForIds.get(worldId).contains(id)){ //&& check same type??){

				joinOnly = true;
				activeId = id;
				configuration = configs.get(id);
				break;
			}
		}



		if(!joinOnly){
			//init_game message received
			System.out.println("Setting up First Agent");
			if (world != null) {
				configuration = new GridGameConfiguration(world.copy());

				activeId = this.collections.getUniqueThreadId();
				if(!gameTypesForIds.containsKey(worldId)){
					gameTypesForIds.put(worldId, new ArrayList<String>());

				}
				gameTypesForIds.get(worldId).add(activeId);

				this.collections.addConfiguration(activeId, configuration);
				//this.collections.addConfiguration(worldId, configuration);
				response.setString(STATUS, "Game " + worldId + " has been initialized");
				this.updateConnected();
			} else {
				response.setError(true);
				response.setString(WHY_ERROR, "The desired world id does not exist");
				return;
			}


			for (String agentTypeToAdd : agentTypes) {
				System.out.println("Adding agent to WI: "+worldId+" of type ATS: "+agentTypeToAdd);
				boolean isValidAgent = this.isValidAgent(worldId, agentTypeToAdd);

				if (!isValidAgent) {
					response.setError(true);
					response.setString(WHY_ERROR, "An agent of type " + agentTypeToAdd + " cannot be added to this game");
					return;
				}
				System.out.println("Agent being added: "+agentTypeToAdd);
				configuration.addAgentType(agentTypeToAdd);


			}


		}else{
			//add agent to game
			System.out.println("Setting up Second Agent");

			if (configuration == null) {
				response.setError(true);
				response.setString(WHY_ERROR, "Add agent: The desired world id does not exist");
				return;
			}

			System.out.println("2ndAgent-- WI: "+worldId+" ATS: "+agentTypeStr);
			boolean isValidAgent = this.isValidAgent(worldId, agentTypeStr);
			if (!isValidAgent) {
				response.setError(true);
				response.setString(WHY_ERROR, "An agent of type " + agentTypeStr + " cannot be added to this game");
				return;
			}
			configuration.addAgentType(agentTypeStr);



		}

		//join_game message received
		String agentName = "unset";
		if (configuration != null) {
			GameHandler handler = new GameHandler(this, session, worldId);

			this.collections.addHandler(clientId, worldId, handler);
			configuration.addHandler(clientId, handler);
			response.setString(STATUS, "Client " + clientId + " has been added to game " + worldId);


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
			System.out.println("Made an initialization message");

			//this.updateConnected();
		}
		System.out.println("Before logging");
		try {
			FileWriter pw = new FileWriter(this.analysisDirectory +"/IDMap.csv", true);

			pw.append(clientId);
			pw.append(",");
			pw.append(turk_id);
			pw.append(",");
			pw.append(agentName);
			pw.append(",");
			pw.append(agentTypeStr);
			pw.append(",");
			pw.append(configuration.getUniqueGameId());
			pw.append("\n");

			pw.flush();

			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("CID: "+clientId+" TID: "+turk_id+"AN: "+agentName+" AT: "+agentTypeStr+" GID: "+configuration.getUniqueGameId());



		//run_game

		if (!configuration.isFullyConfigured()) {
			System.out.println("Not Fully Configured");
			response.setError(true);
			response.setString(WHY_ERROR, "This game has not been fully connected, or it is awaiting humans to join");
			response.setString(GridGameManager.IS_READY,"false");
			return;
		}
		//SEND BACK INITIALIZE MESSAGE STATE PAINTED HERE
		//System.out.println("Updating Connected from CID: "+clientId);
		//this.updateConnected();

		Future<GameAnalysis> future = this.collections.getFuture(activeId);
		String futureId = this.collections.getFutureId(future);
		//map from futureId to agent name and turk id and experiment name

		System.out.println("Future: "+future);
		if (future == null) {
			System.out.println("Before Running game");
			this.runGame(configuration, activeId, response);
			System.out.println("___***___****Should have run a game****___***___");
		}





	}

	/**
	 * When the game monitor notices a game has finished, it attempts to close it or restart it. 
	 * @param future
	 * @param result
	 */
	public void processGameCompletion(Future<GameAnalysis> future, GameAnalysis result) {
		String futureId = this.collections.getFutureId(future);
		System.out.println("_______________Processing Game Completion in GGM___________________");
		if (futureId != null) {

			GridGameConfiguration configuration = this.collections.getConfiguration(futureId);
			//configuration.getHandlers
			
			String rt_path = analysisDirectory+"/"+configuration.getUniqueGameId()+"_reactionTimes_episode" + futureId+"_"+configuration.getGameNum()+".csv";
			try {
				FileWriter writer = new FileWriter(rt_path);

				Map<String, GameHandler> handlers = configuration.getHandlerLookup();
				Set<String> c_ids = handlers.keySet();
				for(String c_id : c_ids){
					writer.write(handlers.get(c_id).getActionRecord());
					writer.flush();
					handlers.get(c_id).clearActionRecord();
				}

				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("Logging in GGM: futureId: "+futureId);
			World world = this.collections.removeRunningWorld(futureId);
			this.collections.removeFuture(futureId);

			String path = this.analysisDirectory + "/"+configuration.getUniqueGameId()+"_episode" + futureId+"_"+configuration.getGameNum();
			System.out.println("Writing to " + path);
			StateJSONParser parser = new StateJSONParser(world.getDomain());
			result.writeToFile(path, parser);



			//print map here
			//file name, agent names and url_client_ids, experiment name

			configuration = this.collections.getConfiguration(futureId);
			if (!configuration.hasReachedMaxIterations())
			{

				List<GameHandler> handlers = this.collections.getHandlersWithGame(futureId);
				System.out.println("Continuing game in GGM: "+handlers.size());
				this.restartGame(configuration, futureId, handlers);

			} else {

				List<GameHandler> handlers = this.collections.getHandlersWithGame(futureId);

				this.informExperimentOver(configuration, futureId, handlers);

				handlers = this.collections.removeHandlers(futureId);
				for (GameHandler handler : handlers) {
					handler.shutdown();
				}

			}
		} else {
			System.err.println("Future was not found in the collection");
		}
	}

	private void informExperimentOver(GridGameConfiguration configuration,
			String futureId, List<GameHandler> handlers) {
		GridGameServerToken msg = new GridGameServerToken();
		System.out.println("Trying to Inform Game is Over");



		msg.setString(GridGameManager.MSG_TYPE, GameHandler.EXPERIMENT_COMPLETED);
		this.broadcastMessage(msg);

		//for (GameHandler handler : handlers) {
		//handler.updateClient(msg);
		//}

	}

	/**
	 * Attempts to kill a currently running game.
	 * @param futureId
	 * @param force
	 */
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
			for (GridGameServerToken token : tokens) {
				String name = token.getString(WorldFile.LABEL);
				World world = GridGameWorldLoader.loadWorld(token);
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
