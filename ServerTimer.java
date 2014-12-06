package server;

public class ServerTimer {
	long startTime;
	boolean switchTimer;
	public boolean isOn() {
		return switchTimer;
	}
	public void turnOn() {
		switchTimer = true;
	}
	public void turnOff() {
		switchTimer = false;
	}
	public void resetTimer() {
		startTime = System.currentTimeMillis();
	}
	public long getTime() {
		return System.currentTimeMillis() - startTime;
	}
}
