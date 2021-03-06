package server;


import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

enum MessageType {
	ACCEPT, PREPARE, SYNC_REQ, SYNC_ACK, CONFIRM, DECIDE
}

abstract class Message {

	/*
	 * Message Format: Field Content SRC sender's server no DES receiver's
	 * server no TYPE message type BODY see subclasses
	 */
	final static char DELIMIT = '\t';
	final static char MSG_END = '$';

	final static int NULL_VALUE = -1;
	int sender;
	int receiver;
	MessageType type;

	public MessageType getType() {
		return type;
	}

	public Message(MessageType type, int receiver, int sender) {
		this.type = type;
		this.receiver = receiver;
		this.sender = sender;
	}

	public void setReceiver(int receiver) {
		this.receiver = receiver;
	}

	public int getSender() {
		return sender;
	}

	public void setSender(int sender) {
		this.sender = sender;
	}

	public int getReceiver() {
		return receiver;
	}

	public String translate() {
		StringBuilder header = new StringBuilder();
		header.append(String.valueOf(sender) + DELIMIT);
		header.append(String.valueOf(receiver) + DELIMIT);
		header.append(type);
		header.append(DELIMIT);
		return header.toString();
	}

	public static Message parseMessage(String messageString) {
		String[] headerParts = messageString.split(
				String.valueOf(Message.DELIMIT), 4);
		int sender = Integer.parseInt(headerParts[0]);
		int receiver = Integer.parseInt(headerParts[1]);
		String[] bodyParts = headerParts[3].split(String
				.valueOf(Message.DELIMIT));

		switch (headerParts[2]) {
		case "PREPARE":
			int ballotNumber = Integer.parseInt(bodyParts[0]);
			int serverNumber = Integer.parseInt(bodyParts[1]);
			Ballot ballot = new Ballot(ballotNumber, serverNumber);
			return new PrepareMessage(MessageType.PREPARE, sender, receiver,
					ballot);

		case "CONFIRM":
			Ballot acceptBallot = null;
			LogEntry log = null;

			int recvBallotNumber = Integer.parseInt(bodyParts[0]);
			int recvServerNumber = Integer.parseInt(bodyParts[1]);
			Ballot recvBallot = new Ballot(recvBallotNumber, recvServerNumber);

			if (!bodyParts[2].equals("NULL") && !bodyParts[3].equals("NULL")) {
				int acceptBallotNumber = Integer.parseInt(bodyParts[2]);
				int acceptServerNumber = Integer.parseInt(bodyParts[3]);
				acceptBallot = new Ballot(acceptBallotNumber,
										acceptServerNumber);
			}
			if (!bodyParts[4].equals("NULL") && !bodyParts[5].equals("NULL") && !bodyParts[6].equals("NULL"))
				log = new LogEntry(bodyParts[4],
						Double.parseDouble(bodyParts[5]), 
						Integer.parseInt(bodyParts[6]));
			return new ConfirmMessage(MessageType.CONFIRM, sender, receiver,
					 recvBallot, acceptBallot, log);

		case "ACCEPT":
			Ballot acceptedBallot = new Ballot(Integer.parseInt(bodyParts[0]),
					Integer.parseInt(bodyParts[1]));
			LogEntry acceptLog = new LogEntry(bodyParts[2],
					Double.parseDouble(bodyParts[3]), Integer.parseInt(bodyParts[4]));
			return new AcceptMessage(MessageType.ACCEPT, sender, receiver,
					acceptedBallot, acceptLog);

		case "DECIDE":			
			LogEntry decideLog = new LogEntry(bodyParts[0],
					Double.parseDouble(bodyParts[1]), Integer.parseInt(bodyParts[2]));
			return new DecideMessage(MessageType.DECIDE, sender, receiver,
					decideLog);
		case "SYNC_REQ":
			int logLength = Integer.parseInt(bodyParts[0]);
			return new SyncReqMessage(MessageType.SYNC_REQ, sender, receiver,
					logLength);
		case "SYNC_ACK":
			List<LogEntry> logs = new LinkedList<LogEntry>();
			for (int i = 0; i < bodyParts.length; i = i + 2) {
				LogEntry logEntry = new LogEntry(bodyParts[i],
						Double.parseDouble(bodyParts[i + 1]), Integer.parseInt(bodyParts[i + 2]));
				logs.add(logEntry);
			}
			return new SyncAckMessage(MessageType.SYNC_ACK, sender, receiver,
					logs);
		}
		return null;
	}

}


class BallotComparator implements Comparator<Ballot> {

	@Override
	public int compare(Ballot bal1, Ballot bal2) {
		if (bal1 == null && bal2 == null) return 0;
		else if (bal1 == null) return -1;
		else if (bal2 == null) return -1;
		else return bal1.compareTo(bal2);
	}
	
}

class Ballot implements Comparable<Ballot> {
	int ballotNumber;
	int serverNumber;

	public Ballot(int ballotNumber, int serverNumber) {
		this.ballotNumber = ballotNumber;
		this.serverNumber = serverNumber;
	}

	@Override
	public int compareTo(Ballot another) {
		if (another == null) return 1;
		if (this.ballotNumber != another.ballotNumber)
			return this.ballotNumber - another.ballotNumber;
		else
			return this.serverNumber - another.serverNumber;
	}

	public int getBallotNumber() {
		return ballotNumber;
	}

	public int getServerNumber() {
		return serverNumber;
	}
}

class AcceptMessage extends Message {
	/*
	 * BODY Field Content BAL_NO ballot number BAL_SERNO ballot server number
	 * LOG_POS log position
	 */
	Ballot ballot;
	LogEntry acceptLog;

