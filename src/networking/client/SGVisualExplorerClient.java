package networking.client;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Map;

import networking.common.GridGameServerToken;
import networking.common.TokenCastException;
import networking.server.GameHandler;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.explorers.SGVisualExplorer;
import burlap.oomdp.visualizer.Visualizer;


public class SGVisualExplorerClient extends SGVisualExplorer implements GGWebSocketListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3506642340442364172L;

	private final GGWebSocketClient socketClient;
	public SGVisualExplorerClient(SGDomain domain, Visualizer painter,
			State baseState, JointActionModel jam, GGWebSocketClient socketClient) {
		super(domain, painter, baseState, jam);
		this.socketClient = socketClient;
		
	}
	
	@Override
	protected void handleKeyPressed(KeyEvent e) {
		long start  = System.nanoTime();
		super.handleKeyPressed(e);
		String key = String.valueOf(e.getKeyChar());
		String mappedAction = this.getKeyAction(key);

		if(mappedAction != null){
			GroundedSingleAction nextAction = this.parseIntoSingleActions(mappedAction);
			this.attemptAction(nextAction);
			System.out.println(nextAction.toString());
			
		}
		
		long end = System.nanoTime();
		System.out.println("Time: " + (end - start)/1000000000.0);
		System.out.println(this.toString() + " " + e.toString());
	}
	
	public GridGameServerToken onMessage(GridGameServerToken msg) {
		GridGameServerToken response = new GridGameServerToken();
		try {
			String msgType = msg.getString(GameHandler.MSG_TYPE);
			if (msgType == null) {
				return new GridGameServerToken();
			} else if (msgType.equals(GameHandler.UPDATE)) {
				GridGameServerToken updateToken = msg.getToken(GameHandler.UPDATE);
				State state = updateToken.getState(GameHandler.STATE, domain);
				GridGameServerToken actionToken = updateToken.getToken(GameHandler.ACTION);
				JointAction jointAction = GridGameServerToken.jointActionFromToken(actionToken, this.domain);
				Map<String, Double> reward = (Map<String, Double>)updateToken.getObject(GameHandler.REWARD);
				Boolean isTerminal = updateToken.getBoolean(GameHandler.IS_TERMINAL);
				this.updateScreen(state, jointAction, reward, isTerminal);
			}
		} catch (TokenCastException e) {
			response.setError(true);
		}
	
		return response;
	}
	
	private void updateScreen(State state, JointAction action, Map<String, Double> reward, boolean isTerminal) {
		this.updateState(state);
		this.printText(action.toString() + "\n" + reward.toString() + "\n" + "Terminal: " + isTerminal);
	}
	
	private void attemptAction(GroundedSingleAction action) {
		GridGameServerToken request = new GridGameServerToken();
		
		request.setString(GameHandler.MSG_TYPE, GameHandler.TAKE_ACTION);
		request.setString(GameHandler.ACTION, action.actionName());
		request.setStringList(GameHandler.ACTION_PARAMS, Arrays.asList(action.params));
		
		
		this.socketClient.sendMessage(request);
	}

}
