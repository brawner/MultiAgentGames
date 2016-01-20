package networking.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import burlap.behavior.stochasticgames.GameAnalysis;

/**
 * Constantly monitors the currently running games to check if they are done and shut them down if they are.
 * @author brawner
 *
 */
public class GameMonitor implements Callable<Boolean>{

	private final List<Future<GameAnalysis>> futures;
	private final GridGameManager server;
	public GameMonitor(GridGameManager server) {
		this.futures = Collections.synchronizedList(new ArrayList<Future<GameAnalysis>>());
		this.server = server;
	}

	public void addFuture(Future<GameAnalysis> future) {
		synchronized(this.futures) {
			this.futures.add(future);
		}
	}
	
	public void removeFuture(Future<GameAnalysis> future) {
		synchronized(this.futures) {
			this.futures.remove(future);
		}
	}
	@Override
	public Boolean call() throws Exception {
		try {
			long count = 0;
			int previousNumber = 0;
		while(true) {
			List<Future<GameAnalysis>> toRemove = new ArrayList<Future<GameAnalysis>>();
			List<GameAnalysis> analyses = new ArrayList<GameAnalysis>();
			synchronized(this.futures){ 
				count++;
				if (this.futures.size() != previousNumber) {
					System.out.println("Number of active games currently running " + this.futures.size());
					previousNumber = this.futures.size();
				}

				for (Future<GameAnalysis> future : this.futures) {
					try {
						GameAnalysis analysis = future.get(0, TimeUnit.SECONDS);
						analyses.add(analysis);
						toRemove.add(future);
					} catch (TimeoutException e) {
						
					}
					
				}
				this.futures.removeAll(toRemove);
				
				for (int i = 0; i < toRemove.size(); i++) {
					Future<GameAnalysis> future = toRemove.get(i);
					GameAnalysis analysis = analyses.get(i);
					this.server.processGameCompletion(future, analysis);
				}
				
			}
			Thread.sleep(10);
		}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	} 

}
