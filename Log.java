package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class Log {
	LinkedList<LogEntry> logs = new LinkedList<LogEntry>();
	boolean synchFlag;

	public Log() {
		logs = new LinkedList<LogEntry>();
		load();
	}
	public int getLogPosition() {
		return logs.size();
	}
	
	private void load() {

		try {
			String pathname = "log.txt";
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
				//Operation operation = Operation.getEnumFromString(array[0]);
				double operand = Double.parseDouble(array[1]);
				appendLogEntry(new LogEntry(array[0], operand));				
				line = br.readLine();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void dump() {

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(
					"log.txt"));
			for (LogEntry e : logs) {
				out.write(e.operation + "  " + e.operand + "\r\n");
			}
			out.close();
		} catch (IOException e) {
		}
	}
	public int getLogLength(){
		return logs.size();
	}
	public boolean verifyLogs(double currentBalance) {
		double sum = 0;
		for (LogEntry e : logs) {
			switch (e.operation.toLowerCase()) {
			case "withdraw":
				sum = sum - e.operand;
				break;
			case "deposit":
				sum = sum + e.operand;
				break;

			}
		}
		return sum == currentBalance;
	}
	
	public double getBalance() {
		double sum = 0;
		for (LogEntry e : logs) {
			switch (e.operation.toLowerCase()) {
			case "withdraw":
				sum = sum - e.operand;
				break;
			case "deposit":
				sum = sum + e.operand;
				break;

			}
		}
		return sum;
	}

	boolean appendLogEntry(LogEntry e) {
		if (e == null)
			return false;

		logs.add(e);
		return true;

	}

	boolean synchronizeLogLists(List<LogEntry> list) {
		if (list == null)
			return false;
		for (LogEntry e : list) {
			logs.add(e);
		}
		return true;

	}
	
	List<LogEntry> compareLog(int logLength) {
		List<LogEntry> complement = new LinkedList<LogEntry>();
		if (logLength > logs.size()) {
			System.out.println("Unsynchronized log is longer than the current log!");
			return null;
		}
		for (int i = logLength; i < logs.size(); i ++)
			complement.add(logs.get(i));
		return complement;
	}
}
