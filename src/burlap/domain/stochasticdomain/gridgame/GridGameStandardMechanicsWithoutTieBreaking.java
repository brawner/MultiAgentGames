package burlap.domain.stochasticdomain.gridgame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
import burlap.mdp.core.Domain;

/**
 * Do NOT use this with semi-walls (full transitions are not well defined).
 *
 * @author Stephen Brawner and James MacGlashan.
 */
public class GridGameStandardMechanicsWithoutTieBreaking extends GridGameStandardMechanics {

	public GridGameStandardMechanicsWithoutTieBreaking(Domain d) {
		super(d);
	}
	
	public GridGameStandardMechanicsWithoutTieBreaking(Domain domain, double semiWallProb) {
		super(domain, semiWallProb);
	}
	
	/**
	 * Overrides Standard mechanics by allowing no agent to win when in collision
	 */
	@Override
	protected Map <Integer, Integer> getWinningAgentMovements(Map<Integer, List<Integer>> collissionSets) {
		return new HashMap<Integer, Integer>();
	}

}
