package networking.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import burlap.behavior.stochasticgame.GameAnalysis;

public class GameMonitor implements Callable<Boolean>{

	private final List<Future<GameAnalysis>> futures;
	private final GridGameServer server;
	public GameMonitor(GridGameServer server) {
		this.futures = Collections.synchronizedList(new ArrayList<Future<GameAnalysis>>());
		this.server = server;
	}

	public void addFuture(Future<GameAnalysis> future) {
		synchronized(this.futures) {
			this.futures.add(future);
		}
	}
	@Override
	public Boolean call() throws Exception {
		while(true) {
			List<Future<GameAnalysis>> toRemove = new ArrayList<Future<GameAnalysis>>();
			synchronized(this.futures){ 
				for (Future<GameAnalysis> future : this.futures) {
					try {
						GameAnalysis analysis = future.get(0, TimeUnit.SECONDS);
						this.server.closeGame(future, analysis);
						toRemove.add(future);
					} catch (TimeoutException e) {
						
					}
					
				}
				this.futures.removeAll(toRemove);
			}
			Thread.sleep(10);
		}
	}

}
