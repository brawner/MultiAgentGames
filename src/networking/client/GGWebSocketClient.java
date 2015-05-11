package networking.client;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import networking.common.GridGameExtreme;
import networking.common.GridGameServerToken;
import networking.common.GridGameWorldLoader;
import networking.common.TokenCastException;
import networking.common.messages.WorldFile;
import networking.server.GameHandler;
import networking.server.GridGameServer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import burlap.domain.stochasticgames.gridgame.GGVisualizer;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.visualizer.Visualizer;

public class GGWebSocketClient implements GGWebSocketListener, ConsoleListener{
	private final String destUri;
	private final WebSocketClient client;
	private Session session;
	private final GGWebSocket socket;
	private final ConsoleInteraction console;
	private final Map<String, SGVisualExplorerClient> explorerClients;
	private String id;
	private boolean amOk;
	private List<GridGameServerToken> worldTokens;
	public GGWebSocketClient(String uriStr) {
		this.amOk = true;
		this.destUri = uriStr;
		this.client = new WebSocketClient();
		this.socket = new GGWebSocket();
		this.socket.addListener(this);
		this.explorerClients = new HashMap<String, SGVisualExplorerClient>();
		this.console = ConsoleInteraction.connect(this);
		this.addListener(this.console);
		
	}
	
	public void addListener(GGWebSocketListener listener) {
		this.socket.addListener(listener);
	}
	
