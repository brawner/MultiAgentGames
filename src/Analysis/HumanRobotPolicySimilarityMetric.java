//package Analysis;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.FilenameFilter;
//import java.io.IOException;
//import java.io.OutputStreamWriter;
//import java.io.Writer;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import burlap.behavior.stochasticgames.GameEpisode;
//import burlap.domain.stochasticgames.gridgame.GridGame;
//import burlap.mdp.core.state.State;
//import burlap.mdp.stochasticgames.JointAction;
//import burlap.mdp.stochasticgames.SGDomain;
//import burlap.statehashing.HashableState;
//import burlap.statehashing.simple.SimpleHashableStateFactory;
//
///**
// * Setup assumed here is that we have a folder with single experiments. With different trials in each experiment folder.
// * Each match within the trial folder. And each match has a human-human game and an agent agent game folder.
// * With .game file in each folder.
// * @author ngopalan
// *
// */
//
//public class HumanRobotPolicySimilarityMetric {
//
//	public double[] calculateMetric(String inputFolder, int exampleMatch, int learnedMatch, boolean trialSpecific){
//		GridGame gg = new GridGame();
//		SGDomain domain = (SGDomain)gg.generateDomain();
//		final int exMatch = exampleMatch;
//		final int learnMatch = learnedMatch;
//
//		boolean debugPrint = false;
//		// experiment -> trial -> match -> round ->
//		// assume path with many games each
//		//String pathToExperiment = "/home/ng/workspace/Experiment1/";
//		String outputFileName = "gameResults10.csv";
//
//
//
//		File experimentLocation = new File(inputFolder);
//		//		File agentExperimentLocation = new File(pathToExperimentAgentTrials);
//
//		System.out.println("Experiment folder: " + inputFolder);
//
//		String[] humanGameFiles = experimentLocation.list(new FilenameFilter() {
//			@Override
//			public boolean accept(File current, String name) {
//				return name.toLowerCase().contains(".game") && name.toLowerCase().contains("match_"+exMatch);
//			}
//		});
//		
//
//		String[] agentGameFiles = experimentLocation.list(new FilenameFilter() {
//			@Override
//			public boolean accept(File current, String name) {
//				return name.toLowerCase().contains(".game") && name.toLowerCase().contains("match_"+learnMatch);
//			}
//		});
//		System.out.println("Num human: "+humanGameFiles.length+" Agent files: "+agentGameFiles.length);
//
//		// create a hashmap of trials
//		Map<String, ArrayList<String>> humanFileMap = getDataStructure(humanGameFiles, trialSpecific);
//		Map<String, ArrayList<String>> agentFileMap = getDataStructure(agentGameFiles, trialSpecific);
//
//		String outputString = "";
//		outputString = outputString + "Game Number"+  ","  + "similar actions from similar states"+ "," + "Total similar states" + "," + "Ratio of similar actions" +"," + "Ratio of similar states"+ "\n";
//
//		// now iterate over trials and matches find the same keys in both sets and run analysis
//		// assume we have two directories over here humans and agents over here
//
//		double[] results = new double[4];
//		for(String trialStr: humanFileMap.keySet()){
//
//			if(!agentFileMap.containsKey(trialStr)){
//				System.err.println("Trial " + trialStr + " does not exist in the agent files.");
//				System.exit(-100);
//			}
//
//			List<File> humanListOfFiles = new ArrayList<File>();//humanFolder.listFiles();
//			List<File> agentListOfFiles = new ArrayList<File>();//agentFolder.listFiles();
//
//			for(String humanFile :humanFileMap.get(trialStr)){
//				humanListOfFiles.add(new File(inputFolder + humanFile));
//			}
//
//			for(String agentFile :agentFileMap.get(trialStr)){
//				agentListOfFiles.add(new File(inputFolder + agentFile));
//			}
//
//			SimpleHashableStateFactory shf = new SimpleHashableStateFactory();
//
//			Map<HashableState,HashMap<JointAction,Integer>> humanStateActionCount = new HashMap<HashableState,HashMap<JointAction,Integer>>();
//			Map<HashableState,HashMap<JointAction,Integer>> robotStateActionCount = new HashMap<HashableState,HashMap<JointAction,Integer>>();
//
//			int actionSimilarity = 0;
//			int stateSimilarity = 0;
//
//			for(int i=0;i<humanListOfFiles.size();i++){
//				//			System.out.println(listOfFiles[i]);
//				// read all files and store states in a hash map -> hash map of actions -> counts of actions
//				//					if (humanListOfFiles[i].getAbsolutePath().indexOf("learned")== -1) {
//				// it is a human game
//				GameEpisode ga = null;
//				//					System.out.println("human path as seen right now " + humanListOfFiles.get(i).getAbsolutePath() );
////				ga = GameEpisode.parseFileIntoGA(humanListOfFiles.get(i).getAbsolutePath() , domain);
//				List<State> sl = ga.states;
//				List<JointAction> jal = ga.jointActions;
//				//				System.out.println(jal.size() + " " + sl.size());
//				for(int stateCount=0;stateCount<sl.size()-1;stateCount++){
//					HashableState sTemp = shf.hashState(sl.get(stateCount));
//
//					JointAction jaTemp = jal.get(stateCount);
//					if(humanStateActionCount.containsKey(sTemp)){
//						if(humanStateActionCount.get(sTemp).containsKey(jaTemp)){
//							int value = humanStateActionCount.get(sTemp).get(jaTemp);
//							humanStateActionCount.get(sTemp).put(jaTemp, value+1);
//						}
//						else{
//							//this is if the action is not present
//							humanStateActionCount.get(sTemp).put(jaTemp, 1);
//						}
//						//						stateActionCount.put(sTemp, value)
//					}
//					else{
//						//this if the state is not present
//						humanStateActionCount.put(sTemp, new HashMap<JointAction,Integer>());
//						humanStateActionCount.get(sTemp).put(jaTemp, 1);
//					}
//				}
//			}
//			for(int i=0;i<agentListOfFiles.size();i++){{
//				// it is a robot game
//				GameEpisode ga = null;
//				ga = GameEpisode.parseFileIntoGA(agentListOfFiles.get(i).getAbsolutePath(), domain);
//				List<State> sl = ga.states;
//				List<JointAction> jal = ga.jointActions;
//				for(int stateCount=0;stateCount<sl.size()-1;stateCount++){
//					HashableState sTemp = shf.hashState(sl.get(stateCount));
//					JointAction jaTemp = jal.get(stateCount);
//					if(robotStateActionCount.containsKey(sTemp)){
//						if(robotStateActionCount.get(sTemp).containsKey(jaTemp)){
//							int value = robotStateActionCount.get(sTemp).get(jaTemp);
//							robotStateActionCount.get(sTemp).put(jaTemp, value+1);
//						}
//						else{
//							//this is if the action is not present
//							robotStateActionCount.get(sTemp).put(jaTemp, 1);
//						}
//						//						stateActionCount.put(sTemp, value)
//					}
//					else{
//						//this if the state is not present
//						robotStateActionCount.put(sTemp, new HashMap<JointAction,Integer>());
//						robotStateActionCount.get(sTemp).put(jaTemp, 1);
//					}
//				}
//			}
//
//			}
//
//
//			// out of the files now need to count mode of actions and see 
//			//if they are the same for all the actions
//
//			// if states present only then check for actions
//			// check for the frequency of actions as well and then display 
//			// with a frequency the mode of picking the modal joint action
//			int tempCount =0;
//			for(HashableState s:humanStateActionCount.keySet()){
//
//				for(HashableState s1:robotStateActionCount.keySet()){
//					if(s.equals(s1)){
//						tempCount+=1;
//						break;
//					}
//					//				break;
//				}
//				//			break;
//			}
//
//
//			//		System.out.println("robot state");
//
//			for(HashableState s:humanStateActionCount.keySet()){
//				Map<JointAction,Integer> humanJaM = humanStateActionCount.get(s);
//				if(!robotStateActionCount.containsKey(s)){
//					//				System.out.println("was here");
//					//				System.out.println(robotStateActionCount.keySet().size());
//					//				System.out.println(humanStateActionCount.keySet().size());
//					continue;
//				}
//
//				stateSimilarity = stateSimilarity + 1;
//
//				Map<JointAction,Integer> robotJaM = robotStateActionCount.get(s);
//				double humanTotalCount =0;
//				double humanMaxJaCount = Double.MIN_VALUE;
//				List<JointAction> humanMaxJa = new ArrayList<JointAction>();
//				for(JointAction ja : humanJaM.keySet()){
//					int tempValue = humanJaM.get(ja);
//					humanTotalCount += tempValue;
//
//					if(tempValue>humanMaxJaCount){
//						humanMaxJa.clear();
//						humanMaxJa.add(ja);
//						humanMaxJaCount = tempValue;
//
//					}
//					else if(tempValue==humanMaxJaCount){
//						humanMaxJa.add(ja);
//					}
//				}
//				double freqHuman = humanMaxJaCount/humanTotalCount;
//
//				double robotTotalCount =0;
//				double robotMaxJaCount = Double.MIN_VALUE;
//				List<JointAction> robotMaxJa = new ArrayList<JointAction>();
//				for(JointAction ja : robotJaM.keySet()){
//					int tempValue = robotJaM.get(ja);
//					robotTotalCount += tempValue;
//
//					if(tempValue>robotMaxJaCount){
//						robotMaxJa.clear();
//						robotMaxJa.add(ja);
//						robotMaxJaCount = tempValue;
//
//					}
//					else if(tempValue==robotMaxJaCount){
//						humanMaxJa.add(ja);
//					}
//				}
//				double freqRobot = robotMaxJaCount/robotTotalCount;
//				if(freqRobot==Double.POSITIVE_INFINITY){
//					System.out.println("list size: " +robotJaM.size());
//				}
//
//				boolean equalityFlag = false;
//
//				for(JointAction jaH : humanMaxJa){
//					for(JointAction jaR: robotMaxJa){
//						if(jaH.equals(jaR)){
//							equalityFlag = true;
//							break;
//						}
//					}
//					if(equalityFlag){
//						actionSimilarity +=1;
//						break;
//					}
//				}
//
//				//					System.out.println(equalityFlag + " frequencyR "+ freqRobot +" frequencyH "+ freqHuman);
//				//					System.out.println(" Robot num "+ robotMaxJaCount +" robot denom "+ robotTotalCount);
//
//				if(debugPrint){
//					for(JointAction jaH : humanMaxJa){
//						System.out.println("action human "+ jaH.actionName());
//					}
//
//					for(JointAction jaR : robotMaxJa){
//						System.out.println("action robot "+ jaR.actionName());
//					}
//				}
//			}
//			//				System.out.println("total equal states: " + tempCount);
//
//			//				System.out.println(humanStateActionCount.keySet().size());
//			//				System.out.println(robotStateActionCount.keySet().size());
//			//
//			//		GameEpisode ga = null;
//			//		ga = GameEpisode.parseFileIntoGA(fileName, domain);
//			//		int sizeJointAction = ga.jointActions.size();
//
//			//		System.out.println(sizeJointAction);
//			//		for(int i =0;i<sizeJointAction;i++){
//			//			System.out.println(ga.jointActions.get(i).actionName());
//			//		}
//			//		
//			//		for(int i=0;i<ga.states.size();i++){
//			//			System.out.println(ga.states.get(i).getCompleteStateDescription()+ "\n"+i);
//			//	
//			//		}
//
//			//		GameSequenceVisualizer gv = new GameSequenceVisualizer(v, domain, fileName);
//
//			outputString = outputString + trialStr +  ","  + actionSimilarity + "," + stateSimilarity +"," +((double)actionSimilarity)/stateSimilarity +"," +((double)stateSimilarity)/humanStateActionCount.size() +"\n";
//			results[0] = actionSimilarity;
//			results[1] = stateSimilarity;
//			results[2] = ((double)actionSimilarity)/stateSimilarity;
//			results[3] = ((double)stateSimilarity)/humanStateActionCount.size();
//		}
//		if(debugPrint)		System.out.println(outputString);
//
//		Writer writer = null;
//
//		try {
//			writer = new BufferedWriter(new OutputStreamWriter(
//					new FileOutputStream(outputFileName), "utf-8"));
//			writer.write(outputString);
//		} catch (IOException ex) {
//			// report
//		} finally {
//			try {writer.close();} catch (Exception ex) {/*ignore*/}
//		}
//		return results;
//	}
//
//
//	public static Map<String, ArrayList<String>> getDataStructure(String[] gameFiles, boolean trialSpecific){
//		Map<String,  ArrayList<String>> fileMap = new HashMap<String, ArrayList<String>>();
//
//		for(String fileName : gameFiles){
//			String[] parts = fileName.split("_");
//			// check if humanFileMap has a trial
//			String trialStr ="trial_x";
//			if(trialSpecific){
//				trialStr =parts[0] + "_"+ parts[1];
//			}
//			if(fileMap.containsKey(trialStr)){
//				fileMap.get(trialStr).add(fileName);
//			}
//			else{
//				fileMap.put(trialStr,new ArrayList<String>());
//				fileMap.put(trialStr, new ArrayList<String>());
//			}
//			// check if that trial's map has the match
//		}
//		return fileMap;
//	}
//}