	public AcceptMessage(MessageType type, int sender, int receiver,
			Ballot ballot, LogEntry acceptLog) {
		super(type, receiver, sender);
		this.ballot = ballot;
		this.acceptLog = acceptLog;
	}

	public String translate() {
		StringBuilder message = new StringBuilder();
		message.append(super.translate());
		message.append(String.valueOf(ballot.ballotNumber) + DELIMIT);
		message.append(String.valueOf(ballot.serverNumber) + DELIMIT);
		message.append(String.valueOf(acceptLog.operation) + DELIMIT);
		message.append(String.valueOf(acceptLog.operand) + DELIMIT);
		message.append(String.valueOf(acceptLog.logPosition) + DELIMIT);
		message.append(String.valueOf(MSG_END));
		return message.toString();
	}

	public Ballot getBallot() {
		return ballot;
	}

	public LogEntry getAcceptLog() {
		return acceptLog;
	}
}

class DecideMessage extends Message {
	/*
	 * BODY Field Content 
	 * LOG_POS operation
	 * LOG_BAL log ballot
	 */
	LogEntry value;
	public LogEntry getValue() {
		return value;
	}
	public DecideMessage(MessageType type, int sender, int receiver,
			LogEntry value) {
		super(type, receiver,sender);
		this.value = value;
	}

	public String translate() {
		StringBuilder message = new StringBuilder();
		message.append(super.translate());
		message.append(String.valueOf(value.operation) + DELIMIT);
		message.append(String.valueOf(value.operand) + DELIMIT);
		message.append(String.valueOf(value.logPosition) + DELIMIT);
		message.append(String.valueOf(MSG_END));
		return message.toString();
	}
}

class PrepareMessage extends Message {
	/*
	 * BODY Field Content BAL_NO ballot number BAL_SERNO ballot server number
	 */
	Ballot ballot;

	public PrepareMessage(MessageType type, int sender, int receiver, Ballot ballot) {
		super(type, receiver, sender);
		this.ballot = ballot;
	}

	public Ballot getBallot() {
		return ballot;
	}

	public String translate() {
		StringBuilder message = new StringBuilder();
		message.append(super.translate());
		message.append(String.valueOf(ballot.ballotNumber) + DELIMIT);
		message.append(String.valueOf(ballot.serverNumber) + DELIMIT);
		message.append(String.valueOf(MSG_END));
		return message.toString();
	}
}

class ConfirmMessage extends Message {
	/*
	 * BODY Field Content ACC_BAL_NO accept ballot number ACC_BAL_SERNO accept
	 * ballot server number REC_BAL_NO received ballot number REC_BAL_SERNO
	 * received ballot server number ACC_VALUE accepted value
	 */
	Ballot acceptBallot;
	Ballot recvBallot;
	LogEntry value;

	public LogEntry getValue() {
		return value;
	}

	public ConfirmMessage(MessageType type, int sender, int receiver,
			Ballot recvB, Ballot acceptB, LogEntry acceptValue) {
		super(type, receiver, sender);
		this.acceptBallot = acceptB;
		this.recvBallot = recvB;
		this.value = acceptValue;
	}

	public Ballot getAcceptBallot() {
		return acceptBallot;
	}

	public Ballot getRecvBallot() {
		return recvBallot;
	}

	

	public String translate() {
		StringBuilder message = new StringBuilder(super.translate());


		message.append(String.valueOf(recvBallot.ballotNumber) + DELIMIT);
		message.append(String.valueOf(recvBallot.serverNumber) + DELIMIT);
		if (acceptBallot != null) {
			message.append(String.valueOf(acceptBallot.ballotNumber) + DELIMIT);
			message.append(String.valueOf(acceptBallot.serverNumber) + DELIMIT);
		} else {
			message.append("NULL" + DELIMIT);
			message.append("NULL" + DELIMIT);
		}
		if (value == null) {
			message.append("NULL" + DELIMIT);
			message.append("NULL" + DELIMIT);
			message.append("NULL" + DELIMIT);
		}
		else {
			message.append(String.valueOf(value.operation) + DELIMIT);
			message.append(String.valueOf(value.operand) + DELIMIT);
			message.append(String.valueOf(value.logPosition) + DELIMIT);
		}
		message.append(String.valueOf(MSG_END));
		return message.toString();
	}
}

class SyncReqMessage extends Message {
	/*
	 * BODY Field Content LOG_LEN log length
	 */
	int logLength;

	public SyncReqMessage(MessageType type, int sender, int receiver,
			int logLength) {
		super(type, receiver, sender);
		this.logLength = logLength;
	}

	public int getLogLength() {
		return logLength;
	}

	public String translate() {
		StringBuilder message = new StringBuilder(super.translate());
		message.append(String.valueOf(logLength) + DELIMIT);
		message.append(String.valueOf(MSG_END));
		return message.toString();
	}
}

class SyncAckMessage extends Message {
	/*
	 * BODY Field Content LOG_OPERATION log operation LOG_OPERAND log operand
	 * ...
	 */
	List<LogEntry> recentLog;

	public SyncAckMessage(MessageType type, int sender, int receiver,
			List<LogEntry> recentLog) {

		super(type, receiver, sender);
		this.recentLog = recentLog;
	}

	public List<LogEntry> getRecentLog() {
		return recentLog;
	}

	public String translate() {
		StringBuilder message = new StringBuilder(super.translate());
		for (int i = 0; i < recentLog.size(); i++) {
			message.append(String.valueOf(recentLog.get(i).operation) + DELIMIT);
			message.append(String.valueOf(recentLog.get(i).operand) + DELIMIT);
			message.append(String.valueOf(recentLog.get(i).logPosition) + DELIMIT);
		}
		message.append(String.valueOf(MSG_END));
		return message.toString();
	}

}