	public void removeListener(GGWebSocketListener listener) {
		this.socket.removeListener(listener);
	}
	public boolean isOK() {
		return this.amOk;
	}
	public void attemptConnect() {
		System.out.println("Attempting to connect to: " + this.destUri);
		
		try {
			try {
				this.client.start();
				URI uri = URI.create(this.destUri);
				Future<Session> future = this.client.connect(this.socket, uri);
				this.session = future.get();
				
				System.out.printf("Connected to : %s%n", this.destUri);
				
			} catch (Exception e) {
				e.printStackTrace();
				this.client.stop();
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}
	

	@Override
	public void onConsoleInput(String command, String[] options) {
		GridGameServerToken msg = new GridGameServerToken();
		
		switch (command) {
		case "init":
			msg.setString(GameHandler.MSG_TYPE, GameHandler.INITIALIZE_GAME);
			msg.setString(GridGameServer.WORLD_ID, options[0]);
			break;
		case "join":
			msg.setString(GameHandler.MSG_TYPE, GameHandler.JOIN_GAME);
			msg.setString(GridGameServer.WORLD_ID, options[0]);
			break;
		case "add":
			msg.setString(GameHandler.MSG_TYPE,  GameHandler.ADD_AGENT);
			msg.setString(GridGameServer.AGENT_TYPE, options[0]);
			msg.setString(GridGameServer.WORLD_ID, options[1]);
			break;
		case "run":
			msg.setString(GameHandler.MSG_TYPE, GameHandler.RUN_GAME);
			msg.setString(GridGameServer.WORLD_ID, options[0]);
			break;
		case "remove":
			msg.setString(GameHandler.MSG_TYPE, GameHandler.REMOVE_GAME);
			msg.setString(GridGameServer.WORLD_ID, options[0]);
			break;
		case "exit":
			msg.setString(GameHandler.MSG_TYPE, GameHandler.EXIT_GAME);
			break;
		case "load":
			msg.setString(GameHandler.MSG_TYPE, GameHandler.LOAD_WORLDS);
			break;
		case "visualize":
			this.visualizeWorld(options[0]);
			return;
		}
		
		this.sendMessage(msg);
	}

	@Override
	public GridGameServerToken onMessage(GridGameServerToken msg) {
		GridGameServerToken response = new GridGameServerToken();
		try {
			GridGameServerToken handlerMsg = msg.getToken(GameHandler.HANDLER_RESPONSE);
			if (handlerMsg != null) {
				msg = handlerMsg;
			}
			List<GridGameServerToken> worldTokens = msg.getTokenList(GridGameServer.WORLDS);
			if (worldTokens != null) {
				this.worldTokens = worldTokens;
			}
			
			String msgType = msg.getString(GameHandler.MSG_TYPE);
			if (msgType == null) {
				return new GridGameServerToken();
				
			} else if (msgType.equals(GameHandler.ACTION_REQUEST)) {
				this.console.println("Please take an action");
				
			} else if (msgType.equals(GameHandler.INITIALIZE)) {
				
				String name = msg.getString(GameHandler.AGENT);
				String worldType = msg.getString(GridGameServer.WORLD_TYPE);
				String threadId = msg.getString(GridGameServer.WORLD_ID);
				List<Map<String, Object>> stateObj = (List<Map<String, Object>>)msg.getObject(GameHandler.STATE);
				this.initializeGuiClient(name, threadId, worldType, stateObj);
			}
			
			if (msgType.equals(GridGameServer.HELLO_MESSAGE)) {
				this.id = msg.getString(GridGameServer.CLIENT_ID);
				
			}
		} catch (TokenCastException e) {
			response.setError(true);
		}
		
		return response;
	}
	
	private void initializeGuiClient(String agentName, String threadId, String worldType, List<Map<String, Object>> stateObjects) {
		if (this.worldTokens == null) {
			throw new RuntimeException("This client has received no world descriptions");
		}
		String explorerId = threadId + agentName;
		if (this.explorerClients.containsKey(explorerId)) {
			return;
		}
		
		World world = null;
		try {
			for (GridGameServerToken worldToken : this.worldTokens) {
				if (worldType.equals(worldToken.getString(WorldFile.DESCRIPTION))) {
					world = GridGameWorldLoader.loadWorld(worldToken);
				}
			}
		} catch (TokenCastException e) {
			e.printStackTrace();
		}
		
		SGDomain domain = world.getDomain();
		StateJSONParser parser = new StateJSONParser(domain);
		State startState = parser.JSONPreparedToState(stateObjects);
		JointActionModel jam = world.getActionModel();
		Visualizer visualizer = GridGameExtreme.getVisualizer(world);
		SGVisualExplorerClient explorerClient = new SGVisualExplorerClient(domain, visualizer, startState, jam, this);
		
		explorerClient.addKeyAction("w", agentName + ":"+GridGame.ACTIONNORTH);
		explorerClient.addKeyAction("s", agentName + ":"+GridGame.ACTIONSOUTH);
		explorerClient.addKeyAction("d", agentName + ":"+GridGame.ACTIONEAST);
		explorerClient.addKeyAction("a", agentName + ":"+GridGame.ACTIONWEST);
		explorerClient.addKeyAction("q", agentName + ":"+GridGame.ACTIONNOOP);
		
		this.explorerClients.put(explorerId, explorerClient);
		this.addListener(explorerClient);
		explorerClient.initGUI();
		String text = this.getInitialGameText(worldType, agentName, world, startState);
		explorerClient.printText(text);
	}
	
	public void exitGame(SGVisualExplorerClient client) {
		for (Map.Entry<String, SGVisualExplorerClient> entry : this.explorerClients.entrySet()) {
			if (entry.getValue().equals(client)) {
				String idToRemove = entry.getKey();
				this.removeListener(client);
				this.explorerClients.remove(idToRemove);
				return;
			}
		}
	}
	
	private String getInitialGameText(String worldType, String agentName, World world, State startState) {
		ObjectInstance agentObj = startState.getObject(agentName);
		int agentNumber = agentObj.getDiscValForAttribute(GridGame.ATTPN);
		
		
		StringBuilder builder = new StringBuilder();
		builder.append("Game: ").append(worldType).append("\nAgent: ").append(GGVisualizer.agentColorNames.get(agentNumber));
		builder.append("\nActions available:\n");
		builder.append("w: ").append(GridGame.ACTIONNORTH).append(", ");
		builder.append("s: ").append(GridGame.ACTIONSOUTH).append(", ");
		builder.append("d: ").append(GridGame.ACTIONEAST).append(", ");
		builder.append("a: ").append(GridGame.ACTIONWEST).append(", ");
		builder.append("q: ").append(GridGame.ACTIONNOOP).append("\n");
		return builder.toString();
	}
	private void visualizeWorld(String worldLabel) {
		Map<String, String> worldDescriptions = this.console.getWorldDescriptions();
		Map<String, String> worldLookup = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : worldDescriptions.entrySet()) worldLookup.put(entry.getValue(), entry.getKey());
		
		String worldType = (worldLabel.equalsIgnoreCase("all")) ? "all" : worldDescriptions.get(worldLabel);
		if (worldType == null) {
			return;
		}
		
		try {
			for (GridGameServerToken worldToken : this.worldTokens) {
				if (worldType.equals("all") || worldType.equals(worldToken.getString(WorldFile.DESCRIPTION))) {
					World world = GridGameWorldLoader.loadWorld(worldToken);
					SGDomain domain = world.getDomain();
					State startState = world.startingState();
					JointActionModel jam = world.getActionModel();
					Visualizer visualizer = GridGameExtreme.getVisualizer(world);
					
					if (visualizer == null) {
						throw new RuntimeException("Visualizer could not be generated for world " + world.toString());
					}
					
					SGVisualExplorerClient explorerClient = new SGVisualExplorerClient(domain, visualizer, startState, jam, this);
					String description = world.toString();
					String label = worldLookup.get(description);
					
					explorerClient.printText(label + ": " + description);
					explorerClient.initGUI();
				}
			} 
		} catch (TokenCastException e) {
			e.printStackTrace();
		}
		
		
	}
	
	
	public void close() {
		try {
			this.client.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendMessage(GridGameServerToken msg) {
		if (msg == null || msg.isEmpty()) {
			return;
		}
		
		msg.setString(GridGameServer.CLIENT_ID, this.id);
		String msgStr = msg.toJSONString();
		
		try {
			this.session.getRemote().sendString(msgStr);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	 
    public boolean isConnected() {
    	return (this.session != null);
    }

    public static void main(String[] args) {
    	String host = "elzar.cs.brown.edu:8787";
    	if (args.length > 0) {
    		host = args[0];
    	}
    	String webSocketAddress = "ws://" + host + "/events/";
    	GGWebSocketClient client = new GGWebSocketClient(webSocketAddress);
    	client.attemptConnect();
    	while (!client.isConnected()) {
    		try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	
    	System.out.println("Client is connected");
    	
    	while (client.isOK()) {
    		try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	
    	System.out.println("Client is shutting down");
    }
    /*
    public void connectToWorld(String worldID) {
		GridGameServerToken request = new GridGameServerToken();
		request.setString(GridGameServer.WORLD_ID, worldID);
		request.setString(GridGameServer.CLIENT_ID, this.id);
		GridGameServerToken gameToken = new GridGameServerToken();
		gameToken.setString(GameHandler.MSG_TYPE, GameHandler.JOIN_GAME);
		gameToken.setString(GameHandler.AGENT_TYPE, "not_robot");
		request.setToken(GridGameServer.GAME_MESSAGE, gameToken);
		
		this.socketClient.sendMessage(request);
	}*/
	
	
	
	
}
