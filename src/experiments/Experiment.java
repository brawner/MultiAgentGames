package experiments;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.io.File;

import networking.common.GridGameServerToken;
import networking.common.GridGameWorldLoader;
import networking.common.TokenCastException;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.stochasticgames.agents.normlearning.NormLearningAgentFactory;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanicsWithoutTieBreaking;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.states.State;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGAgent;
import burlap.oomdp.stochasticgames.SGAgentType;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;

public class Experiment {
	
	String uniqueId;

	public SGDomain sgDomain;
	public TerminalFunction tf;
	public JointReward jr;
	public List<SGAgentType> types;

	GridGame gg;

	List<List<String>> agentKindLists;
	List<List<String>> paramFileLists;

	List<String> games;
	List<Integer> numRounds;

	int numMatches;

	List<List<SGAgent>> agentLists;
	List<State> startingStates; // aka the games
	public int maxTurns;

	public Experiment(String experimentFile, String paramFilesFolder, String gamesFolder) {
		// Initialize lists.
		
		this.agentKindLists = new ArrayList<List<String>>();
		this.paramFileLists = new ArrayList<List<String>>();
		this.numRounds = new ArrayList<Integer>();
		this.games = new ArrayList<String>();
		this.numMatches = 0;

		// Read in experiment parameters from experimentFile.
		readExperimentFile(experimentFile);

		agentLists = new ArrayList<List<SGAgent>>();
		startingStates = new ArrayList<State>();
		
		// set basics for all agents (TODO some of this shouldn't be here?)
		this.gg = new GridGame();
		this.sgDomain = (SGDomain) gg.generateDomain();
		this.sgDomain
		.setJointActionModel(new GridGameStandardMechanicsWithoutTieBreaking(
				sgDomain, .5));
		this.tf = new GridGame.GGTerminalFunction(sgDomain);
		this.jr = new GridGame.GGJointRewardFunction(sgDomain, 0, 10, true);	
		// DPrint.toggleCode(this.w.getDebugId(), false);
		// TODO: fix this...Or is it supposed include other types??? Where is it used?
		this.types = new ArrayList<SGAgentType>();

		// create objects given types from experiment file
		for (int match = 0; match < numMatches; match++) {
			agentLists.add(match, makeAgents(agentKindLists.get(match), paramFileLists.get(match),paramFilesFolder));
			startingStates.add(match, makeState(games.get(match), gamesFolder));
			
		}

	

	}
	
	
	
	private void readExperimentFile(String experimentFile){
		File expSetup = new java.io.File(experimentFile);
		String line = "";
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(expSetup));

			// Comment this part out until we include a header with
			// experiment/game parameters.
			// line = br.readLine();
			// line.split(",");
			// Parse the line accordingly.
			this.maxTurns = 20;

			while ((line = br.readLine()) != null) {
				this.numMatches++;
				String[] matchLine = line.split(",");
				
				numRounds.add(Integer.valueOf(matchLine[0])/2); // EDITIED HERE
				games.add(matchLine[1]);
				List<String> agentKinds = new ArrayList<String>();
				agentKinds.add(matchLine[2]);
				agentKinds.add(matchLine[3]);
				agentKindLists.add(agentKinds);
				List<String> agentParams = new ArrayList<String>();
				agentParams.add(matchLine[4]);
				agentParams.add(matchLine[5]);
				paramFileLists.add(agentParams);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private State makeState(String gameName, String gamesFolder) {
		
		GridGameServerToken token = GridGameWorldLoader.loadText(gamesFolder+"/"+gameName+".json");
		try {
			String name = token.getString("label");
		} catch (TokenCastException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		World world = GridGameWorldLoader.loadWorld(token);
		State state = world.startingState(); 
		
		return state;
	}

	private List<SGAgent> makeAgents(List<String> agentKinds, List<String> paramFiles, String paramFilesFolder) {
		List<SGAgent> matchAgents = new ArrayList<SGAgent>();

		for(int agent = 0; agent< agentKinds.size();agent++){
			matchAgents.add(findAndCreateAgentOfKind(agentKinds.get(agent), paramFilesFolder+paramFiles.get(agent)));
		}
		return matchAgents;
	}



	private SGAgent findAndCreateAgentOfKind(String agentKind, String parametersFile) {
		
		types.add(GridGame.getStandardGridGameAgentType(this.sgDomain));
		// http://i.imgur.com/9G9h8dt.jpg
		switch (agentKind){
		case "norm_learning":
			return NormLearningAgentFactory.getNormLearningAgent(parametersFile, this.sgDomain, this.types, this.jr, this.tf);
		case "model_based":
			return null;
		case "CD":
			return null;
		case "human":
			return null;
		default:
			return null;
		}
	
	}
	
	public State getHallwayState() {
		//TODO: This needs to be generalized for all of our games.
		State s = GridGame.getCleanState(sgDomain, 2, 2, 2, 2, 5, 3);
		GridGame.setAgent(s, 0, 0, 1, 0);
		GridGame.setAgent(s, 1, 4, 1, 1);

		GridGame.setGoal(s, 0, 0, 1, 2);
		GridGame.setGoal(s, 1, 4, 1, 1);

		return s;
	}

	
	public SGAgent getAgent(int match, int agentNum) {
		return agentLists.get(match).get(agentNum);
	}

	public int getNumRounds(int match) {
		return numRounds.get(match);
	}

	public World getWorld(int match) {
		return new World(sgDomain, jr, tf, startingStates.get(match));
	}


}
