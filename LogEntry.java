package server;
public class LogEntry implements Comparable<LogEntry>{
	String operation;
	double operand;
	int logPosition;

	LogEntry(String operation, double operand, int position) {
		this.operation = operation;
		this.operand = operand;
		this.logPosition = position;
	}

	@Override
	public int compareTo(LogEntry log) {
		if(log.operation.equals(this.operation) && log.operand ==this.operand && log.logPosition==this.logPosition)
			return 0;
		return -1;
	}
}