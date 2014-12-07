package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Semaphore;

public class Terminal extends Thread {
	String command;
	Boolean lock;
	
	public Terminal(Boolean lock) {
		this.lock = lock;
	}
	
	public String getCommand() {
		return command;
	}
	
	@Override
	public void run() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			System.out.println("Please input your command:");
			try {
				synchronized(this) {
					command = br.readLine();
					System.out.println(command);
				}
				synchronized(lock) {
					lock.notify();
				}
			} catch (IOException ioe) {
				System.out.println("IO error trying to read your command!");
				System.exit(1);
			}

		}

	}

}
