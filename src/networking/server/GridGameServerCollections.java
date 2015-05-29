package networking.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import networking.common.GridGameServerToken;

import org.eclipse.jetty.websocket.api.Session;

import burlap.behavior.stochasticgame.GameAnalysis;
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
	private final Map<String, GridGameConfiguration> configurations;
	
	private final AtomicLong threadIdCounter;
	private final AtomicLong activeIdCounter;
	private final AtomicLong clientIdCounter;
	
	public GridGameServerCollections() {
		this.threadIdCounter = new AtomicLong();
		this.activeIdCounter = new AtomicLong();
		this.clientIdCounter = new AtomicLong();
		
		this.worldLookup = Collections.synchronizedMap(new HashMap<String, World>());
		//this.activeGameWorlds = Collections.synchronizedMap(new HashMap<String, World>());
		this.currentlyRunningWorlds = Collections.synchronizedMap(new HashMap<String, World>());
		
		this.sessionLookup = Collections.synchronizedMap(new HashMap<String, Session>());
		this.gameLookup = Collections.synchronizedMap(new HashMap<String, GameHandler>());
		this.futures = Collections.synchronizedMap(new HashMap<String, Future<GameAnalysis>>());
		this.handlersAssociatedWithGames = Collections.synchronizedMap(new HashMap<String, List<String>>());
		this.configurations = Collections.synchronizedMap(new HashMap<String, GridGameConfiguration>());
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
	
	public GameHandler getHandler(String id) {
		synchronized(this.gameLookup) {
			return this.gameLookup.get(id);
		}
	}
	
	public List<GameHandler> getHandlersWithGame(String gameId) {
		List<String> ids = new ArrayList<String>();
		synchronized(this.handlersAssociatedWithGames) {
			if (this.handlersAssociatedWithGames.containsKey(gameId)) {
				ids = this.handlersAssociatedWithGames.get(gameId);
			}
		}
		
		synchronized(this.gameLookup) {
			List<GameHandler> handlers = new ArrayList<GameHandler>();
			
			for (String id : ids) {
				handlers.add(this.gameLookup.get(id));
			}
			
			return handlers;
		}
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
	}
	
	public GameHandler removeHandler(String id) {
		synchronized(this.gameLookup) {
			GameHandler handler = this.gameLookup.remove(id);
			for (List<String> ids : this.handlersAssociatedWithGames.values()) {
				ids.remove(handler);
			}
			return handler;
		}
	}
	
	public List<GameHandler> removeHandlers(String worldId) {
		
		synchronized(this.gameLookup) {
			List<String> ids = this.handlersAssociatedWithGames.remove(worldId);
			
			List<GameHandler> handlers = new ArrayList<GameHandler>();
			if (ids == null) {
				return handlers;
			}
			for (String id : ids) {
				GameHandler handler = this.gameLookup.remove(id);
				if (handler != null) {
					handlers.add(handler);
				}
			}
			return handlers;
		}
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
	
	public void addConfiguration(String id, GridGameConfiguration config) {
		synchronized(this.configurations) {
			this.configurations.put(id, config);
		}
	}
	
	public GridGameConfiguration getConfiguration(String id) {
		synchronized(this.configurations) {
			return this.configurations.get(id);
		}
	}
	
	public GridGameConfiguration removeConfiguration(String id) {
		synchronized(this.configurations) {
			return this.configurations.remove(id);
		}
	}
	
	public Map<String, GridGameConfiguration> getConfigurations() {
		synchronized(this.configurations) {
			return new HashMap<String, GridGameConfiguration>(this.configurations);
		}
	}

}
