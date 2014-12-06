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
	public String getOperation() {
		return operation;
	}
	public double getOperand() {
		return operand;
	}
	public int getLogPosition() {
		return logPosition;
	}
	public String toString() {
		return operation + "\t" + operand + "in LogPosition " + logPosition;
	}
	@Override
	public int compareTo(LogEntry log) {
		if (log == null) return 1;
		if(log.operation.equals(this.operation) && log.operand ==this.operand && log.logPosition==this.logPosition)
			return 0;
		return -1;
	}
}