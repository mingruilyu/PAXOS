package server;

import java.util.Timer;
import java.util.TimerTask;

public class ServerTimer {
	private Timer timer;
	Boolean notification;
	public void cancel() {
		timer.cancel();
	}
	ServerTimer(Server server, long expireTime, boolean notify) {
		timer = new Timer(true);
		timer.schedule(new TimerAction(server, notification, notify), expireTime);
		notification = new Boolean(false);
	}	
}

class TimerAction extends TimerTask {
	private Server server;
	private Boolean notification;
	private boolean notify;
	public TimerAction(Server server, Boolean notification, boolean notify) {
		this.server = server;
		this.notification = notification;
	}
	@Override
	public void run() {
		notification = true;
		if (notify == true) {
			server.notifyTerminal(false);
			server.state = Server.State.STATE_START;
		}
	}
	
}
