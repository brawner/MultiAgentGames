package burlap.domain.stochasticdomain.gridgame;

import burlap.mdp.auxiliary.StateMapping;
import burlap.mdp.core.state.State;

public class NullStateMapping implements StateMapping{

	public NullStateMapping() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public State mapState(State s) {
		return s;
	}

}
