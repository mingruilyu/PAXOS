package server;

public class Test {
	public static void main(String[] args) {
		
		//test for log
		Log log = new Log();
		log.appendLogEntry(new LogEntry(Operation.WITHDRAW,10.9));
		log.appendLogEntry(new LogEntry(Operation.DEPOSIT,11.8));
		log.dump();
		
		
		//test for command interpreter
		CommondInterpreter  ci= new CommondInterpreter();
		//System.out.println("deposit(100.01)");		
		ci.interpret("deposit(100.01)");
		ci.interpret("withdraw(130.01)");
		ci.interpret("balance()");
		ci.interpret("fail()");
		ci.interpret("unfail()");
		ci.interpret(" Deposit(   100.01  )   ");
		ci.interpret("widthdraw(130.01");
		ci.interpret("balance(R234)");
		
	}
}
