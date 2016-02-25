package experiments;

import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.auxiliary.common.NullAbstraction;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.stochasticgames.common.ConstantSGStateGenerator;

public class Testing {
	
	public static void main (String[] args){
		
		GridGame test = new GridGame();
		SGDomain domain = (SGDomain)test.generateDomain();
		
		World w = new World(domain, new GridGame.GGJointRewardFunction(domain), 
				new GridGame.GGTerminalFunction(domain), 
				new ConstantSGStateGenerator(GridGame.getPrisonersDilemmaInitialState(domain)), new NullAbstraction(), 2);
		

	}

}
