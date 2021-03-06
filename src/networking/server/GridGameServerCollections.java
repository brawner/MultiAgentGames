package networking.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import networking.common.GridGameServerToken;

import org.eclipse.jetty.websocket.api.Session;

import burlap.behavior.stochasticgames.GameAnalysis;
import burlap.oomdp.stochasticgames.SGAgent;
import burlap.oomdp.stochasticgames.World;

/**
 * Handles all synchronous access to the collection objects required by the server. Everything included here should be thread safe, I hope.
 * @author brawner
 *
 */
public class GridGameServerCollections {

	private final List<GridGameServerToken> worldTokens;
	private final Map<String, World> worldLookup;
	//private final Map<String, World> activeGameWorlds;
	private final Map<String, World> currentlyRunningWorlds;
	
	private final Map<String, Session> sessionLookup;
	private final Map<String, GameHandler> gameLookup;
	private final Map<String, Future<GameAnalysis>> futures;
	private final Map<String, List<String>> handlersAssociatedWithGames;
	private final Map<String, String> clientToGameLookup;
	private final Map<String, ExperimentConfiguration> configurations;
	private final Map<String, List<ExperimentConfiguration>> experiments;
	private final Map<String, Map<String, SGAgent> > continousLearningAgents; // agent type, world id
	
	private final AtomicLong threadIdCounter;
	private final AtomicLong activeIdCounter;
	private final AtomicLong clientIdCounter;
	
	public GridGameServerCollections() {
		this.threadIdCounter = new AtomicLong(0);
		this.activeIdCounter = new AtomicLong(1000);
		this.clientIdCounter = new AtomicLong(1000000);
		
		this.worldLookup = Collections.synchronizedMap(new HashMap<String, World>());
		this.continousLearningAgents = Collections.synchronizedMap(new HashMap<String, Map<String, SGAgent>>());
		
		//this.activeGameWorlds = Collections.synchronizedMap(new HashMap<String, World>());
		this.currentlyRunningWorlds = Collections.synchronizedMap(new HashMap<String, World>());
		
		this.sessionLookup = Collections.synchronizedMap(new HashMap<String, Session>());
		this.gameLookup = Collections.synchronizedMap(new HashMap<String, GameHandler>());
		this.futures = Collections.synchronizedMap(new HashMap<String, Future<GameAnalysis>>());
		this.handlersAssociatedWithGames = Collections.synchronizedMap(new HashMap<String, List<String>>());
		this.clientToGameLookup = Collections.synchronizedMap(new HashMap<String, String>());
		this.configurations = Collections.synchronizedMap(new HashMap<String, ExperimentConfiguration>());
		this.experiments = Collections.synchronizedMap(new HashMap<String, List<ExperimentConfiguration>>());
		this.worldTokens = new ArrayList<GridGameServerToken>();
		
	}
	
	public String getNewCollectionID()
	{
		return Long.toString(this.clientIdCounter.getAndIncrement());
	}
	
	public String getUniqueThreadId() {
		return Long.toString(this.threadIdCounter.getAndIncrement());
	}
	
	public String getNewActiveWorldID() {
		return Long.toString(this.activeIdCounter.getAndIncrement());
	}
	
	public void addWorldTokens(List<GridGameServerToken> tokens) {
		synchronized(this.worldTokens) {
			this.worldTokens.clear();
			this.worldTokens.addAll(tokens);
		}
	}
	
	public List<GridGameServerToken> getWorldTokens() {
		synchronized(this.worldTokens) {
			return new ArrayList<GridGameServerToken>(this.worldTokens);
		}
	}
	
	public void addSession(String id, Session session) {
		synchronized(this.sessionLookup) {
			this.sessionLookup.put(id, session);
		}
	}
	
	public Session getSession(String id) {
		synchronized(this.sessionLookup) {
			return this.sessionLookup.get(id);
		}
	}
	
	public List<Session> getSessions() {
		synchronized(this.sessionLookup) { 
			return new ArrayList<Session>(this.sessionLookup.values());
		}
	}

	public void removeSession(Session session) {
		synchronized(this.sessionLookup) {
			for (Map.Entry<String, Session> entry : this.sessionLookup.entrySet()) {
				if (session.equals(entry.getValue())) {
					this.sessionLookup.remove(entry.getKey());
					return;
				}
			}
		}
	}
	
