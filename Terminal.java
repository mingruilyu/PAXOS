package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class Terminal implements Runnable {

	List<String> commandList;
	public Terminal(List<String> list ){
		commandList = list;
	}
	
	@Override
	public void run() {

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			String command = null;			
			// readLine() method
			try {
				command = br.readLine();
				commandList.add(command);
			} catch (IOException ioe) {
				System.out.println("IO error trying to read your command!");
				System.exit(1);
			}

		}

	}

}
