import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.stochasticgame.GameAnalysis;
import burlap.behavior.stochasticgame.GameSequenceVisualizer;
import burlap.behavior.stochasticgame.PolicyFromJointPolicy;
import burlap.behavior.stochasticgame.agents.mavf.MultiAgentVFPlanningAgent;
import burlap.behavior.stochasticgame.mavaluefunction.backupOperators.CoCoQ;
import burlap.behavior.stochasticgame.mavaluefunction.policies.EGreedyMaxWellfare;
import burlap.behavior.stochasticgame.mavaluefunction.vfplanners.MAValueIteration;
import burlap.domain.stochasticgames.gridgame.GGVisualizer;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.auxiliary.common.StateYAMLParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.visualizer.Visualizer;


public class Chicken {
	private GridGame gridGame;
	public Chicken() {
		this.gridGame = new GridGame();
	}
	public Domain generateDomain() {
		Domain domain = this.gridGame.generateDomain();
		return domain;
	}
	
	public JointActionModel generateJointActionModel(Domain domain) {
		return new GridGameStandardMechanics(domain);
	}
	
	public TerminalFunction generateTerminalFunction() {
		return new TerminalFunction() {

			@Override
			public boolean isTerminal(State s) {
				List<ObjectInstance> agents = s.getObjectsOfClass(GridGame.CLASSAGENT);
				List<ObjectInstance> goals = s.getObjectsOfClass(GridGame.CLASSGOAL);
				for (int i = 0; i < agents.size(); i++) {
					ObjectInstance agent = agents.get(i);
					ObjectInstance goal = goals.get(i);
					if (agent.getDiscValForAttribute(GridGame.ATTX) != goal.getDiscValForAttribute(GridGame.ATTX) || 
							agent.getDiscValForAttribute(GridGame.ATTY) != goal.getDiscValForAttribute(GridGame.ATTY)) {
						return false;
					}
				}
				return true;
			}
			
		};
	}
	
	public JointReward generateJointReward() {
		return new JointReward() {
			
			@Override
			public Map<String, Double> reward(State s, JointAction ja, State sp) {
				Map<String, Double> reward = new HashMap<String, Double>();
				for (String agent : ja.actions.keySet()) {
					reward.put(agent, -1.0);
				}
				return reward;
			}
			
		};
	};
	
	public SGStateGenerator generateStateGenerator(final Domain domain, final int numberAgents, final int width, final int height) {
		
		return new SGStateGenerator() {
			@Override
			public State generateState(List<Agent> agents) {
				State state = GridGame.getCleanState(domain, numberAgents, numberAgents, 2, 2, width, height);
				int maxX = width - 1;
				int maxY = height - 1;
				for (int i = 0; i < numberAgents; i++) {
					int wall = i % 4;
					int wallPosition = i / 4;
					int startX = (wall < 2) ? 2 + 2*wallPosition : (wall - 2) * maxX;
					int startY = (wall < 2) ? wall * maxY : 2 + 2*wallPosition;
					int endX = maxX - startX;
					int endY = maxY - startY;
					GridGame.setAgent(state, i, startX, startY, i);
					GridGame.setGoal(state, i, endX, endY, i+1);
					
				}
				return state;
			}
			
		};
	}
	public World generateWorld(int numberAgents, int width, int height) {
		Domain domain = this.generateDomain();
		JointActionModel jointActionModel = this.generateJointActionModel(domain);
		TerminalFunction terminalFunction = this.generateTerminalFunction();
		JointReward jointReward = new GridGame.GGJointRewardFunction(domain); //this.generateJointReward();
		
		SGStateGenerator stateGenerator = this.generateStateGenerator(domain, numberAgents, width, height);
		World world = new World((SGDomain)domain, jointActionModel, jointReward, terminalFunction, stateGenerator);
		return world;
	}
	
	public void addAgents(World world, Domain domain, int numberAgents, int numSmart, int numRandom) {
		
		EGreedyMaxWellfare ja0 = new EGreedyMaxWellfare(0.0);
		EGreedyMaxWellfare ja1 = new EGreedyMaxWellfare(0.0);
		ja0.setBreakTiesRandomly(false);
		ja1.setBreakTiesRandomly(false);
		JointReward rf = world.getRewardModel();
		StateHashFactory hashingFactory = new DiscreteStateHashFactory();
		MAValueIteration vi0 = 
				new MAValueIteration((SGDomain) domain, world.getActionModel(), rf, world.getTF(), 
						0.95, hashingFactory, 0., new CoCoQ(), 0.00015, 50);
		MAValueIteration vi1 = 
				new MAValueIteration((SGDomain) domain, world.getActionModel(), rf, world.getTF(), 
						0.95, hashingFactory, 0., new CoCoQ(), 0.00015, 50);
		
		MultiAgentVFPlanningAgent a0 = new MultiAgentVFPlanningAgent((SGDomain) domain, vi0, new PolicyFromJointPolicy(ja0));
		MultiAgentVFPlanningAgent a1 = new MultiAgentVFPlanningAgent((SGDomain) domain, vi1, new PolicyFromJointPolicy(ja1));
		AgentType agentType0 = new AgentType(GridGame.CLASSAGENT, domain.getObjectClass(GridGame.CLASSAGENT), domain.getSingleActions());
		AgentType agentType1 = new AgentType(GridGame.CLASSAGENT, domain.getObjectClass(GridGame.CLASSAGENT), domain.getSingleActions());
		
		for (int i = 0; i < numSmart; i++) {
			MAValueIteration vi = 
					new MAValueIteration((SGDomain) domain, world.getActionModel(), rf, world.getTF(), 
							0.95, hashingFactory, 0., new CoCoQ(), 0.00015, 50);
			MultiAgentVFPlanningAgent a = new MultiAgentVFPlanningAgent((SGDomain) domain, vi, new PolicyFromJointPolicy(ja0));
			a.joinWorld(world, agentType0);
		}
		//RandomAgent a0 = new RandomAgent();
		//a0.joinWorld(world, agentType0);
		//a1.joinWorld(world, agentType0);
		for (int i = 0; i < numRandom; i++) {
			SleepyAgent a = new SleepyAgent(1.0);
			a.joinWorld(world, agentType0);
		}
	}
	
	public void addHumanAgent(World world, Domain domain) {
		
	}
	
	public static void main(String[] args) {
		int numberAgents = 4;
		int width = 2 * (int)((numberAgents - 2) / 4) + 5;
		int height = 2 * (int)((numberAgents - 4) / 4) + 5;
		
		Chicken chicken = new Chicken();
		Domain domain = chicken.generateDomain();
		
		World world = chicken.generateWorld(numberAgents, width, height);
		//world.setThreadTimeout(30.0);
		chicken.addAgents(world, domain, numberAgents, 4, 0);
		GameAnalysis analysis = world.runGame(10);
		Visualizer visualizer = GGVisualizer.getVisualizer(width, height);
		StateParser parser = new StateYAMLParser(domain);
		StateJSONParser json;
		String path = "/Users/brawner/";
		analysis.writeToFile(path + "episode1", parser);
		GameSequenceVisualizer sequenceVisualizer = new GameSequenceVisualizer(visualizer, (SGDomain)domain, parser, path);
		
		
	}

}
