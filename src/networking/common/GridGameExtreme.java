package networking.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Arrays;
import java.util.List;

import burlap.domain.stochasticgames.gridgame.GGVisualizer;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanicsWithoutTieBreaking;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.objects.ObjectInstance;
import burlap.oomdp.core.states.State;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.visualizer.RenderLayer;
import burlap.oomdp.visualizer.Visualizer;

public class GridGameExtreme {
	public static String CLASSREWARD = "reward";
	public static String ATTVALUE = "value";
	public static String PFINREWARD = "in_reward";
		
	public static SGDomain generateDomain(GridGame gridGame, boolean randomlyBreakTies) {
		//gridGame.setMaxDim(5);
		Domain domain = gridGame.generateDomain();
		SGDomain sgDomain = (SGDomain)domain;
		if (!randomlyBreakTies) {
			double semiWallProb = gridGame.getSemiWallProb();
			JointActionModel model = new GridGameStandardMechanicsWithoutTieBreaking(domain, semiWallProb);
			sgDomain.setJointActionModel(model);
		}
		
//		Attribute xAtt = domain.getAttribute(GridGame.ATTX);
//		Attribute yAtt = domain.getAttribute(GridGame.ATTY);
//		Attribute cost = new Attribute(domain, ATTVALUE, Attribute.AttributeType.INT);
//		cost.setDiscValuesForRange(-Integer.MAX_VALUE, -Integer.MAX_VALUE, 1);
//		domain.addAttribute(cost);
//		
		//AgentInReward agentInReward = new AgentInReward(PFINREWARD, domain);
		return sgDomain;
	}

	public static SGDomain generateDomain(GridGame gridGame) {
		return generateDomain(gridGame, true);
	}
	
	public static Visualizer getVisualizer(World world) {
		State startState = world.startingState();
		List<ObjectInstance> horizontalWalls = startState.getObjectsOfClass(GridGame.CLASSDIMHWALL);
		List<ObjectInstance> verticalWalls = startState.getObjectsOfClass(GridGame.CLASSDIMVWALL);
		if (horizontalWalls.size() < 2 || verticalWalls.size() < 2) {
			throw new RuntimeException("This world does not have valid walls");
		}
		
		ObjectInstance leftWall = verticalWalls.get(0);
		ObjectInstance rightWall = verticalWalls.get(1);
		ObjectInstance bottomWall = horizontalWalls.get(0);
		ObjectInstance topWall = horizontalWalls.get(1);
		
		final int cWidth = rightWall.getIntValForAttribute(GridGame.ATTP) - leftWall.getIntValForAttribute(GridGame.ATTP);
		final int cHeight = topWall.getIntValForAttribute(GridGame.ATTP) - bottomWall.getIntValForAttribute(GridGame.ATTP); 
		
		Visualizer visualizer = GGVisualizer.getVisualizer(cWidth, cHeight);
		List<Color> colors = Arrays.asList(Color.RED);
		
		RenderLayer layer = new RenderLayer(){

			@Override
			public void render(Graphics2D g2, float width, float height) {
				BasicStroke s = new BasicStroke(2.0f);
				g2.setStroke(s);
				float columnWidth = width / cWidth;
				float columnHeight = height / cHeight;
				
				for (int i = 0; i < width; i++) {
					int x = (int)(i*columnWidth);
					Point bottom = new Point(x, 0);
					Point top = new Point(x, (int)height);
					g2.draw(new java.awt.geom.Line2D.Float(bottom, top));

				}
				for (int i = 0; i < width; i++) {
					int y = (int) (i * columnHeight);
					Point left = new Point( 0, y);
					Point right = new Point((int) width, y);
					g2.draw(new java.awt.geom.Line2D.Float(left, right));

				}
			}
		};
		visualizer.addRenderLayer(layer);
		visualizer.insertObjectClassPainter(0, GridGameExtreme.CLASSREWARD, new GGVisualizer.CellPainter(cWidth,cHeight, colors, 0));
		return visualizer;
	}
}
