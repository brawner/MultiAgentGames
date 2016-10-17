//package experiments;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import burlap.behavior.stochasticgames.GameEpisode;
//import burlap.domain.stochasticgames.gridgame.GridGame;
//import burlap.mdp.stochasticgames.SGDomain;
//
//public class CalculateMetrics {
//
//	// step, round, match, trial
//
//	public CalculateMetrics(String inputFolder){
//
//		File folder = new File(inputFolder);
//		File[] listOfFiles = folder.listFiles();
//		try {
//			FileWriter writer = new FileWriter(inputFolder+"/stats.csv");
//
//
//			for (int i = 0; i < listOfFiles.length; i++) {
//				if(!listOfFiles[i].getAbsolutePath().contains(".csv")){
//					if (listOfFiles[i].isFile()) {
//						String res = calculateMetricsForGame(listOfFiles[i].getAbsolutePath());
//
//						writer.append(res);
//
//						writer.append('\n');
//						System.out.println(res);
//					} else if (listOfFiles[i].isDirectory()) {
//						System.out.println("Directory " + listOfFiles[i].getName());
//					}
//
//
//				}
//
//			}
//			writer.flush();
//			writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	public String calculateMetricsForGame(String file){
//
//		// do I have to do this??
//		SGDomain sgDomain = (SGDomain) new GridGame().generateDomain();
//		sgDomain.setJointActionModel(new GridGameStandardMechanicsWithoutTieBreaking(
//				sgDomain, .5));
//		new GridGame.GGTerminalFunction(sgDomain);
//		new GridGame.GGJointRewardFunction(sgDomain, 0, 10, true);
//
//		String outString = parseFilename(file);
//		GameEpisode ga = makeGA(file, sgDomain);
//		int numSteps = ga.states.size()-1;
//		List<Map<String,Double>> rewards = ga.jointRewards;
//		Map<String,Double> agentRewards = calcAgentRewards(rewards);
//		double totalReward = calcTotalRewards(agentRewards);
//
//		outString+=Integer.toString(numSteps)+","+agentRewards.get("agent0")
//				+","+agentRewards.get("agent1")+","+totalReward;
//
//		return outString;
//
//	}
//
//	private String parseFilename(String file) {
//
//		//System.out.println(file);
//		String[] fullFileName = file.split("\\.");
//		String[] fileName = fullFileName[0].split("/");
//		//System.out.println(fileName[fileName.length-1]);
//		String[] fileParts = fileName[fileName.length-1].split("_");
//
//		String trial = "";
//		String match = "";
//		String round = "";
//
//		for(int i = 0; i<fileParts.length-1;i++){
//			if(fileParts[i].compareToIgnoreCase("trial")==0){
//				trial = fileParts[i+1];
//			}
//			if(fileParts[i].compareToIgnoreCase("match")==0){
//				match = fileParts[i+1];
//			}
//			if(fileParts[i].compareToIgnoreCase("round")==0){
//				round = fileParts[i+1];
//			}
//		}
//		return trial+","+match+","+round+",";
//	}
//
//	private double calcTotalRewards(Map<String, Double> agentRewards) {
//		double reward = 0.0;
//		for(String agent : agentRewards.keySet()){
//			reward+= agentRewards.get(agent);
//		}
//		return reward;
//	}
//
//	private Map<String, Double> calcAgentRewards(
//			List<Map<String, Double>> rewards) {
//		Map<String, Double> agentRewards = new HashMap<String, Double>();
//		for(Map<String,Double> r : rewards){
//			for(String agent : r.keySet()){
//				if(agentRewards.containsKey(agent)){
//					agentRewards.put(agent,agentRewards.get(agent)+r.get(agent));
//				}else{
//					agentRewards.put(agent,r.get(agent));
//				}
//			}
//		}
//		return agentRewards;
//	}
//
//	private GameEpisode makeGA(String file,SGDomain domain) {
//		GameEpisode ga = GameEpisode.parseFileIntoGA(file, domain);
//		return ga;
//	}
//
//	public static void main(String[] args){
//
//		File folder = new File("/home/betsy/workspace/MultiAgentGames/grid_games/results");
//		File[] listOfFiles = folder.listFiles();
//
//		for (int i = 0; i < listOfFiles.length; i++) {
//
//			if (listOfFiles[i].isDirectory()){
//				String inputFolder = listOfFiles[i].getAbsolutePath();
//				CalculateMetrics metricsCalc = new CalculateMetrics(inputFolder);
//
//			}
//
//		}
//	}
//
//}
