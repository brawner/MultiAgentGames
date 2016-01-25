package experiments;

import Analysis.Analysis;
import burlap.behavior.policy.GreedyQPolicy;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.stochasticgames.GameAnalysis;
import burlap.behavior.stochasticgames.agents.HumanAgent;
import burlap.behavior.stochasticgames.agents.normlearning.NormLearningAgent;
import burlap.behavior.stochasticgames.auxiliary.GameSequenceVisualizer;
import burlap.behavior.stochasticgames.auxiliary.jointmdp.CentralizedDomainGenerator;
import burlap.behavior.stochasticgames.auxiliary.jointmdp.TotalWelfare;
import burlap.domain.stochasticgames.gridgame.GGAltVis;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.states.State;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.statehashing.SimpleHashableStateFactory;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.SGAgent;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.visualizer.Visualizer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import networking.server.ExperimentConfiguration;
import examples.GridGameNormRF2;

/**
 * @author Betsy Hilliard, Carl Trimbach, based on example by James MacGlashan
 */
public class NormLearnersExperiment {

	Experiment experiment;

	List<List<GameAnalysis>> results;

	public NormLearnersExperiment(Experiment experiment) {
		this.experiment = experiment;
	}


	public static void writeGameToFile(GameAnalysis result, String path, int matchNum, int round, String worldType) {
		try {
			File f = new File(path);
			f.getParentFile().mkdirs();
			FileWriter writer = new FileWriter(path, true);
			writer.append("Match,Round,Turn,agent1,agent1_x,agent1_y,agent1_rt,agent1_action,agent2,agent2_x,agent2_y,agent2_rt,agent2_action\n");
			List<JointAction> actions = result.jointActions;

			List<Map<String,Double>> rewards = result.jointRewards;

			List<State> states = result.states;
			int i = 0;
			for (; i < actions.size(); i++) {
				JointAction action = actions.get(i);
				State state = states.get(i);
				Map<String,Double> rewardMap = rewards.get(i);
				Analysis.writeLineToFile(state, action, matchNum, round, i,  rewardMap, worldType, writer);
			}

			State finalState = states.get(states.size()-1);
			Map<String,Double> rewardMap = rewards.get(rewards.size()-1);
			Analysis.writeLineToFile(finalState, null, matchNum, round, i, rewardMap, worldType, writer);
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		File file = new File(path);
		file.setExecutable(true, false);
		file.setReadable(true, false);
	}

	public void runExperiment(int trial,String outputFolder) {


		results = new ArrayList<List<GameAnalysis>>();

		for (int match = 0; match < experiment.numMatches; match++) {

			// create agents
			SGAgent agent0 = experiment.getAgent(match, 0);
			SGAgent agent1 = experiment.getAgent(match, 1);

			World w = experiment.getWorld(match);
			// have them join the world
			// what the heck?
			agent0.joinWorld(w, experiment.types.get(0));
			agent1.joinWorld(w, experiment.types.get(0));

			// play games
			ArrayList<GameAnalysis> matchList = new ArrayList<GameAnalysis>();
			for (int round = 0; round < experiment.getNumRounds(match); round++) {
				System.out.println("running round " + round + " of match "+match);
				GameAnalysis game = w.runGame(experiment.maxTurns);
				matchList.add(game);

				writeGameToFile(game,outputFolder+"/trial_"+trial+"_summary.csv", match, round, experiment.games.get(match));
			}
			results.add(matchList);

		}


	}

	public void visualizeResults(){

		// visualize results

		Visualizer v = GGAltVis.getVisualizer(7, 6);
		List<GameAnalysis> gamesToSee = new ArrayList<GameAnalysis>();
		for(List<GameAnalysis> games : results){
			gamesToSee.addAll(games);
		}
		new GameSequenceVisualizer(v, this.experiment.sgDomain, gamesToSee);

	}

	public void runHumanNormAgent() {

		Visualizer v = GGAltVis.getVisualizer(5, 5);
		HumanAgent human = new HumanAgent(v);
		human.addKeyAction("w",
				this.experiment.sgDomain.getSingleAction(GridGame.ACTIONNORTH)
				.getAssociatedGroundedAction(""));
		human.addKeyAction("s",
				this.experiment.sgDomain.getSingleAction(GridGame.ACTIONSOUTH)
				.getAssociatedGroundedAction(""));
		human.addKeyAction("d",
				this.experiment.sgDomain.getSingleAction(GridGame.ACTIONEAST)
				.getAssociatedGroundedAction(""));
		human.addKeyAction("a",
				this.experiment.sgDomain.getSingleAction(GridGame.ACTIONWEST)
				.getAssociatedGroundedAction(""));
		human.addKeyAction("x",
				this.experiment.sgDomain.getSingleAction(GridGame.ACTIONNOOP)
				.getAssociatedGroundedAction(""));

		// convert grid game to a centralized MDP (which will be used to define
		// social reward functions)
		CentralizedDomainGenerator mdpdg = new CentralizedDomainGenerator(
				experiment.sgDomain, experiment.types);
		Domain cmdp = mdpdg.generateDomain();
		RewardFunction crf = new TotalWelfare(experiment.jr);

		// create joint task planner for social reward function and RHIRL leaf
		// node values
		final SparseSampling jplanner = new SparseSampling(cmdp, crf,
				experiment.tf, 0.99, new SimpleHashableStateFactory(), 20, -1);
		jplanner.toggleDebugPrinting(false);

		// create independent social reward functions to learn for each agent
		final GridGameNormRF2 agent1RF = new GridGameNormRF2(crf,
				new GreedyQPolicy(jplanner), this.experiment.sgDomain);

		NormLearningAgent agent1 = new NormLearningAgent(
				this.experiment.sgDomain, agent1RF, -1,
				agent1RF.createCorresponingDiffVInit(jplanner), false);

		// have them join the world
		World w = experiment.getWorld(0);
		human.joinWorld(w, this.experiment.types.get(0));
		agent1.joinWorld(w, this.experiment.types.get(0));

		// play games
		results = new ArrayList<List<GameAnalysis>>();
		ArrayList<GameAnalysis> gameList = new ArrayList<GameAnalysis>();
		for (int i = 0; i < 5; i++) {
			System.out.println("running game " + i);
			GameAnalysis game = w.runGame(20);
			gameList.add(game);
		}
		results.add(gameList);
	}

	private void outputTrialResults(String outputFolder) {

		int match = 0;
		for(List<GameAnalysis> list : results){
			int round = 0;
			for (GameAnalysis ga : list) {
				ga.writeToFile(outputFolder + "match_"+match+"_round_" + round);
				round++;
			}
			match++;
		}
	}

	private static Map<String, String> parseArguments(String[] args){
		HashMap<String, String> arguments = new HashMap<String,String>();
		arguments.put("numTrials", "2");

		arguments.put("experiment", "corner_2");
		arguments.put("outputF","/grid_games/results/");
		arguments.put("gamesF","/resources/worlds");
		arguments.put("paramF","/resources/parameters/");
		arguments.put("experimentF", "/resources/experiments/");

		for(int i = 0; i<args.length;i++){
			if (args[i].startsWith("--")){
				String key = args[i].substring(2);
				if (arguments.containsKey(key)){
					arguments.put(key, args[i+1]);
					i++;
				}
			}
		}
		return arguments;
	}

	private static String generateUniqueID(String expName) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SS");
		Date date = new Date();
		String dateStr = dateFormat.format(date);
		Random rand = new Random();
		String randVal = Integer.toString(rand.nextInt(Integer.MAX_VALUE));

		return dateStr+"/"+expName;
	}

