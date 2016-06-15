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
 * 
 * This version switches to the model where 
 */
public class RewardFnNormLearnersExperiment extends NormLearnersExperiment{

	public RewardFnNormLearnersExperiment(Experiment experiment) {
		super(experiment);
	
	}

	@Override
	public String runExperiment(int trial,int numSamples, String outputFolder, boolean calcMetric) {

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

			//			String comparisonVal = experiment.comparePolicies(outputFolder+"/trial_"+trial+"_", match, match-1, true);
			//			if(comparisonVal.compareTo("-1")!=0){
			//				toPrint+=trial+","+numSamples+","+comparisonVal+"\n";
			//			}
			results.add(matchList);

		}
		//TODO: switch when done
		//TODO: change compare policies to run this
		calcMetric = true;
		if(calcMetric){
			String comparisonVal = experiment.comparePolicies(outputFolder+"/", experiment.numMatches-1, experiment.numMatches-2,
					false,false, true,experiment.tf);
			if(comparisonVal.compareTo("-1")!=0){
				toPrint+=trial+","+numSamples+","+comparisonVal+"\n";
			}

		}
		System.out.println("Outputting results");
		return toPrint;

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

	
	public static void main(String[] args) {
		boolean visualize = true;
		int increment = 1;
		int minNumSamples = 16;
		int maxNumSamples = 16; //12
		String currDir = System.getProperty("user.dir"); 
//				+ "/MultiAgentGames" ;
		Map<String, String> arguments = parseArguments(args);
		
		System.out.println(currDir);

		int numTrials = Integer.parseInt(arguments.get("numTrials")); //from args
		String[] experiments = {"exp2_H1_ANP_N1"}; //"exp2_H1_ANP_N1","exp2_H1_AT_N1","exp2_H1_ANS_N0","exp2_H1_ANP_N0","exp2_H1_AT_N0","exp2_H1_ANS_N2","exp2_H1_ANP_N2","exp2_H1_AT_N2"
		//"exp2_H2_ANP_N1","exp2_H2_ANP_N2","exp2_H2_ANP_N3"

		// "exp2_H2_ANP_N2","exp2_H2_ANP_N3" //"exp2_H1_ANP_N2","exp2_H1_ANP_N3","exp2_H2_ANP_N1","exp2_H2_ANP_N2","exp2_H2_ANP_N3" 
		//"exp4_H2H1_ANP_N1","exp4_H2H1_ANP_N2","exp4_H2H1_ANP_N3","exp4_H1H2_ANP_N1", "exp4_H1H2_ANP_N2","exp4_H1H2_ANP_N3"

		// "IJCAI/exp4_H2H1_ANP_N3", "IJCAI/exp2_H1_ANP_N1"//"Batch_fromUpDown", "Batch_fromWaitDown",
		//"Batch_fromUpDown.json", "Test_CodedNorms.json", "Test_LooseNorms.json"Batch_fromLooseUpDown, Test_WaitDownNorms
		//"hall_1","hall_2","door","tunnels","manners","corner_1","corner_2","hall_pair","corner_pair"
		String uniqueTime =  NormLearnersExperiment.generateUniqueID();
		for(int e =0; e<experiments.length;e++){
			String toPrint = "";
			String experimentFile = "IJCAI/"+experiments[e];
			//String experimentFile = experiments[e];//arguments.get("experiment"); //from args
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
			
			try {
				File outFile = new File (outputFolder+uniqueId);
				outFile.mkdirs();
				FileWriter writer = new FileWriter(outFile.getAbsolutePath()+"/metrics.csv");

				for (; numSamples<=maxNumSamples; numSamples+=increment){

					for (int trial = 0; trial < numTrials; trial++) {
						Experiment experiment = new Experiment(experimentFile,
								paramFilesFolder, gamesFolder, outputFolder+uniqueId+"_"+numSamples, numSamples, Integer.toString(trial));
						RewardFnNormLearnersExperiment ex = new RewardFnNormLearnersExperiment(experiment);
						boolean calcMetric = true;

						if(trial==numTrials-1){
							calcMetric = true;
						}
						toPrint+=ex.runExperiment(trial, numSamples,outputFolder+uniqueId+"_"+numSamples, calcMetric);
						ex.visualizeResults(visualize);
					}
					System.out.println("NUM SAMP Finished: "+numSamples);
					writer.append(toPrint);
					toPrint="";
				}

				writer.flush();
				writer.close();
			}
			catch(IOException e1)
			{
				e1.printStackTrace();
			} 
			System.out.println("Done outputing results");
		}
	}
}
