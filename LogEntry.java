package server;

public class LogEntry implements Comparable<LogEntry> {
	String operation;
	double operand;
	int logPosition;
	int serverNo;
	int sequenceNo;

	LogEntry(String operation, double operand, int position, int serverNo,
			int sequenceNo) {
		this.operation = operation;
		this.operand = operand;
		this.logPosition = position;
		this.serverNo= serverNo;
		this.sequenceNo = sequenceNo;
	}

	LogEntry(String operation, double operand) {
		this.operation = operation;
		this.operand = operand;
		logPosition = 0;
		serverNo = 0;
		sequenceNo = 0;
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
		if (log == null)
			return 1;
		if (this.serverNo==log.serverNo && this.sequenceNo == log.sequenceNo)
			return 0;
		return -1;
	}
}