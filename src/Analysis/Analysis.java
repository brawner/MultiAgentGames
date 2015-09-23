package Analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import networking.common.GridGameExtreme;
import burlap.behavior.stochasticgames.GameAnalysis;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.states.State;
import burlap.oomdp.legacy.StateJSONParser;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.SGDomain;


public class Analysis {
	public static GridGame gridGame = new GridGame();
	public static SGDomain domain = GridGameExtreme.generateDomain(gridGame, false);
	public static StateJSONParser sp = new StateJSONParser(domain);
	
	public static class AgentTurn {
		public String name;
		public String turkId;
		public String action;
		public int x;
		public int y;
		public int rt;
		
		public void write(FileWriter writer) throws IOException {
			writer.append(turkId).append(",").append(Integer.toString(x)).append(",");
			writer.append(Integer.toString(y)).append(",").append(Integer.toString(rt));
			if (action != null) {
				writer.append(",").append(action);
			}
		}
	}
	
	public static class Turn {
		public AgentTurn agent1 = new AgentTurn();
		public AgentTurn agent2 = new AgentTurn();
		public int turnNumber;
		
		public void write(FileWriter writer, String id, int roundNumber) throws IOException {
			writer.append(id).append(",").append(Integer.toString(roundNumber)).append(",").append(Integer.toString(turnNumber)).append(",");
			agent1.write(writer);
			writer.append(",");
			agent2.write(writer);
			writer.append("\n");
		}
	}
	
	public static class Round implements Comparable<Round>{
		public List<Turn> turns = new ArrayList<Turn>();
		public int roundNumber;
		public void write(FileWriter writer, String id) throws IOException {
			for (Turn turn : turns) {
				turn.write(writer, id, roundNumber);
			}
		}
		@Override
		public int compareTo(Round o) {
			return Integer.compare(this.roundNumber, o.roundNumber);
		}
	}
	
	public static class Match {
		public List<Round> rounds = new ArrayList<Round>();
		public String matchId;
		public void write(FileWriter writer) throws IOException {
			for (Round round : rounds) {
				round.write(writer, matchId);
			}
		}
	}
	
	public static class Experiment {
		public Map<String, Match> matches = new LinkedHashMap<String, Match>();
		public void write(FileWriter writer) throws IOException {
			for (Map.Entry<String, Match> entry : matches.entrySet()) {
				Match match = entry.getValue();
				match.write(writer);
			}
		}
	}
	
	public static Round getGameResult(String filename) {
		int s = filename.lastIndexOf("_");
		int end = filename.lastIndexOf(".");
		int roundNumber = Integer.parseInt(filename.substring(s+1,end));
		String text;
		try {
			text = new String(Files.readAllBytes(Paths.get(filename)));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}	
		GameAnalysis analysis = GameAnalysis.legacyParseStringIntoGameAnalysis(text, domain, sp);
		List<JointAction> actions = analysis.getJointActions();
		List<State> states = analysis.getStates();
		Round round = new Round();
		round.roundNumber = roundNumber;
		for (int i = 0; i < actions.size(); i++) {
			JointAction action = actions.get(i);
			State state = states.get(i);
			GroundedSingleAction agent1Action = action.action("agent0");
			GroundedSingleAction agent2Action = action.action("agent1");
			Turn turn = new Turn();
			turn.agent1.name = agent1Action.actingAgent;
			turn.agent1.action = agent1Action.actionName();
			turn.agent2.name = agent2Action.actingAgent;
			turn.agent2.action = agent2Action.actionName();
			turn.agent1.x = state.getObject("agent0").getIntValForAttribute(GridGame.ATTX);
			turn.agent1.y = state.getObject("agent0").getIntValForAttribute(GridGame.ATTY);
			turn.agent2.x = state.getObject("agent1").getIntValForAttribute(GridGame.ATTX);
			turn.agent2.y = state.getObject("agent1").getIntValForAttribute(GridGame.ATTY);
			turn.turnNumber = i;
			round.turns.add(turn);
		}
		
		State state = states.get(states.size() - 1);
		Turn turn = new Turn();
		turn.agent1.name = "agent0";
		turn.agent1.action = null;
		turn.agent2.name = "agent1";
		turn.agent1.x = state.getObject("agent0").getIntValForAttribute(GridGame.ATTX);
		turn.agent1.y = state.getObject("agent0").getIntValForAttribute(GridGame.ATTY);
		turn.agent2.x = state.getObject("agent1").getIntValForAttribute(GridGame.ATTX);
		turn.agent2.y = state.getObject("agent1").getIntValForAttribute(GridGame.ATTY);
		turn.turnNumber = round.turns.size();
		round.turns.add(turn);
		
		return round;
	}
	