	public void removeSession(String clientId) {
		synchronized(this.sessionLookup) {
			this.sessionLookup.remove(clientId);
		}
	}
	
	public GameHandler getHandler(String id) {
		synchronized(this.gameLookup) {
			return this.gameLookup.get(id);
		}
	}
	
	public List<GameHandler> getHandlersWithGame(String gameId) {
		List<String> ids = null;
		List<GameHandler> handlers = new ArrayList<GameHandler>();
		synchronized(this.handlersAssociatedWithGames) {
			ids = this.handlersAssociatedWithGames.get(gameId);
		}
		if (ids == null) {
			System.err.println("This game id was not found " + this.handlersAssociatedWithGames.keySet());
			return handlers;
		} else if (ids.size() == 0) {
			System.err.println("The ids for this game are empty");
		}
		synchronized(this.gameLookup) {
			for (String id : ids) {
				handlers.add(this.gameLookup.get(id));
			}
		}
		return handlers;
	}
	
	public void addHandler(String clientId, String worldId, GameHandler handler) {
		synchronized(this.gameLookup) {
			List<String> handlers = this.handlersAssociatedWithGames.get(worldId);
			if (handlers == null) {
				handlers = Collections.synchronizedList(new ArrayList<String>());
				this.handlersAssociatedWithGames.put(worldId, handlers);
			}
			handlers.add(clientId);
			this.gameLookup.put(clientId, handler);
		}
		
		synchronized(this.clientToGameLookup) {
			this.clientToGameLookup.put(clientId, worldId);
		}
	}
	
	public GameHandler removeHandler(String clientId) {
		GameHandler handler = null;
		synchronized(this.gameLookup) {
			handler = this.gameLookup.remove(clientId);
			for (List<String> ids : this.handlersAssociatedWithGames.values()) {
				ids.remove(handler);
			}
		}
		
		synchronized(this.clientToGameLookup) {
			this.clientToGameLookup.remove(clientId);
		}
		return handler;
	}
	
	public List<GameHandler> removeHandlers(String worldId) {
		List<String> ids = null;
		List<GameHandler> handlers = new ArrayList<GameHandler>();
		
		synchronized(this.gameLookup) {
			ids = this.handlersAssociatedWithGames.remove(worldId);
			
			if (ids == null) {
				return handlers;
			}
			for (String id : ids) {
				GameHandler handler = this.gameLookup.remove(id);
				if (handler != null) {
					handlers.add(handler);
				}
			}
			
		}
		synchronized(this.clientToGameLookup) {
			if (ids != null) {
				for (String id : ids) {
					this.clientToGameLookup.remove(id);
				}
			}
		}
		return handlers;
	}
	
	public World getWorld(String id) {
		synchronized(this.worldLookup) {
			return this.worldLookup.get(id);
		}
	}
	
	public void addWorld(String id, World world) {
		synchronized(this.worldLookup) {
			this.worldLookup.put(id,  world);
		}
	}
	
	public SGAgent getContinousLearningAgent(String agentType, World world) {
		synchronized(this.continousLearningAgents) {
			Map<String, SGAgent> agentsOfType = this.continousLearningAgents.get(agentType);
			if (agentsOfType == null) {
				return null;
			}
			
			return agentsOfType.get(world.toString());
		}
	}
	
	public void addContinuousLearningAgent(String agentType, World world, SGAgent agent) {
		synchronized(this.continousLearningAgents) {
			Map<String, SGAgent> agentsOfType = this.continousLearningAgents.get(agentType);
			if (agentsOfType == null) {
				agentsOfType = Collections.synchronizedMap(new HashMap<String, SGAgent>());
				this.continousLearningAgents.put(agentType, agentsOfType);
			}
			
			agentsOfType.put(world.toString(), agent);
		}
	}
	
	
	
	
	
