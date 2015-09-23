package domain;

import java.util.List;

import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.states.State;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;

public class TwoAgentsTwoGoals {

	private static SGDomain generateDomain(GridGame gridGame) {
		SGDomain domain = (SGDomain) gridGame.generateDomain();
		return domain;
	}
	
	private static JointActionModel generateJointActionModel(Domain domain) {
		return new GridGameStandardMechanics(domain);
	}
	
	private static TerminalFunction generateTerminalFunction(SGDomain domain) {
		return new GridGame.GGTerminalFunction(domain);
	}
	
	public static JointReward generateJointReward(SGDomain domain) {
		return new GridGame.GGJointRewardFunction(domain);
	};
	
	

	private static SGStateGenerator generateStateGenerator(final Domain domain, final int version) {
		
		return new SGStateGenerator() {
			private State generateStateGeneratorType0(final Domain domain, List<Agent> agents) {
				State state = GridGame.getCleanState(domain, agents, 2, 2, 2, 4, 5, 5);
				
				GridGame.setAgent(state, 0, 4, 4, 0);
				GridGame.setAgent(state, 1, 0, 0, 1);
				GridGame.setVerticalWall(state, 2, 4, 4, 5, 0);
				GridGame.setVerticalWall(state, 3, 1, 0, 0, 0);
				GridGame.setGoal(state, 0, 0, 4, 1);
				GridGame.setGoal(state, 1, 4, 0, 2);
				return state;
			}
			
			private State generateStateGeneratorType1(final Domain domain, List<Agent> agents) {
				State state = GridGame.getCleanState(domain, agents, 2, 2, 3, 3, 5, 5);
				
				GridGame.setAgent(state, 0, 0, 4, 0);
				GridGame.setAgent(state, 1, 4, 0, 1);
				GridGame.setVerticalWall(state, 2, 1, 4, 5, 0);
				GridGame.setHorizontalWall(state, 2, 1, 4, 5, 0);
				GridGame.setGoal(state, 0, 4, 0, 1);
				GridGame.setGoal(state, 1, 0, 4, 2);
				return state;
			}
			
			private State generateStateGeneratorType2(final Domain domain, List<Agent> agents) {
				State state = GridGame.getCleanState(domain, agents, 2, 2, 2, 2, 5, 5);
				
				GridGame.setAgent(state, 0, 1, 3, 0);
				GridGame.setAgent(state, 1, 3, 1, 1);
				GridGame.setGoal(state, 0, 4, 0, 1);
				GridGame.setGoal(state, 1, 0, 4, 2);
				return state;
			}
			
			private State generateStateGeneratorType3(final Domain domain, List<Agent> agents) {
				State state = GridGame.getCleanState(domain, agents, 2, 2, 4, 2, 5, 5);
				
				GridGame.setAgent(state, 0, 0, 2, 0);
				GridGame.setAgent(state, 1, 4, 2, 1);
				
				GridGame.setHorizontalWall(state, 2, 3, 0, 0, 0);
				GridGame.setHorizontalWall(state, 3, 2, 4, 4, 0);
				
				
				GridGame.setGoal(state, 0, 4, 2, 1);
				GridGame.setGoal(state, 1, 0, 2, 2);
				return state;
			}
			
			private State generateStateGeneratorType4(final Domain domain, List<Agent> agents) {
				State state = GridGame.getCleanState(domain, agents, 2, 2, 2, 4, 5, 5);
				
				GridGame.setAgent(state, 0, 4, 4, 0);
				GridGame.setAgent(state, 1, 0, 0, 1);
				GridGame.setVerticalWall(state, 2, 3, 0, 0, 1);
				GridGame.setVerticalWall(state, 3, 2, 4, 4, 1);
				
				
				GridGame.setGoal(state, 0, 0, 4, 1);
				GridGame.setGoal(state, 1, 4, 0, 2);
				return state;
			}
			
			@Override
			public State generateState(List<Agent> agents) {
				switch(version) {
				case 0:
					return this.generateStateGeneratorType0(domain, agents);
				case 1:
					return this.generateStateGeneratorType1(domain, agents);
				case 2:
					return this.generateStateGeneratorType2(domain, agents);
				case 3:
					return this.generateStateGeneratorType3(domain, agents);
				case 4:
					return this.generateStateGeneratorType4(domain, agents);
				}
				throw new RuntimeException("Invalid version");
			}
			
		};
	}
	public static World generateWorld(int version) {
		GridGame gridGame = new GridGame();
		SGDomain domain = TwoAgentsTwoGoals.generateDomain(gridGame);
		JointActionModel jointActionModel = TwoAgentsTwoGoals.generateJointActionModel(domain);
		TerminalFunction terminalFunction = TwoAgentsTwoGoals.generateTerminalFunction(domain);
		JointReward jointReward = new GridGame.GGJointRewardFunction(domain); //this.generateJointReward();
		
		SGStateGenerator stateGenerator = TwoAgentsTwoGoals.generateStateGenerator(domain, version);
		World world = new World((SGDomain)domain, jointActionModel, jointReward, terminalFunction, stateGenerator);
		world.setDescription("Two agents, two goals, type " + version);
		
		return world;
	}
}