	public static void addReactionTimes(String filename, Round round) {
		List<String> lines;
		try {
			lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		int turnNum = 0;
		String lastAgent = null;
		for (String line : lines) {
			String[] split = line.split(",");
			String agent = split[1];
			if (!agent.equals(lastAgent)) {
				lastAgent = agent;
				turnNum = 0;
			}
			String turkId = split[2];
			//String turnStr = split[4];
			String rtStr = split[5];
			Turn turn = round.turns.get(turnNum);
			AgentTurn agentTurn = (agent.equals("agent0")) ? turn.agent1 : turn.agent2;
			agentTurn.rt = Integer.parseInt(rtStr);
			agentTurn.turkId = turkId;
			turnNum++;
		}
	}

	public static Match getMatch(final String baseDir, String number) {
		final String filePattern = number;
		File file = new File(baseDir);
		FilenameFilter filter = new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.contains(filePattern) &&
						name.contains(".game");
			}
		};
		Match match = new Match();
		String[] children = file.list(filter);
		Integer matchNum = null;
		for (int i = 0; i < children.length; ++i) {
			if (matchNum == null) {
				int s = children[i].indexOf("episode") + "episode".length();
				int e = children[i].lastIndexOf("_");
				matchNum = Integer.parseInt(children[i].substring(s,e));
				
			}
			match.rounds.add(getGameResult(baseDir + "/" + children[i]));
		}
		if (matchNum != null) {
			match.matchId = matchNum.toString();
		}
		Collections.sort(match.rounds);
		FilenameFilter filterRT = new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.contains(filePattern) &&
						name.contains("reactionTimes");
			}
		};
		
		children = file.list(filterRT);
		for (int i = 0; i < children.length; ++i) {
			int s = children[i].lastIndexOf("_");
			int e = children[i].lastIndexOf(".");
			int roundNum = Integer.parseInt(children[i].substring(s+1,e))-1;
			addReactionTimes(baseDir + "/" + children[i], match.rounds.get(roundNum));
		}
		
		return match;
	}
	
	// 1000009,name_2,agent0,human,2015_09_15_17_32_46_680_277064168
	public static Experiment loadExperiment(String baseDir, String filename) {
		Experiment experiment = new Experiment();
		Path path = Paths.get(baseDir, filename).toAbsolutePath();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path.toString()));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] split = line.split(",");
				if (split.length != 5) {
					continue;
				}
				
				String filePrefix = split[4];
				
				if (!experiment.matches.containsKey(filePrefix)) {
					Match match = getMatch(baseDir, filePrefix);
					experiment.matches.put(filePrefix, match);
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			System.err.println("Failed to read file " + path.toString());
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return experiment;
	}
	
	public static void write(String filename, Experiment experiment) {
		try {
			FileWriter writer = new FileWriter(filename);
			experiment.write(writer);
			writer.close();
		} catch (IOException e) {
			System.err.println("Failed to write file");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String directory = (args.length == 0) ? null : args[0];
        File file;
		while (directory == null || !(file = new File(directory)).exists() || !file.isDirectory()) {
			System.out.println("Enter an appropriate directory");
			try {
				directory = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		
		String outfilename = (args.length < 2) ? null : args[1];
		File outFile;
		while (outfilename == null || (outFile = new File(outfilename)).exists()) {
			System.out.println("Enter an appropriate output file name");
			try {
				outfilename = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		
		Experiment experiment = loadExperiment(file.getAbsolutePath(), "IDMap.csv");
		if (outFile.exists()) {
			System.out.println("Cannot overwrite existing data file, choose a different name");
			return;
		}
		
		write(outFile.getAbsolutePath(), experiment);
		
	}
}
