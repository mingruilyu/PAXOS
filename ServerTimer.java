package server;

import java.util.Timer;
import java.util.TimerTask;
class Notification {
	
	boolean notification;
	Notification(boolean notification) {
		this.notification = notification;
	}
}
public class ServerTimer {
	private Timer timer;
	Notification notification;
	public void cancel() {
		timer.cancel();
	}
	ServerTimer(Server server, long expireTime, boolean notify) {
		timer = new Timer(true);
		TimerAction action = new TimerAction(server, notify);
		timer.schedule(action, expireTime);
		notification = action.getNotification();
	}	
}

class TimerAction extends TimerTask {
	private Server server;
	private Notification notification;
	private boolean notify;
	public Notification getNotification() {
		return notification;
	}
	public TimerAction(Server server, boolean notify) {
		this.server = server;
		this.notification = new Notification(false);
		this.notify = notify;
	}
	@Override
	public void run() {
		notification.notification = true;
		if (notify) {
			System.out.println("USERTIMER TIMEOUT");
			server.notifyTerminal(false);
			server.state = Server.State.STATE_START;
		}
		else {
			System.out.println("WAITTIMER TIMEOUT");
		}
	}
	
}
