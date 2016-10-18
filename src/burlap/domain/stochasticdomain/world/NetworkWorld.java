package burlap.domain.stochasticdomain.world;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import networking.common.WorldLoadingStateGenerator;
import burlap.behavior.stochasticgames.GameEpisode;
import burlap.debugtools.DPrint;
import burlap.mdp.auxiliary.StateMapping;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.oo.state.OOState;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.SGDomain;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import burlap.mdp.stochasticgames.world.World;
import burlap.mdp.stochasticgames.world.WorldObserver;
import burlap.oomdp.stochasticgames.Callables.GameStartingCallable;
import burlap.oomdp.stochasticgames.Callables.GetActionCallable;
import burlap.oomdp.stochasticgames.Callables.ObserveOutcomeCallable;
import burlap.parallel.Parallel;
import burlap.parallel.Parallel.ForCallable;
import burlap.parallel.Parallel.ForEachCallable;


/**
 * This class provides a means to have agents play against each other and synchronize all of their actions and observations.
 * Any number of agents can join a World instance and they will be told when a game is starting, when a game ends, when
 * they need to provide an action, and what happened to all agents after every agent made their action selection. The world
 * may also make use of an optional {@link burlap.oomdp.auxiliary.StateAbstraction} object so that agents are provided an 
 * abstract and simpler representation of the world. A game can be run until a terminal state is hit, or for a specific
 * number of stages, the latter of which is useful for repeated games.
 * @author James MacGlashan
 *
 */
public class NetworkWorld extends World {

	protected final int								maxAgentsCanJoin;
	protected final String							worldDescription;
	protected double 							threadTimeout;
	private final AtomicBoolean							isOk;
	private final WorldLoadingStateGenerator				wlStateGenerator;
	/**
	 * Initializes the world.
	 * @param domain the SGDomain the world will use
	 * @param jr the joint reward function
	 * @param tf the terminal function
	 * @param sg a state generator for generating initial states of a game
	 */
	public NetworkWorld(SGDomain domain, JointRewardFunction jr, TerminalFunction tf, WorldLoadingStateGenerator sg, StateMapping stateAbstraction, int numAgents, String description){
		super(domain, jr, tf, sg, stateAbstraction);
		this.maxAgentsCanJoin = numAgents;
		this.worldDescription = description;
		if (sg == null) {
			throw new RuntimeException("This can't be null");
		}
		
		this.wlStateGenerator = sg;
		this.isOk = new AtomicBoolean(false);
		this.threadTimeout = Parallel.NO_TIME_LIMIT;
	}
	
	/**
	 * Generates an example starting state;
	 * @return
	 */
	public State startingStateWithoutNames() {
		WorldLoadingStateGenerator wlsg = (WorldLoadingStateGenerator) this.initialStateGenerator;
		return (OOState) wlsg.generateState(this.agents);		
	}
	
	public State startingStateWithNames() {
//		WorldLoadingStateGenerator wlsg = (WorldLoadingStateGenerator) this.initialStateGenerator;
//		OOState startingState = (OOState) wlsg.generateState(this.agents);
//		return this.abstractionForAgents.mapState(startingState);
		return this.startingStateWithoutNames();
	}
	
	
	@Override
	public void generateNewCurrentState(){
		if(!this.gameIsRunning()) {
			this.currentState = this.startingStateWithNames();
		}
	}
	
	private void setOk(boolean value) {
		this.isOk.set(value);
	}
	
	public void stopGame() {
		this.setOk(false);
	}
	
	public boolean isOk() {
		return this.isOk.get();
	}
	
	public int getMaximumAgentsCanJoin() {
		return this.maxAgentsCanJoin;
	}
	
	public NetworkWorld copy() {
		return new NetworkWorld(this.domain, this.jointRewardFunction, this.tf, this.wlStateGenerator, this.abstractionForAgents, this.maxAgentsCanJoin, this.worldDescription);
	}
	
	
	/**
	 * Sets the timeout for threads that call an agent's getAction methods. -1.0 is currently no time limit
	 * @param timeout
	 */
	public void setThreadTimeout(double timeout) {
		if (timeout < 0) {
			this.threadTimeout = Parallel.NO_TIME_LIMIT;
		}
		this.threadTimeout = timeout;
	}
	
	
	/**
	 * Returns the cumulative reward that the agent with name aname has received across all interactions in this world.
	 * @param aname the name of the agent
	 * @return the cumulative reward the agent has received in this world.
	 */
	public double getCumulativeRewardForAgent(String aname){
		return agentCumulativeReward.v(aname);
	}
	
	@Override
	public GameEpisode runGame(int maxStages){	
		this.currentState = this.startingStateWithNames();
		return this.runGame(maxStages, this.currentState);
		
	}
	
