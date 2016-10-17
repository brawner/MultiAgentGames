package networking.client;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class GGWebSocketServlet extends WebSocketServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8854487478890320039L;

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.register(GGWebSocket.class);

	}

}