	public static void main(String[] args) {
		boolean visualize = true;
		String currDir = System.getProperty("user.dir");
		Map<String, String> arguments = parseArguments(args);

		int numTrials = Integer.parseInt(arguments.get("numTrials")); //from args
		String[] experiments = {"Test_CodedNorms.json"}; 
		//"hall_1","hall_2","door","tunnels","manners","corner_1","corner_2","hall_pair","corner_pair"
		for(int e =0; e<experiments.length;e++){
			String experimentFile = experiments[e]; //arguments.get("experiment"); //from args
			if(!experimentFile.contains(".json")){
				experimentFile+=".csv";

			}

			// some other folder locations here
			String outputFolder = currDir+arguments.get("outputF"); //from args
			String paramFilesFolder = currDir+arguments.get("paramF"); //from args
			String experimentFolder = currDir+arguments.get("experimentF"); //from args
			String gamesFolder = currDir+arguments.get("gamesF");

			experimentFile = experimentFolder + experimentFile;
			String uniqueId = NormLearnersExperiment.generateUniqueID(experiments[e].split("\\.")[0]); //edited here too

			for (int trial = 0; trial < numTrials; trial++) {
				Experiment experiment = new Experiment(experimentFile,
						paramFilesFolder, gamesFolder);
				NormLearnersExperiment ex = new NormLearnersExperiment(experiment);

				ex.runExperiment(trial, outputFolder+uniqueId);
				ex.outputTrialResults(outputFolder+uniqueId+"/trial_"+trial+"_");
				ex.visualizeResults();



			}
		}
	}



}
