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

public class GridGameServerCollections {

	private final List<GridGameServerToken> worldTokens;
	private final Map<String, World> worldLookup;
	private final Map<String, World> activeGameWorlds;
	private final Map<String, World> currentlyRunningWorlds;
	
	private final Map<String, Session> sessionLookup;
	private final Map<String, GameHandler> gameLookup;
	private final Map<String, Future<GameAnalysis>> futures;
	private final Map<String, List<String>> handlersAssociatedWithGames;
	
	private final ReadWriteLock collectionsLock;
	
	private final AtomicLong threadIdCounter;
	private final AtomicLong activeIdCounter;
	private final AtomicLong clientIdCounter;
	
	public GridGameServerCollections() {
		this.threadIdCounter = new AtomicLong();
		this.activeIdCounter = new AtomicLong();
		this.clientIdCounter = new AtomicLong();
		
		this.collectionsLock = new ReentrantReadWriteLock();
		
		this.worldLookup = Collections.synchronizedMap(new HashMap<String, World>());
		this.activeGameWorlds = Collections.synchronizedMap(new HashMap<String, World>());
		this.currentlyRunningWorlds = Collections.synchronizedMap(new HashMap<String, World>());
		
		this.sessionLookup = Collections.synchronizedMap(new HashMap<String, Session>());
		this.gameLookup = Collections.synchronizedMap(new HashMap<String, GameHandler>());
		this.futures = Collections.synchronizedMap(new HashMap<String, Future<GameAnalysis>>());
		this.handlersAssociatedWithGames = Collections.synchronizedMap(new HashMap<String, List<String>>());
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
		this.worldTokens.clear();
		this.worldTokens.addAll(tokens);
	}
	
	public List<GridGameServerToken> getWorldTokens() {
		List<GridGameServerToken> tokens = null;
		synchronized(this.worldTokens) {
			tokens = new ArrayList<GridGameServerToken>(this.worldTokens);
		}
		return tokens;		
	}
	
	public void addSession(String id, Session session) {
		synchronized(this.sessionLookup) {
			this.sessionLookup.put(id, session);
		}
	}
	
	public Session getSession(String id) {
		Session session = null;
		synchronized(this.sessionLookup) {
			session = this.sessionLookup.get(id);
		}
		return session;
	}
	
	public List<Session> getSessions() {
		this.collectionsLock.readLock().lock();
		List<Session> sessions = null;
		synchronized(this.sessionLookup) { 
			sessions = new ArrayList<Session>(this.sessionLookup.values());
		}
		return sessions;
	}
	public GameHandler getHandler(String id) {
		GameHandler handler = null;
		synchronized(this.gameLookup) {
			handler = this.gameLookup.get(id);
		}
		return handler;
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
		GameHandler handler = null;
		synchronized(this.gameLookup) {
			handler = this.gameLookup.remove(id);
			for (List<String> ids : this.handlersAssociatedWithGames.values()) {
				ids.remove(handler);
			}
		}
		return handler;
	}
	
	public List<GameHandler> removeHandlers(String worldId) {
		
		List<GameHandler> handlers = null;
		synchronized(this.gameLookup) {
			List<String> ids = this.handlersAssociatedWithGames.remove(worldId);
			handlers = new ArrayList<GameHandler>();
			for (String id : ids) {
				GameHandler handler = this.gameLookup.remove(id);
				if (handler != null) {
					handlers.add(handler);
				}
			}
		}
		
		return handlers;
	}
	
	public World getWorld(String id) {
		World world = null;
		synchronized(this.worldLookup) {
			world = this.worldLookup.get(id);
		}
		return world;
	}
	
	public void addWorld(String id, World world) {
		synchronized(this.worldLookup) {
			this.worldLookup.put(id,  world);
		}
	}
	
	public World getActiveWorld(String id) {
		World world = null;
		synchronized(this.activeGameWorlds) {
			world = this.activeGameWorlds.get(id);
		}
		return world;
	}
	
	public Map<String, World> getActiveWorlds() {
		Map<String, World> worlds = null;
		synchronized(this.activeGameWorlds) {
			worlds = new HashMap<String, World>(this.activeGameWorlds);
		}
		return worlds;
	}
	public void addActiveWorld(String id, World world) {
		synchronized(this.activeGameWorlds) {
			this.activeGameWorlds.put(id,  world);
		}
	}
	
	public World removeActiveWorld(String id) {
		World world = null;
		synchronized(this.activeGameWorlds) {
			world = this.activeGameWorlds.remove(id);
		}
		return world;
	}
	
	public void addRunningWorld(String id, World world) {
		synchronized(this.currentlyRunningWorlds) {
			this.currentlyRunningWorlds.put(id,  world);
		}
	}
	
	public World removeRunningWorld(String id) {
		World world = null;
		synchronized(this.currentlyRunningWorlds) {
			world = this.currentlyRunningWorlds.remove(id);
		}
		return world;
	}
	
	public Future<GameAnalysis> getFuture(String id) {
		Future future = null;
		synchronized(this.futures) {
			future = this.futures.get(id);
		}
		return future;
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
		Future<GameAnalysis> future = null;
		synchronized(this.futures) {
			future = this.futures.get(id);
		}
		return future;
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
		
		synchronized(this.activeGameWorlds) {
			this.activeGameWorlds.clear();
		}
		
	}

}
