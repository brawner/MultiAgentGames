package experiments;

import java.util.List;

import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.stochasticgames.auxiliary.jointmdp.CentralizedDomainGenerator;
import burlap.domain.stochasticgames.gridgame.GGAltVis;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.states.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.environment.Environment;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

public class BatchEpisodeAnalysisGenerator {


	String gameFile;
	public BatchEpisodeAnalysisGenerator(String gameFile) {

		this.gameFile = gameFile;
	}

	private List<EpisodeAnalysis> generateBatch(boolean humanGenerated) {
		
		
		if(humanGenerated){
			//
		}else {
			
			
			
		}
		return null;
	}
	
	public static void main(String[] args) {

		String gameFile = "";
		boolean humanGenerated = false;

		// read in game type
		BatchEpisodeAnalysisGenerator batchGenerator = new BatchEpisodeAnalysisGenerator(gameFile);

		//either play game or sample from Policy
		List<EpisodeAnalysis> episodes = batchGenerator.generateBatch(humanGenerated);

		//output EpisodeAnalysies
		EpisodeAnalysis.writeEpisodesToDisk(episodes, "DIRPATH", "BASEFILE");


	}



}
