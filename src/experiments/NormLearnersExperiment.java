package experiments;

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

import Analysis.Analysis;
import burlap.behavior.stochasticgames.GameAnalysis;
import burlap.behavior.stochasticgames.auxiliary.GameSequenceVisualizer;
import burlap.domain.stochasticgames.gridgame.GGAltVis;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.objects.ObjectInstance;
import burlap.oomdp.core.states.State;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.SGAgent;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.visualizer.Visualizer;

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
			writer.append("Trial,Match,Round,Turn,agent1,agent1_x,agent1_y,agent1_rt,agent1_action,agent2,agent2_x,"
					+ "agent2_y,agent2_rt,agent2_action,agent1_rw,agent2_rw, joint_rw, world\n");
			List<JointAction> actions = result.jointActions;

			List<Map<String,Double>> rewards = result.jointRewards;

			List<State> states = result.states;
			int i = 0;
			
			String participantId1 = "agent0";
			String participantId2 = "agent1";
			List<ObjectInstance> agents = states.get(0).getObjectsOfClass(GridGame.CLASSAGENT);
			for (ObjectInstance agent : agents) {
				if (agent.getIntValForAttribute(GridGame.ATTPN) == 0) {
					participantId1 = agent.getName();
				} else if (agent.getIntValForAttribute(GridGame.ATTPN)== 1 ) {
					participantId2 = agent.getName();
				}
			}
			
			for (; i < actions.size(); i++) {
				JointAction action = actions.get(i);
				State state = states.get(i);
				Map<String,Double> rewardMap = rewards.get(i);
				Analysis.writeLineToFile(state, action, matchNum, round, i,  rewardMap, worldType, writer, participantId1, participantId2);
			}

			State finalState = states.get(states.size()-1);
			Map<String,Double> rewardMap = rewards.get(rewards.size()-1);
			Analysis.writeLineToFile(finalState, null, matchNum, round, i, rewardMap, worldType, writer, participantId1, participantId2);

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		File file = new File(path);
		file.setExecutable(true, false);
		file.setReadable(true, false);
	}

	public String runExperiment(int trial,int numSamples, String outputFolder) {

		String toPrint = "";
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
				//System.out.println("running round " + round + " of match "+match);
				GameAnalysis game = w.runGame(experiment.maxTurns);
				matchList.add(game);

				writeGameToFile(game,outputFolder+"/trial_"+trial+"_summary.csv", match, round, experiment.games.get(match));
				
			}
			//comparePolicies here somehow? 
			
			outputMatchResults(outputFolder+"/trial_"+trial+"_", matchList, match);
			
			double comparisonVal = experiment.comparePolicies(outputFolder+"/trial_"+trial+"_", match, match-1);
			if(comparisonVal>=0.0){
				toPrint+=trial+","+numSamples+","+comparisonVal+"\n";
			}

			//			double comparisonVal = experiment.comparePolicies(outputFolder+"/trial_"+trial+"_", match, match-1);
			//			if(comparisonVal>=0.0){
			//				toPrint+=trial+","+numSamples+","+comparisonVal+"\n";
			//			}
			results.add(matchList);

		}
		return toPrint;

	}

	public void visualizeResults(boolean visualize){
		if(visualize){
			// visualize results
			Visualizer v = GGAltVis.getVisualizer(7, 6);
			List<GameAnalysis> gamesToSee = new ArrayList<GameAnalysis>();
			for(List<GameAnalysis> games : results){
				gamesToSee.addAll(games);
			}
			new GameSequenceVisualizer(v, this.experiment.sgDomain, gamesToSee);
		}

	}

	private void outputTrialResults(String outputFolder) {
		//System.out.println("Outputting here: "+outputFolder);
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

	private void outputMatchResults(String outputFolder, List<GameAnalysis> rounds, int matchNum) {
		//System.out.println("Outputting here: "+outputFolder);

		int round = 0;
		for (GameAnalysis ga : rounds) {
			ga.writeToFile(outputFolder + "match_"+matchNum+"_round_" + round);
			round++;
		}
		round++;
	}


	private static Map<String, String> parseArguments(String[] args){
		HashMap<String, String> arguments = new HashMap<String,String>();
		arguments.put("numTrials", "1");
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

	private static String generateUniqueID() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SS");
		Date date = new Date();
		String dateStr = dateFormat.format(date);
		Random rand = new Random();
		String randVal = Integer.toString(rand.nextInt(Integer.MAX_VALUE));

		return dateStr;
	}

	public static void main(String[] args) {
		boolean visualize = true;
		int maxNumSamples = 25;
		int minNumSamples = 25;
		String currDir = System.getProperty("user.dir");
		Map<String, String> arguments = parseArguments(args);

		int numTrials = Integer.parseInt(arguments.get("numTrials")); //from args
		String[] experiments = {"exp2_H1_ANP_N3"}; //"exp2_H1_ANP_N2","exp2_H1_ANP_N3","exp2_H2_ANP_N1","exp2_H2_ANP_N2","exp2_H2_ANP_N3" 
		//"exp4_H2H1_ANP_N1","exp4_H2H1_ANP_N2","exp4_H2H1_ANP_N3","exp4_H1H2_ANP_N1", "exp4_H1H2_ANP_N2","exp4_H1H2_ANP_N3"

		// "IJCAI/exp4_H2H1_ANP_N3", "IJCAI/exp2_H1_ANP_N1"//"Batch_fromUpDown", "Batch_fromWaitDown",
		//"Batch_fromUpDown.json", "Test_CodedNorms.json", "Test_LooseNorms.json"Batch_fromLooseUpDown, Test_WaitDownNorms
		//"hall_1","hall_2","door","tunnels","manners","corner_1","corner_2","hall_pair","corner_pair"
		String uniqueTime =  NormLearnersExperiment.generateUniqueID();
		for(int e =0; e<experiments.length;e++){
			String toPrint = "";
			String experimentFile = "IJCAI/"+experiments[e]; //arguments.get("experiment"); //from args
			if(!experimentFile.contains(".json")){
				experimentFile+=".json";

			}

			// some other folder locations here
			String outputFolder = currDir+arguments.get("outputF"); //from args
			String paramFilesFolder = currDir+arguments.get("paramF"); //from args
			String experimentFolder = currDir+arguments.get("experimentF"); //from args
			String gamesFolder = currDir+arguments.get("gamesF");

			experimentFile = experimentFolder + experimentFile;
			String uniqueId = uniqueTime+"/"+experiments[e].split("\\.")[0];//edited here too
			int numSamples = minNumSamples;
			if(maxNumSamples<0){
				numSamples = maxNumSamples;
			}
			for (; numSamples<=maxNumSamples; numSamples++){

				System.out.println("NUM SAMP: "+numSamples);
				for (int trial = 0; trial < numTrials; trial++) {
					Experiment experiment = new Experiment(experimentFile,
							paramFilesFolder, gamesFolder, outputFolder+uniqueId+"_"+numSamples, numSamples, Integer.toString(trial));
					NormLearnersExperiment ex = new NormLearnersExperiment(experiment);

					toPrint+=ex.runExperiment(trial, numSamples,outputFolder+uniqueId+"_"+numSamples);
					ex.visualizeResults(visualize);

				}
			}

			try
			{
				File outFile = new File (outputFolder+uniqueId);
				outFile.mkdirs();
				FileWriter writer = new FileWriter(outFile.getAbsolutePath()+"/metrics.csv");

				writer.append(toPrint);
				writer.flush();
				writer.close();
			}
			catch(IOException e1)
			{
				e1.printStackTrace();
			} 

		}
	}
}
