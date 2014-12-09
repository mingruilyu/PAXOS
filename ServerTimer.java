package server;

import java.util.Timer;
import java.util.TimerTask;

public class ServerTimer {
	private Timer timer;
	Boolean notification;
	public void cancel() {
		timer.cancel();
	}
	ServerTimer(Server server, long expireTime) {
		timer = new Timer(true);
		timer.schedule(new TimerAction(server, notification), expireTime);
		notification = new Boolean(false);
	}	
}

class TimerAction extends TimerTask {
	private Server server;
	private Boolean notification;
	public TimerAction(Server server, Boolean notification) {
		this.server = server;
		this.notification = notification;
	}
	@Override
	public void run() {
		notification = true;
		server.notifyTerminal(false);
	}
	
}
