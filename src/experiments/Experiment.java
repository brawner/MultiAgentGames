package experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import networking.common.GridGameExperimentToken;
import networking.common.GridGameServerToken;
import networking.common.GridGameWorldLoader;
import networking.common.Token;
import networking.common.TokenCastException;
import networking.server.GridGameManager;
import burlap.domain.stochasticdomain.gridgame.GridGameStandardMechanicsWithoutTieBreaking;
import burlap.domain.stochasticdomain.world.NetworkWorld;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.state.GGAgent;
import burlap.domain.stochasticgames.gridgame.state.GGGoal;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.oo.OODomain;
import burlap.mdp.core.oo.state.generic.GenericOOState;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.SGDomain;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.agent.SGAgentGenerator;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import burlap.mdp.stochasticgames.world.World;

public class Experiment {

	private static final String AGENTS = "agents";

	private static final String PARAMS = "params";

	String uniqueId;

	String paramFilesFolder;
	String gamesFolder;
	String outputFolder;


	public SGDomain sgDomain;
	public TerminalFunction tf;
	public JointRewardFunction jr;
	public List<SGAgentType> types;
	private SGAgentGenerator agentGenerator;
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

	
	public Experiment(String experimentFile, String paramFilesFolder, String gamesFolder, String outputFolder, SGAgentGenerator agentGenerator, int numSamples, String trial) {
		// Initialize lists.
		this.paramFilesFolder = paramFilesFolder;
		this.gamesFolder = gamesFolder;
		this.outputFolder = outputFolder;
		this.agentKindLists = new ArrayList<List<String>>();
		this.paramFileLists = new ArrayList<List<String>>();
		this.numRounds = new ArrayList<Integer>();
		this.games = new ArrayList<String>();
		this.numMatches = 0;
		this.agentGenerator = agentGenerator;


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
		this.tf = new GridGame.GGTerminalFunction((OODomain) sgDomain);
		this.jr = new GridGame.GGJointRewardFunction((OODomain) sgDomain, 0, 10, true);	
		// DPrint.toggleCode(this.w.getDebugId(), false);
		// TODO: fix this...Or is it supposed include other types??? Where is it used?
		this.types = new ArrayList<SGAgentType>();
		types.add(GridGame.getStandardGridGameAgentType(this.sgDomain));
		types.add(GridGame.getStandardGridGameAgentType(this.sgDomain));
		// create objects given types from experiment file
		for (int match = 0; match < numMatches; match++) {
			//System.out.println(paramFileLists.get(match).size());
			agentLists.add(match, makeAgents(agentKindLists, paramFileLists, match,paramFilesFolder));
			startingStates.add(match, makeState(games.get(match), gamesFolder));

		}



	}


	private void readJSONExperimentFile(String experimentFile) {


		
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
		//System.out.println("Num matches: "+numMatches);
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
		NetworkWorld world = GridGameWorldLoader.loadWorld(token);
		State state = world.startingStateWithoutNames(); 

		return state;
	}

	private List<SGAgent> makeAgents(List<List<String>> agentKindsList,List<List<String>> paramFilesLists, int match, String paramFilesFolder) {
		List<SGAgent> matchAgents = new ArrayList<SGAgent>();


		//System.out.println("Num agents: "+agentKindsList.get(match).size());
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
		return this.agentGenerator.generateAgent(agentKind, parametersFile);
	}

	public State getHallwayState() {
		//TODO: This needs to be generalized for all of our games.
		GenericOOState s =  new GenericOOState(
				new GGAgent(0, 1, 0, "agent0"),
				new GGAgent(4, 1, 1, "agent1"),
				new GGGoal(0, 1, 1, "g0"),
				new GGGoal(1, 4, 2, "g1")
		);

		GridGame.setBoundaryWalls(s, 5, 3);

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



//	public String comparePolicies(String sourceFolder, int matchLearned, int matchCorrect, boolean useKL, 
//			boolean useImpSampling, TerminalFunction tf) {
//		System.out.println("Comparing policies started");
//		HumanRobotPolicySimilarityMetric metricCalc = new HumanRobotPolicySimilarityMetric();
//		if(agentKindLists.get(matchLearned).get(0).compareTo("norm_learning")==0 
//				&& agentKindLists.get(matchCorrect).get(0).compareTo("fixed_policy")==0){
//			if(useKL){
//				NormLearningAgent learnedAgent = (NormLearningAgent)(agentLists.get(matchLearned).get(0));
//				NormSetStrategyAgent setAgent = (NormSetStrategyAgent)agentLists.get(matchCorrect).get(0);
//
//				NormJointPolicy setPolicy = setAgent.getPolicy();
//				setPolicy.setNoislessPolicy();
//
//				PolicyComparisonWithKLDivergence klMetric =
//						new PolicyComparisonWithKLDivergence(setPolicy, learnedAgent.getJointPolicy(), 
//								startingStates.get(matchLearned),learnedAgent.getCmdpDomain());
//				double value = klMetric.runPolicyComparison();
//				System.out.println("VALUE: "+value);
//				System.out.println("Comparing policies ended");
//				return value+"";
//			}else if(useImpSampling){
//				System.out.println("Using Imp sampling");
//				NormLearningAgent learnedAgent = (NormLearningAgent)(agentLists.get(matchLearned).get(0));
//				NormSetStrategyAgent setAgent = (NormSetStrategyAgent)agentLists.get(matchCorrect).get(0);
//
//				NormJointPolicy setPolicy = setAgent.getPolicy();
//				setPolicy.setNoislessPolicy();
//				System.out.println("set noiseless");
//				ImportanceSamplingBasedTrajectoryKLDivergence impSamp = 
//						new ImportanceSamplingBasedTrajectoryKLDivergence(setPolicy, learnedAgent.getJointPolicy(), 
//								startingStates.get(matchLearned), learnedAgent.getCmdpDomain(), tf);
//				System.out.println("running comparison");
//				double result = impSamp.runPolicyComparison(12000);
//				System.out.println("Comparing policies ended");
//				return result+"";
//			}else{
//				double[] metrics = metricCalc.calculateMetric(sourceFolder, matchCorrect, matchLearned, false);
//				System.out.println("VALUE: "+metrics[2]);
//				System.out.println("Comparing policies ended");
//				return metrics[2]+","+metrics[3];
//				//System.out.println("VALUE: "+value);
//				//return value;
//			}
//		}else if(agentKindLists.get(matchLearned).get(0).compareTo("norm_learning")==0 
//				&& agentKindLists.get(matchCorrect).get(0).compareTo("copy_agent")==0){
//			// norm and copy
//			double[] metrics = metricCalc.calculateMetric(sourceFolder, matchCorrect, matchLearned, false);
//			System.out.println("Comparing policies ended");
//			return metrics[2]+","+metrics[3];
//		}else {
//			System.out.println("Comparing policies ended");
//			return "-1";
//		}
//
//	}


}