	/*
	public World getActiveWorld(String id) {
		synchronized(this.activeGameWorlds) {
			return this.activeGameWorlds.get(id);
		}
	}
	
	public Map<String, World> getActiveWorlds() {
		synchronized(this.activeGameWorlds) {
			return new HashMap<String, World>(this.activeGameWorlds);
		}
	}
	public void addActiveWorld(String id, World world) {
		synchronized(this.activeGameWorlds) {
			this.activeGameWorlds.put(id,  world);
		}
	}
	
	public World removeActiveWorld(String id) {
		synchronized(this.activeGameWorlds) {
			return this.activeGameWorlds.remove(id);
		}
	}*/
	
	public void addRunningWorld(String id, World world) {
		synchronized(this.currentlyRunningWorlds) {
			this.currentlyRunningWorlds.put(id,  world);
		}
	}
	
	public World removeRunningWorld(String id) {
		synchronized(this.currentlyRunningWorlds) {
			return this.currentlyRunningWorlds.remove(id);
		}
	}
	
	public Future<GameAnalysis> getFuture(String id) {
		synchronized(this.futures) {
			return this.futures.get(id);
		}
	}
	
	public String getFutureId(Future<GameAnalysis> future) {
		synchronized(this.futures) {
			for (Map.Entry<String, Future<GameAnalysis>> entry : this.futures.entrySet()) {
				if (entry.getValue().equals(future)) {
					return entry.getKey();
				}
			}
		}
		return null;
	}
	
	public void addFuture(String id, Future<GameAnalysis> future) {
		synchronized(this.futures){ 
			this.futures.put(id, future);
		}
	}
	
	public Future<GameAnalysis> removeFuture(String id) {
		synchronized(this.futures) {
			return this.futures.get(id);
		}
	}
	
	
	
	public void clearWorlds() {
		synchronized(this.worldLookup) {
			this.worldLookup.clear();
		}
		
		synchronized(this.sessionLookup) {
			this.sessionLookup.clear();
		}
		
		synchronized(this.gameLookup) {
			this.gameLookup.clear();
		}
		
		synchronized(this.futures) {
			this.futures.clear();
		}
		
		/*
		synchronized(this.activeGameWorlds) {
			this.activeGameWorlds.clear();
		}*/
		
	}
	
	public void addConfiguration(ExperimentConfiguration config) {
		String id = config.getActiveGameID();
		synchronized(this.configurations) {
			this.configurations.put(id, config);
		}
		
		synchronized(this.experiments) {
			String experimentType = config.getExperimentType();
			List<ExperimentConfiguration> configs = this.experiments.get(experimentType);
			if (configs == null) {
				configs = Collections.synchronizedList(new ArrayList<ExperimentConfiguration>());
				this.experiments.put(experimentType, configs);
			}
			configs.add(config);
		}
	}
	
	public ExperimentConfiguration getConfiguration(String id) {
		synchronized(this.configurations) {
			return this.configurations.get(id);
		}
	}
	
	public ExperimentConfiguration removeConfiguration(String id) {
		ExperimentConfiguration configuration = null;
		synchronized(this.configurations) {
			configuration = this.configurations.remove(id);
		}
		
		synchronized(this.experiments) {
			String experimentType = configuration.getExperimentType();
			List<ExperimentConfiguration> configs = this.experiments.get(experimentType);
			if (configs != null) {
				configs.remove(configuration);
			}
		}
		
		return configuration;
	}
	
	public Map<String, ExperimentConfiguration> getConfigurations() {
		synchronized(this.configurations) {
			return new HashMap<String, ExperimentConfiguration>(this.configurations);
		}
	}
	
	public ExperimentConfiguration getFirstOpenConfiguration(
			String experimentType) {
		synchronized(this.experiments) {
			List<ExperimentConfiguration> configs = this.experiments.get(experimentType);
			if (configs == null) {
				return null;
			}
			for (ExperimentConfiguration configuration : configs) {
				if (!configuration.isFullyConfigured()) {
					return configuration;
				}
			}
		}
		return null;
	}

	public String getClientId(Session session) {
		synchronized(this.sessionLookup) {
			for (Map.Entry<String, Session> entry : this.sessionLookup.entrySet()) {
				if (entry.getValue() == session) {
					return entry.getKey();
				}
			}
		}
		return null;
	}

	public String getClientsGame(String clientId) {
		synchronized(this.clientToGameLookup) {
			return this.clientToGameLookup.get(clientId);
		}
	}

	

	


}
