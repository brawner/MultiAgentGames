package networking.common;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import burlap.behavior.stochasticgames.auxiliary.GameSequenceVisualizer;
import burlap.domain.stochasticgames.gridgame.GGVisualizer;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.legacy.StateJSONParser;
import burlap.oomdp.legacy.StateParser;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.visualizer.Visualizer;
import burlap.parallel.Parallel;
import burlap.parallel.Parallel.ForEachCallable;

public class GameAnalysisFileReader {

	public static class VisualizerCallable extends ForEachCallable<File, Boolean> {
		private final SGDomain domain;
		private final StateParser parser;
		private final File directory;
		
		public VisualizerCallable(SGDomain domain, StateParser parser, File directory) {
			this.domain = domain;
			this.parser = parser;
			this.directory = directory;
		}
		@Override
		public Boolean perform(File current) {
			if (!current.isDirectory()) {
				return false;
			}
			Visualizer visualizer = GGVisualizer.getVisualizer(5, 6);
			
			GameSequenceVisualizer sequenceVisualizer = new GameSequenceVisualizer(visualizer, domain, current.getAbsolutePath());
			sequenceVisualizer.initGUI();
			return true;
		}

		@Override
		public ForEachCallable<File, Boolean> copy() {
			return new VisualizerCallable(this.domain, this.parser, this.directory);
		}
		
	}
	
	public static void main(String[] args) {
		String directory = args[0];
		GridGame gridGame = new GridGame();
		SGDomain domain = GridGameExtreme.generateDomain(gridGame, false);
		StateParser parser = new StateJSONParser(domain);
		
		
		Path path = GridGameWorldLoader.expandDirectory(directory);
		File file = path.toFile();
		if (!file.exists()) {
			System.err.println(directory + " could not be found. Check the path");
		}
		List<File> directories = Arrays.asList(file.listFiles());
		VisualizerCallable callable = new VisualizerCallable(domain, parser, null);
		Parallel.ForEach(directories, callable);
		
		
		
	}
}
