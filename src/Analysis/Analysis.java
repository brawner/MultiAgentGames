package Analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import networking.server.ExperimentConfiguration;
import networking.server.GameHandler;
import burlap.behavior.stochasticgames.GameEpisode;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.state.GGAgent;
import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;


public class Analysis {
//	public static GridGame gridGame = new GridGame();
//	public static SGDomain domain = GridGameExtreme.generateDomain(gridGame, false);
//	public static StateJSONParser sp = new StateJSONParser(domain);
//	
//	public static class AgentTurn {
//		public String name;
//		public String turkId;
//		public String action;
//		public int x;
//		public int y;
//		public int rt;
//		public double reward;
//		
//		public void write(FileWriter writer) throws IOException {
//			writer.append(turkId).append(",").append(Integer.toString(x)).append(",");
//			writer.append(Integer.toString(y)).append(",").append(Integer.toString(rt));
//			writer.append(",");
//			if (action != null) {
//				writer.append(action);
//			} else {
//				writer.append("null");
//			}
//			
//		}
//	}
//	
//	public static class Turn {
//		public AgentTurn agent1 = new AgentTurn();
//		public AgentTurn agent2 = new AgentTurn();
//		public int turnNumber;
//		
//		public void write(FileWriter writer, String id, int roundNumber) throws IOException {
//			writer.append(id).append(",").append(Integer.toString(roundNumber)).append(",").append(Integer.toString(turnNumber)).append(",");
//			agent1.write(writer);
//			writer.append(",");
//			agent2.write(writer);
//			writer.append(",");
//			writer.append(Double.toString(agent1.reward));
//			writer.append(",");
//			writer.append(Double.toString(agent1.reward));
//			writer.append("\n");
//		}
//	}
//	
//	public static class Round implements Comparable<Round>{
//		public List<Turn> turns = new ArrayList<Turn>();
//		public double rewardAgent1 = 0;
//		public double rewardAgent2 = 0;
//		public int roundNumber;
//		public void write(FileWriter writer, String id) throws IOException {
//			for (Turn turn : turns) {
//				turn.write(writer, id, roundNumber);
//			}
//		}
//		@Override
//		public int compareTo(Round o) {
//			return Integer.compare(this.roundNumber, o.roundNumber);
//		}
//	}
//	
//	public static class Match {
//		public List<Round> rounds = new ArrayList<Round>();
//		public double rewardAgent1 = 0;
//		public double rewardAgent2 = 0;
//		public String turkId1;
//		public String turkId2;
//		public String matchId;
//		public void write(FileWriter writer) throws IOException {
//			for (Round round : rounds) {
//				round.write(writer, matchId);
//			}
//		}
//		
//		public void writeRewards(FileWriter writer) throws IOException {
//			writer.append(matchId).append(",").append(turkId1).append(",").append(Double.toString(rewardAgent1));
//			writer.append(",").append(turkId2).append(",").append(Double.toString(rewardAgent2));
//		}
//	}
//	
//	public static class Experiment {
//		public Map<String, Match> matches = new LinkedHashMap<String, Match>();
//		public void write(FileWriter writer) throws IOException {
//			for (Map.Entry<String, Match> entry : matches.entrySet()) {
//				Match match = entry.getValue();
//				match.write(writer);
//			}
//		}
//		
//		public void writeRewards(FileWriter writer) throws IOException {
//			for (Map.Entry<String, Match> entry : matches.entrySet()) {
//				Match match = entry.getValue();
//				match.writeRewards(writer);
//				writer.append("\n");
//			}
//		}
//	}
//	
//	public static Round getGameResult(String filename, List<GameEpisode> games) {
//		int s = filename.lastIndexOf("/") + 4;
//		int end = filename.lastIndexOf(".");
//		int roundNumber = Integer.parseInt(filename.substring(s+1,end));
//		String text;
//		try {
//			text = new String(Files.readAllBytes(Paths.get(filename)));
//		} catch (IOException e) {
//			e.printStackTrace();
//			return null;
//		}	
//		GameEpisode analysis = GameEpisode.legacyParseStringIntoGameEpisode(text, domain, sp);
//		if (analysis.states.isEmpty()) {
//			analysis = GameEpisode.read(filename);
//			if (analysis.states.isEmpty()) {
//				System.err.println(filename + " didn't contain a full game");
//				
//				return null;
//			}
//		}
//		games.add(analysis);
//		List<JointAction> actions = analysis.getJointActions();
//		List<State> states = analysis.getStates();
//		Round round = new Round();
//		round.roundNumber = roundNumber;
//		double sumReward1 = 0.0;
//		double sumReward2 = 0.0;
//		for (int i = 0; i < actions.size(); i++) {
//			JointAction action = actions.get(i);
//			State state = states.get(i);
//			Action agent1Action = action.action(0);
//			Action agent2Action = action.action(1);
//			
//			Turn turn = new Turn();
//			turn.agent1.name = agent1Action.actingAgent;
//			turn.agent1.action = agent1Action.actionName();
//			turn.agent2.name = agent2Action.actingAgent;
//			turn.agent2.action = agent2Action.actionName();
//			turn.agent1.x = state.getObject("agent0").getIntValForAttribute(GridGame.ATTX);
//			turn.agent1.y = state.getObject("agent0").getIntValForAttribute(GridGame.ATTY);
//			turn.agent2.x = state.getObject("agent1").getIntValForAttribute(GridGame.ATTX);
//			turn.agent2.y = state.getObject("agent1").getIntValForAttribute(GridGame.ATTY);
//			turn.turnNumber = i;
//			turn.agent1.reward = analysis.getRewardForAgent(i+1, "agent0");
//			turn.agent2.reward = analysis.getRewardForAgent(i+1, "agent1");
//			sumReward1 += analysis.getRewardForAgent(i+1, "agent0");
//			sumReward2 += analysis.getRewardForAgent(i+1, "agent1");
//			
//			round.turns.add(turn);
//		}
//		
//		State state = states.get(states.size() - 1);
//		Turn turn = new Turn();
//		turn.agent1.name = "agent0";
//		turn.agent1.action = null;
//		turn.agent2.name = "agent1";
//		turn.agent2.action = null;
//		turn.agent1.x = state.getObject("agent0").getIntValForAttribute(GridGame.VAR_X);
//		turn.agent1.y = state.getObject("agent0").getIntValForAttribute(GridGame.VAR_Y);
//		turn.agent2.x = state.getObject("agent1").getIntValForAttribute(GridGame.VAR_X);
//		turn.agent2.y = state.getObject("agent1").getIntValForAttribute(GridGame.VAR_Y);
//		turn.turnNumber = round.turns.size();
//		round.turns.add(turn);
//		round.rewardAgent1 = sumReward1;
//		round.rewardAgent2 = sumReward2;
//		return round;
//	}
//	
//
//	public static void addReactionTimes(String filename, Round round) {
//		List<String> lines;
//		try {
//			lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
//		} catch (IOException e) {
//			e.printStackTrace();
//			return;
//		}
//		int turnNum = 0;
//		String lastAgent = null;
//		for (String line : lines) {
//			String[] split = line.split(",");
//			String agent = split[1];
//			if (!agent.equals(lastAgent)) {
//				lastAgent = agent;
//				turnNum = 0;
//			}
//			String turkId = split[2];
//			//String turnStr = split[4];
//			String rtStr = split[5];
//			Turn turn = round.turns.get(turnNum);
//			AgentTurn agentTurn = (agent.equals("agent0")) ? turn.agent1 : turn.agent2;
//			agentTurn.rt = Integer.parseInt(rtStr);
//			agentTurn.turkId = turkId;
//			turnNum++;
//		}
//	}
//
//	public static Match getMatch(final String baseDir, String number, List<GameEpisode> games) {
//		final String filePattern = number;
//		File file = new File(baseDir);
//		FilenameFilter filter = new FilenameFilter() {
//			
//			@Override
//			public boolean accept(File dir, String name) {
//				return name.contains(filePattern) &&
//						name.contains(".game");
//			}
//		};
//		Match match = new Match();
//		String[] children = file.list(filter);
//		Integer matchNum = null;
//		for (int i = 0; i < children.length; ++i) {
//			if (matchNum == null) {
//				int s = children[i].indexOf("episode") + "episode".length();
//				int e = children[i].lastIndexOf("_");
//				matchNum = Integer.parseInt(children[i].substring(s,e));
//				
//			}
//			Round r = getGameResult(baseDir + "/" + children[i], games);
//			if (r != null) {
//				match.rounds.add(r);
//				match.rewardAgent1 += r.rewardAgent1;
//				match.rewardAgent2 += r.rewardAgent2;
//			}
//		}
//		if (match.rounds.isEmpty()) {
//			return null;
//		}
//		if (matchNum != null) {
//			match.matchId = matchNum.toString();
//		}
//		Collections.sort(match.rounds);
//		FilenameFilter filterRT = new FilenameFilter() {
//			
//			@Override
//			public boolean accept(File dir, String name) {
//				return name.contains(filePattern) &&
//						name.contains("reactionTimes");
//			}
//		};
//		
//		children = file.list(filterRT);
//		for (int i = 0; i < children.length; ++i) {
//			int s = children[i].lastIndexOf("_");
//			int e = children[i].lastIndexOf(".");
//			int roundNum = Integer.parseInt(children[i].substring(s+1,e))-1;
//			addReactionTimes(baseDir + "/" + children[i], match.rounds.get(roundNum));
//		}
//		if (match.rounds.size() > 0) {
//			match.turkId1 = match.rounds.get(0).turns.get(0).agent1.turkId;
//			match.turkId2 = match.rounds.get(0).turns.get(0).agent2.turkId;
//		}
//		
//		return match;
//	}
//	
//	// 1000009,name_2,agent0,human,2015_09_15_17_32_46_680_277064168
//	public static Experiment loadExperiment(String baseDir, String filename, List<List<GameEpisode>> games) {
//		Experiment experiment = new Experiment();
//		Path path = Paths.get(baseDir, filename).toAbsolutePath();
//		try {
//			BufferedReader reader = new BufferedReader(new FileReader(path.toString()));
//			String line;
//			
//			while ((line = reader.readLine()) != null) {
//				String[] split = line.split(",");
//				if (split.length != 5) {
//					continue;
//				}
//				
//				String filePrefix = split[4];
//				
//				if (!experiment.matches.containsKey(filePrefix)) {
//					List<GameEpisode> matchGames = new ArrayList<GameEpisode>();
//					games.add(matchGames);
//					
//					Match match = getMatch(baseDir, filePrefix, matchGames);
//					if (match != null) {
//						experiment.matches.put(filePrefix, match);
//					}
//				}
//			}
//			reader.close();
//		} catch (FileNotFoundException e) {
//			System.err.println("Failed to read file " + path.toString());
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return experiment;
//	}
//	
//	public static Match loadMatch(String dir, List<List<GameEpisode>> allGames) {
//		File file = new File(dir);
//		if (!file.isDirectory()) {
//			return null;
//		}
//		FilenameFilter filter = new FilenameFilter() {
//			
//			@Override
//			public boolean accept(File dir, String name) {
//				return name.contains(".game");
//			}
//		};
//		Match match = new Match();
//		String[] children = file.list(filter);
//		String filename = file.getName();
//		Integer matchNum = Integer.parseInt(filename.substring(5));
//		for (int i = 0; i < children.length; ++i) {
//			if (matchNum == null) {
//				int s = children[i].indexOf("episode") + "episode".length();
//				int e = children[i].lastIndexOf("_");
//				matchNum = Integer.parseInt(children[i].substring(s,e));
//				
//			}
//			List<GameEpisode> games = new ArrayList<GameEpisode>();
//			Round r = getGameResult(dir + "/" + children[i], games);
//			if (r != null) {
//				match.rounds.add(r);
//				match.rewardAgent1 += r.rewardAgent1;
//				match.rewardAgent2 += r.rewardAgent2;
//				allGames.add(games);
//			}
//		}
//		if (match.rounds.isEmpty()) {
//			return null;
//		}
//		if (matchNum != null) {
//			match.matchId = matchNum.toString();
//		}
//		
//		Collections.sort(match.rounds);
//		
//		return match;
//	}
//	
//	public static Experiment loadExperiment(String baseDir, List<List<GameEpisode>> games) {
//		File dir = new File(baseDir);
//		if (!dir.isDirectory()) {
//			throw new RuntimeException(baseDir + " is not a directory");
//		}
//		
//		File[] gameDirs = dir.listFiles();
//		Experiment exp = new Experiment();
//		for (File d : gameDirs) {
//			Match match = loadMatch(d.getAbsolutePath(), games);
//			if (match != null) {
//				exp.matches.put(d.getName(), match);
//			}
//		}
//		return exp;
//	}
//	
//	public static ObservedPolicy getPolicyFromGames(List<GameEpisode> games, String abstractedAgent, List<State> states) {
//		ObservedPolicy policy = new ObservedPolicy(abstractedAgent);
//		for (GameEpisode game : games) {
//			for (int i = 0; i < game.maxTimeStep(); i++) {
//				policy.addStateActionObservation(game.state(i), game.agentAction(i, abstractedAgent));
//				states.add(game.state(i));
//			}
//		}
//		
//		return policy;
//	}
//	
//	public static void visualizePolicy(List<List<State>> states, String abstractedAgent, List<ObservedPolicy> policies) {
//		Visualizer vis = GGVisualizer.getVisualizer(5, 3);
//		
//		PolicyGlyphPainter2D spp = ArrowActionGlyph.getNSEWPolicyGlyphPainter(GridGame.CLASS_AGENT, 
//				GridGame.VAR_X, GridGame.VAR_Y,
//				GridGame.ACTION_NORTH, GridGame.ACTION_SOUTH, GridGame.ACTION_EAST, GridGame.ACTION_WEST);
//		
//		spp.setXYAttByObjectReference(abstractedAgent, GridGame.ATTX, abstractedAgent, GridGame.ATTY);
//		spp.setNumXCells(5);
//		spp.setNumYCells(3);
//		PolicyRenderLayer pLayer = new PolicyRenderLayer(states.get(0), spp, policies.get(0));
//		
//		vis.addRenderLayer(pLayer);
//		
//		
//		String activeAgent = (abstractedAgent.equals("agent0")) ? "agent1" : "agent0";
//		SGPolicyExplorer exp = new SGPolicyExplorer(domain, vis, states.get(0).get(0), states, pLayer, policies, abstractedAgent);
//		exp.addKeyAction("w", activeAgent+":"+GridGame.ACTION_NORTH);
//		exp.addKeyAction("s", activeAgent+":"+GridGame.ACTION_SOUTH);
//		exp.addKeyAction("d", activeAgent+":"+GridGame.ACTION_EAST);
//		exp.addKeyAction("a", activeAgent+":"+GridGame.ACTION_WEST);
//		exp.addKeyAction("q", activeAgent+":"+GridGame.ACTION_NOOP);
//		exp.initGUI();
////		PolicyExplorerGUI gui = new PolicyExplorerGUI(vis, states, policy);
////		gui.updateState(states.get(0));
////		gui.initGUI();
////		ValueFunction vf = new ValueFunction() {
////			@Override
////			public double value(State s) {
////				return s.toString().length() % 2 == 0 ? 1.0 : 0.0;
////			}
////			
////		};
////		
////		PolicyGlyphPainter2D spp = ArrowActionGlyph.getNSEWPolicyGlyphPainter(GridGame.CLASSAGENT, 
////				GridGame.ATTX, GridGame.ATTY, 
////				GridGame.ACTIONNORTH, GridGame.ACTIONSOUTH, GridGame.ACTIONEAST, GridGame.ACTIONWEST);
////		
////		ValueFunctionVisualizerGUI gui = 
////				ValueFunctionVisualizerGUI.createGridGameSingleAgentVFVisualizerGUI(states, vf, policy);
////		gui.setPolicy(policy);
////		
////		gui.setSpp(spp);
////		gui.setState(0);
////		gui.initGUI();
//	}
//	
//	public static void write(String filename, String rewardsFilename, Experiment experiment) {
//		try {
//			FileWriter writer = new FileWriter(filename);
//			FileWriter rewardWriter = new FileWriter(rewardsFilename);
//			experiment.write(writer);
//			experiment.writeRewards(rewardWriter);
//			writer.close();
//			rewardWriter.close();
//		} catch (IOException e) {
//			System.err.println("Failed to write file");
//			e.printStackTrace();
//		}
//	}
//	
	public static void writeGameToFile(ExperimentConfiguration configuration, GameEpisode result, String path) {
		int matchNum = configuration.getCurrentMatchNumber();
		int roundNum = configuration.getCurrentMatch().getRoundNumber();
		String worldType = configuration.getCurrentMatch().getWorldWithAgents().toString().replace(",", " ");
		try {
			boolean writeHeaders = !Files.exists(Paths.get(path));
			FileWriter writer = new FileWriter(path, true);
			if (writeHeaders) {
				writer.append("Trial,Match,Round,Turn,agent1,agent1_x,agent1_y,agent1_rt,agent1_action,agent2,agent2_x,agent2_y,agent2_rt,agent2_action, agent1_reward, agent2_reward, joint_reward, grid_game\n");
			}
			List<JointAction> actions = result.jointActions;
			List<double[]> rewards = result.jointRewards;
			List<State> states = result.states;
			int i = 0;
			String participantId1 = null;
			String participantId2 = null;
			for (GameHandler handler : configuration.getCurrentMatch().getHandlerLookup().values()) {
				if (participantId1 == null) {
					participantId1 = handler.getParticipantId();
				} else if (participantId2 == null) {
					participantId2 = handler.getParticipantId();
				}
			}
			
			
			for (; i < actions.size(); i++) {
				JointAction action = actions.get(i);
				double[] reward = rewards.get(i); 
				State state = states.get(i);
				Analysis.writeLineToFile(state, action, matchNum, roundNum, i, reward, worldType, writer, participantId1, participantId2);
			}
			State finalState = states.get(states.size()-1);
			
			Analysis.writeLineToFile(finalState, null, matchNum, roundNum, i, null, worldType, writer, participantId1, participantId2);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file = new File(path);
		file.setExecutable(true, false);
		file.setReadable(true, false);
	}
	
	public static void writeLineToFile(State state, JointAction action, int matchNum, int roundNum, int turnNum, double[] reward, String worldType, FileWriter writer, String participant1Id, String participant2Id) throws IOException {
		List<ObjectInstance> agents = (List<ObjectInstance>)state.get(GridGame.CLASS_AGENT);
		GGAgent[] players = new GGAgent[2];
		
		for (ObjectInstance agent : agents) {
			GGAgent ggAgent = (GGAgent)agent;
			if (ggAgent.player < 2) {
				players[ggAgent.player] = ggAgent;
			}
		}
		int a1x = players[0].x;
		int a1y = players[0].y;
		int a2x = players[1].x;
		int a2y = players[1].y;
		int trialNum = 0;
		int reaction1 = 0;
		int reaction2 = 0;
		String action1 = (action == null) ? "null" : action.action(0).actionName();
		String action2 = (action == null) ? "null" : action.action(1).actionName();
		
		String agent1Reward = "0.0";
		String agent2Reward = "0.0";
		String totalReward = "0.0";
		if (reward != null) {
			agent1Reward = Double.toString(reward[0]);
			agent2Reward = Double.toString(reward[1]);
			totalReward = Double.toString(reward[0] + reward[1]);
		}
		writer.append(Integer.toString(trialNum)).append(",");
		writer.append(Integer.toString(matchNum)).append(",").append(Integer.toString(roundNum)).append(",");
		writer.append(Integer.toString(turnNum)).append(",");
		
		writer.append(participant1Id).append(",").append(Integer.toString(a1x)).append(",");
		writer.append(Integer.toString(a1y)).append(",").append(Integer.toString(reaction1)).append(",");
		writer.append(action1).append(",");
		
		writer.append(participant2Id).append(",").append(Integer.toString(a2x)).append(",");
		writer.append(Integer.toString(a2y)).append(",").append(Integer.toString(reaction2)).append(",");
		writer.append(action2).append(", ");
		writer.append(agent1Reward).append(",").append(agent2Reward).append(",");
		writer.append(totalReward).append(",").append(worldType).append("\n");
		
	}
//	
//	
//	
//	public static void main(String[] args) {
//		
//		List<List<GameEpisode>> games = new ArrayList<List<GameEpisode>>();
//		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//        String directory = (args.length == 0) ? null : args[0];
//        File file;
//		while (directory == null || !(file = new File(directory)).exists() || !file.isDirectory()) {
//			System.out.println("Enter an appropriate directory");
//			try {
//				directory = br.readLine();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}	
//		}
//		
//		String outfilename = (args.length < 2) ? null : args[1];
//		File outFile;
//		while (outfilename == null || (outFile = new File(outfilename)).exists()) {
//			System.out.println("Enter an appropriate results filename");
//			try {
//				outfilename = br.readLine();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}	
//		}
//		String rewardsFilename = (args.length < 3) ? null : args[2];
//		File rewardsOutfile;
//		while (rewardsFilename == null || (rewardsOutfile = new File(rewardsFilename)).exists()) {
//			System.out.println("Enter an appropriate rewards file name");
//			try {
//				rewardsFilename = br.readLine();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}	
//		}
//		
//		Experiment experiment = loadExperiment(file.getAbsolutePath(), games);
//		
//		
//		
//		/*
//		List<List<GameEpisode>> games = new ArrayList<List<GameEpisode>>();
//		Experiment experiment = loadExperiment(file.getAbsolutePath(), "IDMap.csv", games);
//		*/
////		for (String agent : Arrays.asList("agent0", "agent1")) {
////			List<ObservedPolicy> policies = new ArrayList<ObservedPolicy>();
////			List<List<State>> allStates = new ArrayList<List<State>>();
////			
////			for (int i = 7; i < 28; i++) {
////				List<GameEpisode> list = games.get(i);
////				List<State> states = new ArrayList<State>();
////				ObservedPolicy policy = getPolicyFromGames(list, agent, states);
////				policies.add(policy);
////				allStates.add(states);
////			}
////			visualizePolicy(allStates, agent, policies);
////		}
//		
//		write(outFile.getAbsolutePath(), rewardsOutfile.getAbsolutePath(), experiment);
//		
//	}
}
