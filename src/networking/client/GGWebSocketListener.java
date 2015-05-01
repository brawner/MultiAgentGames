package networking.client;
import networking.common.GridGameServerToken;


public interface GGWebSocketListener {
	GridGameServerToken onMessage(GridGameServerToken msg);
}
