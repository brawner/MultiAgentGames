package networking.client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import networking.common.GridGameServerToken;
import networking.common.TokenCastException;
import networking.common.messages.WorldFile;
import networking.server.GridGameManager;

/**
 * Handles text interaction on the command line for the client. Probably not a whole lot has to be changed, except maybe the instructions it prints out.
 * @author brawner
 *
 */
public class ConsoleInteraction implements Callable<Boolean>, GGWebSocketListener{

	private static ConsoleInteraction singleton;
	private BufferedReader reader;
	private final Map<String, String> worlds;
	private final Map<String, String> activeWorlds;
	private final List<ConsoleListener> listeners;
	private States state;
	
	private ConsoleInteraction() {
		this.state = States.HOME;
		this.reader = new BufferedReader(new InputStreamReader(System.in));
		this.worlds = new HashMap<String, String>();
		this.activeWorlds = new HashMap<String, String>();
		this.listeners = new ArrayList<ConsoleListener>();
	}
	
	public static ConsoleInteraction connect(ConsoleListener listener) {
		if (ConsoleInteraction.singleton == null) {
			ConsoleInteraction.singleton = new ConsoleInteraction();
		}
		ConsoleInteraction.singleton.addListener(listener);
		return ConsoleInteraction.singleton;
	}
	
	public GridGameServerToken onMessage(GridGameServerToken msg) {
		try {
			List<GridGameServerToken> worlds = msg.getTokenList(GridGameManager.WORLDS);
			
			if (worlds != null) {
				this.worlds.clear();
				
				for (GridGameServerToken token : worlds) {
					this.worlds.put(token.getString(WorldFile.LABEL), token.getString(WorldFile.DESCRIPTION));
				}
			}
			
			List<GridGameServerToken> activeGames = msg.getTokenList(GridGameManager.ACTIVE);
			
			if (activeGames != null) {
				this.activeWorlds.clear();
				for (GridGameServerToken gameToken : activeGames) {
					this.activeWorlds.put(gameToken.getString(WorldFile.LABEL), gameToken.getString(WorldFile.DESCRIPTION));
				}
			}
			
		} catch (TokenCastException e) {
			e.printStackTrace();
		}
		
		
		this.printOptions();
		
		
		return null;
	}
	
	private void addListener(ConsoleListener listener) {
		if (!this.listeners.contains(listener)) {
			this.listeners.add(listener);
		}
	}
	
	public Map<String, String> getWorldDescriptions() {
		return new HashMap<String, String>(this.worlds);
	}

	@Override
	public Boolean call() throws Exception {
		while (true) {
			if (!this.worlds.isEmpty()) {
				String input = this.waitForInput();
				String[] split = input.split(" ");
				
				if (split.length > 0) {
					String command = split[0];
					String[] params = (split.length == 1) ? null : Arrays.copyOfRange(split, 1, split.length);
					
					for (ConsoleListener listener : this.listeners) {
						listener.onConsoleInput(command, params);
					}
				}
			} else {
				Thread.sleep(10);
			}
			
		}
	}
	
	public void println(String str) {
		System.out.println(str);
	}
	
	private void printOptions() {
	
		switch(this.state) {
			case HOME:
				String homescreen = this.home();
				System.out.println(homescreen);
				break;
			default:
				return;
		}
	}
	
	private String home() {
		StringBuilder builder = new StringBuilder();
		
		this.availableCommands(builder);
		this.availableOptions(builder);
		
		return builder.toString();
	}
	
	private void availableCommands(StringBuilder builder) {
		builder.append("Commands: \n");
		builder.append("\t").append("init [world id] | Initializes a new game with the given world id, with no agents\n");
		builder.append("\t").append("join [game id] | Join an already active game, that is waiting for more agents\n");
		builder.append("\t").append("add (MAVI, Random) [game id] | Adds an automated agent of the specified type to the game\n");
		builder.append("\t").append("run [game id] | Runs the game with the given game id, and the already connected agents\n");
		builder.append("\t").append("remove [game id] | Removes the game with the given game id\n");
		builder.append("\t").append("visualize [world id / all] | Visualize the specified world with max number of agents, or all available worlds");
		builder.append("\t").append("exit | Exits the client from the currently running game\n");
	}
	
	private void availableOptions(StringBuilder builder) {
		builder.append("Available worlds:\n");
		builder.append("\tid: world description\n");
		
		List<String> worldLines = new ArrayList<String>();
		for (Map.Entry<String, String> entry : this.worlds.entrySet()) {
			StringBuilder b = new StringBuilder();
			b.append("\t").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
			worldLines.add(b.toString());
		}
		Collections.sort(worldLines);
		for (String line : worldLines) {
			builder.append(line);
		}
		
		if (this.activeWorlds != null) {
			builder.append("Available games to join:\n");
			builder.append("\tid: game description\n");
			List<String> activeLines = new ArrayList<String>();
			for (Map.Entry<String, String> entry : this.activeWorlds.entrySet()) {
				StringBuilder b = new StringBuilder();
				b.append("\t").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
				activeLines.add(b.toString());
			}
			Collections.sort(activeLines);
			for (String line : activeLines) {
				builder.append(line);
			}
		}
		builder.append("Enter a command:\n");
	}
	
	
	private String waitForInput() {
		try {
			String lin = this.reader.readLine();
			System.out.println("Read: " + lin);
			return lin;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private enum States {
		HOME,
		WORLD_SELECTION,
		GAME_SELECTION
	}

}