	@Override
	public GameEpisode runGame(int maxStages, State startState){
		this.setOk(true);
		this.currentState = startState;
		
		ForCallable<Boolean> gameStarting = new GameStartingCallable(this, this.agents);
		Parallel.For(0, this.agents.size(), 1, gameStarting);
		
		for(WorldObserver wob : this.worldObservers){
			wob.gameStarting(this.currentState);
		}
		/*
		for(Agent a : agents){
			a.gameStarting();
		}*/
		
		this.currentGameEpisodeRecord = new GameEpisode(currentState);
		this.isRecordingGame = true;
		int t = 0;
		
		while(!tf.isTerminal(currentState) && (maxStages < 0 || t < maxStages) && this.isOk.get()){
			this.runStage();
			t++;
		}
		
		// call to agents needs to be threaded, and timed out
		for(SGAgent a : agents){
			a.gameTerminated();
		}
		
		for(WorldObserver wob : this.worldObservers){
			wob.gameEnding(this.currentState);
		}

		
		// clean up threading
		
		DPrint.cl(debugId, currentState.toString());
		
		this.isRecordingGame = false;
		
		return this.currentGameEpisodeRecord;
	}
	
	
	
	
	
	/**
	 * Runs a single stage of this game.
	 */
	@Override
	public void runStage(){
		if (this.currentState == null) {
			throw new RuntimeException("The current state has not been set.");
		}
		if(tf.isTerminal(currentState)){
			return ; //cannot continue this game
		}
		
		JointAction ja = new JointAction();
		State abstractedCurrent = abstractionForAgents.mapState(currentState);
		
		// Call to agents needs to be threaded, and timed out
		ForEachCallable<List<SGAgent>, List<Action>> getActionCallable = new GetActionCallable(abstractedCurrent);
		
		// Agents that can threaded independently of any others should have their own list
		List<List<SGAgent>> agentsByThread = new ArrayList<List<SGAgent>>();
		
		// All agents that share a common library, or for some reason can't be run on a different thread as another must go in this list
		List<SGAgent> singleThreadedAgents = new ArrayList<SGAgent>();
		
		// Check all agents for their parallizability
		for (SGAgent agent : agents) {
			agentsByThread.add(Arrays.asList(agent));
		}
		// Randomize order so if not all agents can run in the alloted time, it's at least different each run
		Collections.shuffle(singleThreadedAgents);
		agentsByThread.add(singleThreadedAgents);
		
		// Run agents on separate threads if possible
		List<List<Action>> allActions = Parallel.ForEach(agentsByThread, getActionCallable, this.threadTimeout);
		
		
		for (List<Action> actions : allActions) {
			if (actions == null) {
				continue;
			}
			for (Action action : actions) {
				if (action != null) {
					ja.addAction(action);
				}
			}
		}
			
		/*for(Agent a : agents){
			ja.addAction(a.getAction(abstractedCurrent));
		}*/
		this.lastJointAction = ja;
		
		
		DPrint.cl(debugId, ja.toString());
		
		
		//now that we have the joint action, perform it
		State sp = worldModel.sample(currentState, ja);
		//State abstractedPrime = this.abstractionForAgents.abstraction(sp);
		double[] jointReward = jointRewardFunction.reward(currentState, ja, sp);
		
		DPrint.cl(debugId, jointReward.toString());
		
		//index reward
		for(int i = 0; i < jointReward.length; i++){
			String agentName = this.agents.get(i).agentName();
			agentCumulativeReward.add(agentName, jointReward[i]);
		}
		
		// This needs to be threaded, and timed out
		//tell all the agents about it
		ForEachCallable<SGAgent, Boolean> callable = new ObserveOutcomeCallable(currentState, ja, jointReward, sp, this.abstractionForAgents, tf.isTerminal(sp));
		Parallel.ForEach(agents, callable);
		/*for(Agent a : agents){
			a.observeOutcome(abstractedCurrent, ja, jointReward, abstractedPrime, tf.isTerminal(sp));
		}*/
		
		// Maybe need to be threaded, and timed out.
		//tell observers
		for(WorldObserver o : this.worldObservers){
			o.observe(currentState, ja, jointReward, sp);
		}
		
		//update the state
		currentState = sp;
		
		//record events
		if(this.isRecordingGame){
			this.currentGameEpisodeRecord.transition(this.lastJointAction, this.currentState, jointReward);
		}
		
	}
	
	@Override
	public String toString() {
		return (this.worldDescription == null) ? "" : this.worldDescription;
	}
	
	public String getDescription() {
		return this.worldDescription;
	}
}
