package Analysis;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.util.List;

import javax.swing.JFrame;

import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.PolicyRenderLayer;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.ArrowActionGlyph;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.PolicyGlyphPainter2D;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.states.State;
import burlap.oomdp.visualizer.Visualizer;

public class PolicyExplorerGUI extends JFrame {

	private Visualizer visualizer;
	private PolicyGlyphPainter2D spp;
	/**
	 * 
	 */
	private static final long serialVersionUID = -7498749475584425183L;

	public PolicyExplorerGUI(Visualizer visualizer, List<State> states, Policy policy) throws HeadlessException {
		super();
		this.visualizer = visualizer;
		this.visualizer.setPreferredSize(new Dimension(500, 300));
		
		PolicyGlyphPainter2D spp = ArrowActionGlyph.getNSEWPolicyGlyphPainter(GridGame.CLASSAGENT, GridGame.ATTX, GridGame.ATTY,
				GridGame.ACTIONNORTH, GridGame.ACTIONSOUTH, GridGame.ACTIONEAST, GridGame.ACTIONWEST);
		spp.setNumXCells(5);
		spp.setNumYCells(3);
		PolicyRenderLayer renderLayer = new PolicyRenderLayer(states, spp, policy);
		
		this.visualizer.addRenderLayer(renderLayer);
	}

	public void initGUI() {
		this.setPreferredSize(new Dimension(500, 300));
		this.visualizer.setPreferredSize(new Dimension(500, 300));
		this.pack();
		
		this.getContentPane().add(this.visualizer, BorderLayout.CENTER);
		this.setEnabled(true);
		this.setVisible(true);
	}
	
	public void updateState(State s){
		this.visualizer.updateState(s);
		
	}

}
