import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;

public class Log {
	LinkedList<LogEntry> logs = new LinkedList<LogEntry>();
	boolean synchFlag;

	Log() {
		load();
	}

	private void load() {

		try {
			String pathname = "E:\\2014Fall\\CS271\\Paxos\\log\\log.txt";
			File logFile = new File(pathname);
			if (!logFile.exists()) {
				return;
			}

			InputStreamReader reader = new InputStreamReader(
					new FileInputStream(logFile));
			BufferedReader br = new BufferedReader(reader);
			String line = br.readLine();
			while (line != null) {
				String[] array = line.split("\\s+");
				Operation operation = Operation.getEnumFromString(array[0]);
				double operand = Double.parseDouble(array[1]);
				appendLogEntry(new LogEntry(operation, operand));
				line = br.readLine();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void dump() {

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(
					"E:\\2014Fall\\CS271\\Paxos\\log\\log.txt"));
			for (LogEntry e : logs) {
				out.write(e.operation + "  " + e.operand + "\r\n");
			}
			out.close();
		} catch (IOException e) {
		}
	}

	boolean verifyLogs(double currentBalance) {
		double sum = 0;
		for (LogEntry e : logs) {
			switch (e.operation) {
			case Withdraw:
				sum = sum - e.operand;
				break;
			case Deposit:
				sum = sum + e.operand;
				break;

			}
		}
		return sum == currentBalance;
	}

	boolean appendLogEntry(LogEntry e) {
		if (e == null)
			return false;

		logs.add(e);
		return true;

	}

	boolean synchronizeLogLists(LinkedList<LogEntry> list) {
		if (list == null)
			return false;
		for (LogEntry e : list) {
			logs.add(e);
		}
		return true;

	}

}