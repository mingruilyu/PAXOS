package server;
public class LogEntry implements Comparable<LogEntry>{
	String operation;
	double operand;

	LogEntry(String operation, double operand) {
		this.operation = operation;
		this.operand = operand;
	}

	@Override
	public int compareTo(LogEntry log) {
		if(log.operation.equals(this.operation) && log.operand ==this.operand)
			return 0;
		return -1;
	}
}