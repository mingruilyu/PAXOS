package server;

import java.util.LinkedList;
import java.util.List;

public class Test {
	public static void main(String[] args) {
		
		//test for log
		/*Log log = new Log();
		log.appendLogEntry(new LogEntry(Operation.WITHDRAW,10.9));
		log.appendLogEntry(new LogEntry(Operation.DEPOSIT,11.8));
		log.dump();*/
		
		Thread Terminal = new Terminal(new LinkedList<String>());
		//test for command interpreter
			
		
		
	}
}
