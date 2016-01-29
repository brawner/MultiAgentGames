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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import networking.common.GridGameExperimentToken;
import networking.common.GridGameServerToken;
import networking.common.GridGameWorldLoader;
import networking.common.Token;
import networking.common.TokenCastException;
import networking.server.ExperimentConfiguration;
import networking.server.GridGameManager;
import networking.server.MatchConfiguration;
import Analysis.PolicyComparisonWithKLDivergence;
import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.stochasticgames.agents.normlearning.NormLearningAgent;
import burlap.behavior.stochasticgames.agents.normlearning.NormLearningAgentFactory;
import burlap.behavior.stochasticgames.agents.normlearning.baselines.BaselineAgentFactory;
import burlap.behavior.stochasticgames.agents.normlearning.baselines.TeamPolicyBaseline;
import burlap.behavior.stochasticgames.agents.normlearning.modelbasedagents.ModelBasedLearningAgent;
import burlap.behavior.stochasticgames.agents.normlearning.setpolicyagents.NormSetStrategyAgent;
import burlap.behavior.stochasticgames.agents.normlearning.setpolicyagents.NormSetStrategyAgentFactory;
import burlap.behavior.stochasticgames.agents.normlearning.utilityagents.CopyGameFilesAgent;
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

	private static final String AGENTS = "agents";

	private static final String PARAMS = "params";

	String uniqueId;

	String paramFilesFolder;
	String gamesFolder;
	String outputFolder;
	

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

	private Integer DEFAULT_MAX_TURNS = 20;

	private Integer DEFAULT_MAX_ROUNDS = 20;

	private int numSamples = -1;
	private String trial;

	public Experiment(String experimentFile, String paramFilesFolder, String gamesFolder, String outputFolder, int numSamples, String trial) {
		// Initialize lists.
		this.paramFilesFolder = paramFilesFolder;
		this.gamesFolder = gamesFolder;
		this.outputFolder = outputFolder;
		this.agentKindLists = new ArrayList<List<String>>();
		this.paramFileLists = new ArrayList<List<String>>();
		this.numRounds = new ArrayList<Integer>();
		this.games = new ArrayList<String>();
		this.numMatches = 0;
		this.numSamples =numSamples;
		this.trial = trial;
		

		if(experimentFile.split("\\.")[1].compareTo("csv")==0){
			// Read in experiment parameters from experimentFile.
			readExperimentFile(experimentFile);
		} else {
			readJSONExperimentFile(experimentFile);
		}
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
		types.add(GridGame.getStandardGridGameAgentType(this.sgDomain));
		types.add(GridGame.getStandardGridGameAgentType(this.sgDomain));
		// create objects given types from experiment file
		for (int match = 0; match < numMatches; match++) {
			System.out.println(paramFileLists.get(match).size());
			agentLists.add(match, makeAgents(agentKindLists, paramFileLists, match,paramFilesFolder));
			startingStates.add(match, makeState(games.get(match), gamesFolder));

		}



	}


	private void readJSONExperimentFile(String experimentFile) {


		List<String> params = new ArrayList<String>();

		BufferedReader reader;
		FileReader fileReader;
		try {
			fileReader = 
					new FileReader(experimentFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("The file " + experimentFile + " somehow does not exist");
		}
		StringBuilder builder = new StringBuilder();
		try {
			String line = "";

			reader = new BufferedReader(fileReader);

			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			reader.close();
		} catch (IOException e) {

		} 
		String text = builder.toString();

		//now parse the text
		GridGameExperimentToken token = GridGameExperimentToken.tokenFromJSONString(text);
		List<GridGameExperimentToken> matches;
		try {
			matches = token.getTokenList(GridGameExperimentToken.MATCHES);

			for (GridGameExperimentToken match : matches) {
				if(match.getInt(GridGameExperimentToken.MAX_TURNS)!=null){
					this.maxTurns = match.getInt(GridGameExperimentToken.MAX_TURNS);
				}else{
					this.maxTurns = DEFAULT_MAX_TURNS;
				}
				this.numMatches++;
				if(match.getInt(GridGameExperimentToken.MAX_ROUNDS)!=null){
					numRounds.add(Integer.valueOf(match.getInt(GridGameExperimentToken.MAX_ROUNDS)));
				}else{
					numRounds.add(DEFAULT_MAX_ROUNDS);
				}

				games.add(match.getString(GridGameManager.WORLD_ID));


				List<String> agentKinds = new ArrayList<String>();
				List<String> agentParams = new ArrayList<String>();	

				List<GridGameExperimentToken> agentTokens = match.getTokenList(AGENTS);
				if (agentTokens == null) {
					System.err.println("Agent tokens were not specified");
				}
				for (Token agentToken : agentTokens) {
					String agentTypeStr;
					agentTypeStr = agentToken.getString(GridGameManager.AGENT_TYPE);

					if (agentTypeStr == null) {
						System.err.println("Agent's type was not specified");
					}
					String paramFile = agentToken.getString(PARAMS);
					if (paramFile != null) {
						Path path = Paths.get(paramFilesFolder, paramFile + ".properties");
						if (!Files.exists(path)) {
							System.err.println(path.toString() + " does not exist");
						}

					}
					agentKinds.add(agentTypeStr);
					agentParams.add(paramFile);

				}
				agentKindLists.add(agentKinds);
				paramFileLists.add(agentParams);

			}
		} catch (TokenCastException e) {
			e.printStackTrace();
			System.out.println("ERROR!!");

		}
		System.out.println("Num matches: "+numMatches);
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
		World world = GridGameWorldLoader.loadWorld(token);
		State state = world.startingState(); 

		return state;
	}

	private List<SGAgent> makeAgents(List<List<String>> agentKindsList,List<List<String>> paramFilesLists, int match, String paramFilesFolder) {
		List<SGAgent> matchAgents = new ArrayList<SGAgent>();


		System.out.println("Num agents: "+agentKindsList.get(match).size());
		for(int agent = 0; agent< agentKindsList.get(match).size();agent++){
			if(match==0){
				matchAgents.add(findAndCreateAgentOfKind(agentKindsList.get(match).get(agent),
						paramFilesFolder+paramFilesLists.get(match).get(agent), outputFolder));

			}else if(agentKindsList.get(match).get(agent).compareTo(agentKindsList.get(match-1).get(agent))==0 &&
					paramFilesLists.get(match).get(agent).compareTo(paramFilesLists.get(match-1).get(agent))==0){
				matchAgents.add(agentLists.get(match-1).get(agent));

			}else{
				matchAgents.add(findAndCreateAgentOfKind(agentKindsList.get(match).get(agent), 
						paramFilesFolder+paramFilesLists.get(match).get(agent),outputFolder));
			}
		}
		return matchAgents;
	}



	private SGAgent findAndCreateAgentOfKind(String agentKind, String parametersFile, String outputFile) {


		// http://i.imgur.com/9G9h8dt.jpg
		switch (agentKind){
		case "norm_learning":
			System.out.println("NumSamples Exp: "+this.numSamples);
			return NormLearningAgentFactory.getNormLearningAgent(parametersFile, outputFile, trial, this.numSamples, this.sgDomain, this.types, this.jr, this.tf);
		case "fixed_policy":
			
			return NormSetStrategyAgentFactory.getSetStrategyAgent(parametersFile, this.sgDomain);
		case "model_based":
			//TODO: actually create this agent
			return new ModelBasedLearningAgent();
		case "baseline":
			//TODO: actually create this agent
			return BaselineAgentFactory.getBaselineAgent(parametersFile, this.sgDomain, this.types, this.jr, this.tf);
		case "CD":
			return null;
		case "human":
			return null;
		case "copy_agent":
			return new CopyGameFilesAgent(parametersFile,outputFile, this.sgDomain);
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



	public void comparePolicies(String outputLoc, int matchLearned, int matchCorrect) {
		// TODO Auto-generated method stub
		if(agentKindLists.get(matchLearned).get(0).compareTo("norm_learning")==0 
				&& agentKindLists.get(matchCorrect).get(0).compareTo("fixed_policy")==0){
			NormLearningAgent learnedAgent = (NormLearningAgent)(agentLists.get(matchLearned).get(0));
			NormSetStrategyAgent setAgent = (NormSetStrategyAgent)agentLists.get(matchCorrect).get(0);

			PolicyComparisonWithKLDivergence klMetric =
					new PolicyComparisonWithKLDivergence(setAgent.getPolicy(), setAgent.getPolicy(), 
							startingStates.get(matchLearned),learnedAgent.getCmdpDomain());
			double value = klMetric.runPolicyComparison();
			System.out.println("VALUE: "+value);
		}
		
	}


}
