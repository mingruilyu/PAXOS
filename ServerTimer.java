package server;

public class ServerTimer {
	long startTime;
	public void resetTimer() {
		startTime = System.currentTimeMillis();
	}
	public long getTime() {
		return System.currentTimeMillis() - startTime;
	}
}
