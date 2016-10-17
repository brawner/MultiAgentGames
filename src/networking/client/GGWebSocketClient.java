//package networking.client;
//import java.awt.Color;
//import java.io.IOException;
//import java.net.URI;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.Future;
//
//import networking.common.GridGameServerToken;
//import networking.common.GridGameWorldLoader;
//import networking.common.TokenCastException;
//import networking.common.messages.WorldFile;
//import networking.server.GameHandler;
//import networking.server.GridGameManager;
//
//import org.eclipse.jetty.websocket.api.Session;
//import org.eclipse.jetty.websocket.client.WebSocketClient;
//
//import burlap.domain.stochasticgames.gridgame.GGVisualizer;
//import burlap.domain.stochasticgames.gridgame.GridGame;
//import burlap.mdp.core.oo.state.ObjectInstance;
//import burlap.mdp.core.state.State;
//import burlap.mdp.stochasticgames.SGDomain;
//import burlap.mdp.stochasticgames.model.FullJointModel;
//import burlap.mdp.stochasticgames.model.JointModel;
//import burlap.mdp.stochasticgames.world.World;
//import burlap.visualizer.Visualizer;
//
///**
// * The java client to the network games. This connects to a GGWebSocketServer at a specified address. The default constantly changes
// * between localhost and elzar. It may be better to specify it as the command line argument.
// * @author brawner
// *
// */
//public class GGWebSocketClient implements GGWebSocketListener, ConsoleListener{
//	public static final List<Color> AGENT_COLORS = Arrays.asList(Color.green, Color.blue, Color.magenta, Color.orange);
//	private final String destUri;
//	private final WebSocketClient client;
//	private Session session;
//	private final GGWebSocket socket;
//	private final ConsoleInteraction console;
//	private final Map<String, SGVisualExplorerClient> explorerClients;
//	private String id;
//	private boolean amOk;
//	private List<GridGameServerToken> worldTokens;
//	public GGWebSocketClient(String uriStr) {
//		this.amOk = true;
//		this.destUri = uriStr;
//		this.client = new WebSocketClient();
//		this.socket = new GGWebSocket();
//		this.socket.addListener(this);
//		this.explorerClients = new HashMap<String, SGVisualExplorerClient>();
//		this.console = ConsoleInteraction.connect(this);
//		this.addListener(this.console);
//		
//	}
//	
//	public void addListener(GGWebSocketListener listener) {
//		this.socket.addListener(listener);
//	}
//	
//	public void removeListener(GGWebSocketListener listener) {
//		this.socket.removeListener(listener);
//	}
//	public boolean isOK() {
//		return this.amOk;
//	}
//	public void attemptConnect() {
//		System.out.println("Attempting to connect to: " + this.destUri);
//		
//		try {
//			try {
//				this.client.start();
//				URI uri = URI.create(this.destUri);
//				Future<Session> future = this.client.connect(this.socket, uri);
//				this.session = future.get();
//				
//				System.out.printf("Connected to : %s%n", this.destUri);
//				
//			} catch (Exception e) {
//				e.printStackTrace();
//				this.client.stop();
//			}
//		}
//		catch (Throwable t) {
//			t.printStackTrace();
//		}
//	}
//	
//
//	@Override
//	public void onConsoleInput(String command, String[] options) {
//		GridGameServerToken msg = new GridGameServerToken();
//		
//		switch (command) {
//		case "init":
//			msg.setString(GridGameManager.MSG_TYPE, GameHandler.INITIALIZE);
//			msg.setString(GridGameManager.WORLD_ID, options[0]);
//			break;
//		case "join":
//			msg.setString(GridGameManager.MSG_TYPE, GameHandler.JOIN_GAME);
//			msg.setString(GridGameManager.WORLD_ID, options[0]);
//			break;
//		case "add":
//			msg.setString(GridGameManager.MSG_TYPE,  GameHandler.ADD_AGENT);
//			msg.setString(GridGameManager.AGENT_TYPE, options[0]);
//			msg.setString(GridGameManager.WORLD_ID, options[1]);
//			break;
//		case "run":
//			msg.setString(GridGameManager.MSG_TYPE, GameHandler.RUN_GAME);
//			msg.setString(GridGameManager.WORLD_ID, options[0]);
//			break;
//		case "remove":
//			msg.setString(GridGameManager.MSG_TYPE, GameHandler.REMOVE_GAME);
//			msg.setString(GridGameManager.WORLD_ID, options[0]);
//			break;
//		case "exit":
//			msg.setString(GridGameManager.MSG_TYPE, GameHandler.EXIT_GAME);
//			break;
//		case "load":
//			msg.setString(GridGameManager.MSG_TYPE, GameHandler.LOAD_WORLDS);
//			break;
//		case "visualize":
//			this.visualizeWorld(options[0]);
//			return;
//		}
//		
//		this.sendMessage(msg);
//	}
//
//	@Override
//	public GridGameServerToken onMessage(GridGameServerToken msg) {
//		System.out.println("Running on Message at 133 GGWSC");
//		GridGameServerToken response = new GridGameServerToken();
//		try {
//			
//			List<GridGameServerToken> worldTokens = msg.getTokenList(GridGameManager.WORLDS);
//			if (worldTokens != null) {
//				this.worldTokens = worldTokens;
//			}
//			
//			String msgType = msg.getString(GridGameManager.MSG_TYPE);
//			if (msgType == null) {
//				return new GridGameServerToken();
//				
//			} else if (msgType.equals(GameHandler.ACTION_REQUEST)) {
//				this.console.println("Please take an action");
//				
//			} else if (msgType.equals(GameHandler.INITIALIZE)) {
//				
//				String name = msg.getString(GameHandler.AGENT);
//				String worldType = msg.getString(GridGameManager.WORLD_TYPE);
//				String threadId = msg.getString(GridGameManager.WORLD_ID);
//				List<Map<String, Object>> stateObj = (List<Map<String, Object>>)msg.getObject(GameHandler.STATE);
//				this.initializeGuiClient(name, threadId, worldType, stateObj);
//			}
//			
//			if (msgType.equals(GridGameManager.HELLO_MESSAGE)) {
//				this.id = msg.getString(GridGameManager.CLIENT_ID);
//				
//			}
//		} catch (TokenCastException e) {
//			e.printStackTrace();
//			response.setError(true);
//		}
//		
//		return response;
//	}
//	
//	/**
//	 * Initializes a GUI client, if new key actions should be added, that would happen here.
//	 * @param agentName
//	 * @param threadId
//	 * @param worldType
//	 * @param stateObjects
//	 */
//	private void initializeGuiClient(String agentName, String threadId, String worldType, List<Map<String, Object>> stateObjects) {
//		if (this.worldTokens == null) {
//			throw new RuntimeException("This client has received no world descriptions");
//		}
//		String explorerId = threadId + agentName;
//		if (this.explorerClients.containsKey(explorerId)) {
//			return;
//		}
//		
//		World world = null;
//		try {
//			for (GridGameServerToken worldToken : this.worldTokens) {
//				if (worldType.equals(worldToken.getString(WorldFile.DESCRIPTION))) {
//					world = GridGameWorldLoader.loadWorld(worldToken);
//				}
//			}
//		} catch (TokenCastException e) {
//			e.printStackTrace();
//		}
//		
//		SGDomain domain = world.getDomain();
//		StateJSONParser parser = new StateJSONParser(domain);
//		State startState = parser.JSONPreparedToState(stateObjects);
//		JointModel jam = world.getActionModel();
//		Visualizer visualizer = GridGameExtreme.getVisualizer(world);
//		SGVisualExplorerClient explorerClient = new SGVisualExplorerClient(domain, visualizer, startState, (FullJointModel)jam, this);
//		
//		explorerClient.addKeyAction("w", 0, agentName + ":"+GridGame.ACTION_NORTH, explorerId);
//		explorerClient.addKeyAction("s", 0, agentName + ":"+GridGame.ACTION_SOUTH, explorerId);
//		explorerClient.addKeyAction("d", 0, agentName + ":"+GridGame.ACTION_EAST, explorerId);
//		explorerClient.addKeyAction("a", 0, agentName + ":"+GridGame.ACTION_WEST, explorerId);
//		explorerClient.addKeyAction("q", 0, agentName + ":"+GridGame.ACTION_NOOP, explorerId);
//		
//		this.explorerClients.put(explorerId, explorerClient);
//		this.addListener(explorerClient);
//		explorerClient.initGUI();
//		System.out.println("Agent name " + agentName);
//		System.out.println("State: " + startState.toString());
//		String text = this.getInitialGameText(worldType, agentName, world, startState);
//	}
//	
//	public void exitGame(SGVisualExplorerClient client) {
//		for (Map.Entry<String, SGVisualExplorerClient> entry : this.explorerClients.entrySet()) {
//			if (entry.getValue().equals(client)) {
//				String idToRemove = entry.getKey();
//				this.removeListener(client);
//				this.explorerClients.remove(idToRemove);
//				return;
//			}
//		}
//	}
//	
//	private String getInitialGameText(String worldType, String agentName, World world, State startState) {
//		ObjectInstance agentObj = (ObjectInstance) startState.get(agentName);
//		int agentNumber = (int) agentObj.get(GridGame.VAR_PN);
//		
//		
//		StringBuilder builder = new StringBuilder();
//		builder.append("Game: ").append(worldType).append("\nAgent: ").append(AGENT_COLORS.get(agentNumber).toString());
//		builder.append("\nActions available:\n");
//		builder.append("w: ").append(GridGame.ACTION_NORTH).append(", ");
//		builder.append("s: ").append(GridGame.ACTION_SOUTH).append(", ");
//		builder.append("d: ").append(GridGame.ACTION_EAST).append(", ");
//		builder.append("a: ").append(GridGame.ACTION_WEST).append(", ");
//		builder.append("q: ").append(GridGame.ACTION_NOOP).append("\n");
//		return builder.toString();
//	}
//	private void visualizeWorld(String worldLabel) {
//		Map<String, String> worldDescriptions = this.console.getWorldDescriptions();
//		Map<String, String> worldLookup = new HashMap<String, String>();
//		for (Map.Entry<String, String> entry : worldDescriptions.entrySet()) worldLookup.put(entry.getValue(), entry.getKey());
//		
//		String worldType = (worldLabel.equalsIgnoreCase("all")) ? "all" : worldDescriptions.get(worldLabel);
//		if (worldType == null) {
//			System.err.println("World label " + worldLabel + " does not exist.");
//			return;
//		}
//		
//		try {
//			boolean found = false;
//			for (GridGameServerToken worldToken : this.worldTokens) {
//				if (worldType.equals("all") || worldType.equals(worldToken.getString(WorldFile.DESCRIPTION))) {
//					found = true;
//					World world = GridGameWorldLoader.loadWorld(worldToken);
//					SGDomain domain = world.getDomain();
//					State startState = world.startingState();
//					JointModel jam = world.getActionModel();
//					Visualizer visualizer = GridGameExtreme.getVisualizer(world);
//					
//					if (visualizer == null) {
//						throw new RuntimeException("Visualizer could not be generated for world " + world.toString());
//					}
//					
//					SGVisualExplorerClient explorerClient = new SGVisualExplorerClient(domain, visualizer, startState, (FullJointModel)jam, this);
//					String description = world.toString();
//					String label = worldLookup.get(description);
//					
//					explorerClient.initGUI();
//				}
//			} 
//			if (!found) {
//				System.out.println("World label not found");
//			}
//		} catch (TokenCastException e) {
//			e.printStackTrace();
//		}
//		
//		
//	}
//	
//	
//	public void close() {
//		try {
//			this.client.stop();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public void sendMessage(GridGameServerToken msg) {
//		if (msg == null || msg.isEmpty()) {
//			return;
//		}
//		
//		msg.setString(GridGameManager.CLIENT_ID, this.id);
//		String msgStr = msg.toJSONString();
//		
//		try {
//			this.session.getRemote().sendString(msgStr);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	 
//    public boolean isConnected() {
//    	return (this.session != null);
//    }
//
//    public static void main(String[] args) {
//    	String host = "localhost:8787";//"elzar.cs.brown.edu:8787";
//    	if (args.length > 0) {
//    		host = args[0];
//    	}
//    	String webSocketAddress = "ws://" + host + "/events/";
//    	GGWebSocketClient client = new GGWebSocketClient(webSocketAddress);
//    	client.attemptConnect();
//    	while (!client.isConnected()) {
//    		try {
//				Thread.sleep(10);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//    	}
//    	
//    	System.out.println("Client is connected");
//    	
//    	while (client.isOK()) {
//    		try {
//				Thread.sleep(10);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//    	}
//    	
//    	System.out.println("Client is shutting down");
//    }
//    /*
//    public void connectToWorld(String worldID) {
//		GridGameServerToken request = new GridGameServerToken();
//		request.setString(GridGameServer.WORLD_ID, worldID);
//		request.setString(GridGameServer.CLIENT_ID, this.id);
//		GridGameServerToken gameToken = new GridGameServerToken();
//		gameToken.setString(GameHandler.MSG_TYPE, GameHandler.JOIN_GAME);
//		gameToken.setString(GameHandler.AGENT_TYPE, "not_robot");
//		request.setToken(GridGameServer.GAME_MESSAGE, gameToken);
//		
//		this.socketClient.sendMessage(request);
//	}*/
//	
//	
//	
//	
//}
