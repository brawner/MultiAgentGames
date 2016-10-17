//package experiments;
//
//import java.util.List;
//
//public class BatchEpisodeAnalysisGenerator {
//
//
//	String gameFile;
//	public BatchEpisodeAnalysisGenerator(String gameFile) {
//
//		this.gameFile = gameFile;
//	}
//
//	private List<EpisodeAnalysis> generateBatch(boolean humanGenerated) {
//		
//		
//		if(humanGenerated){
//			//
//		}else {
//			
//			
//			
//		}
//		return null;
//	}
//	
//	public static void main(String[] args) {
//
//		String gameFile = "";
//		boolean humanGenerated = false;
//
//		// read in game type
//		BatchEpisodeAnalysisGenerator batchGenerator = new BatchEpisodeAnalysisGenerator(gameFile);
//
//		//either play game or sample from Policy
//		List<EpisodeAnalysis> episodes = batchGenerator.generateBatch(humanGenerated);
//
//		//output EpisodeAnalysies
//		EpisodeAnalysis.writeEpisodesToDisk(episodes, "DIRPATH", "BASEFILE");
//
//
//	}
//
//
//
//}
