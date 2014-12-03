package server;

import java.util.List;

enum MessageType {
    ACCEPT, PREPARE, SYNC_REQ, SYNC_ACK, CONFIRM, DECIDE
}
abstract class Message {
	/*	Message Format:
	 * 	Field	Content
	 * 	SRC		sender's server no
	 * 	DES		receiver's server no
	 * 	TYPE	message type
	 * 	BODY	see subclasses
	 * */
	final static char DELIMIT = '\t';
	final static char MSG_END = '$';
	int sender;
	int receiver;
	MessageType type;
	public Message(MessageType type, int sender, int receiver) {
		this.type = type;
		this.sender = sender;
		this.receiver = receiver;
	}
	public String translate() {
		StringBuilder header = new StringBuilder();
		header.append(String.valueOf(sender) + DELIMIT);
		header.append(String.valueOf(receiver) + DELIMIT);
		header.append(type);
		header.append(DELIMIT);
		return header.toString();
	}
}

class Ballot {
	int ballotNumber;
	int serverNumber;
}

class AcceptMessage extends Message {
	/*	BODY
	 * 	Field		Content
	 * 	BAL_NO		ballot number
	 * 	BAL_SERNO	ballot server number 	
	 * 	LOG_POS		log position	
	 * */
	Ballot ballot;
	int logPosition;
	public AcceptMessage(MessageType type, int sender, int receiver) {
		super(type, sender, receiver);
	}
	public String translate() {
		StringBuilder message = new StringBuilder();
		message.append(super.translate());
		message.append(String.valueOf(ballot.ballotNumber) + DELIMIT);
		message.append(String.valueOf(ballot.serverNumber) + DELIMIT);
		message.append(String.valueOf(logPosition) + DELIMIT);
		message.append(String.valueOf(MSG_END));
		return message.toString();
	}
}

class DecideMessage extends Message {
	/*	BODY
	 * 	Field		Content
	 * 	LOG_POS		log position	
	 * */
	int logPosition;
	public DecideMessage(MessageType type, int sender, int receiver){
		super(type, sender, receiver);
	}
	public String translate() {
		StringBuilder message = new StringBuilder();
		message.append(super.translate());
		message.append(String.valueOf(logPosition) + DELIMIT);
		message.append(String.valueOf(MSG_END));
		return message.toString();
	}
}

class PrepareMessage extends Message {
	/*	BODY
	 * 	Field		Content
	 * 	BAL_NO		ballot number
	 * 	BAL_SERNO	ballot server number 	
	 * */
	Ballot ballot;
	public PrepareMessage(MessageType type, int sender, int receiver){
		super(type, sender, receiver);
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
	/*	BODY
	 * 	Field		Content
	 * 	ACC_BAL_NO		accept ballot number
	 * 	ACC_BAL_SERNO	accept ballot server number
	 * 	REC_BAL_NO		received ballot number
	 * 	REC_BAL_SERNO	received ballot server number
	 * 	ACC_VALUE		accepted value 	
	 * */
	Ballot acceptBallot;
	Ballot recvBallot;
	int acceptValue;
	public ConfirmMessage(MessageType type, int sender, int receiver){
		super(type, sender, receiver);
	}
	public String translate() {
		StringBuilder message = new StringBuilder(super.translate());
		message.append(String.valueOf(acceptBallot.ballotNumber) + DELIMIT);
		message.append(String.valueOf(acceptBallot.serverNumber) + DELIMIT);
		message.append(String.valueOf(recvBallot.ballotNumber) + DELIMIT);
		message.append(String.valueOf(recvBallot.serverNumber) + DELIMIT);
		message.append(String.valueOf(acceptValue) + DELIMIT);
		message.append(String.valueOf(MSG_END));
		return message.toString();
	}
}

class SyncReqMessage extends Message {
	/*	BODY
	 * 	Field		Content
	 * 	LOG_LEN		log length
	 * */
	int logLength;
	public SyncReqMessage(MessageType type, int sender, int receiver){
		super(type, sender, receiver);
	}
	public String translate() {
		StringBuilder message = new StringBuilder(super.translate());
		message.append(String.valueOf(logLength) + DELIMIT);
		message.append(String.valueOf(MSG_END));
		return message.toString();
	}
}

class SyncAckMessage extends Message {
	/*	BODY
	 * 	Field				Content
	 * 	LOG_OPERATION		log operation
	 * 	LOG_OPERAND			log operand
	 * 	... 	
	 * */
	List<LogEntry> recentLog;
	public SyncAckMessage(MessageType type, int sender, int receiver){
		super(type, sender, receiver);
	}
	public String translate() {
		StringBuilder message = new StringBuilder(super.translate());
		for (int i = 0; i < recentLog.size(); i ++) {
			message.append(String.valueOf(recentLog.get(i).operation) + DELIMIT);
			message.append(String.valueOf(recentLog.get(i).operand) + DELIMIT);
		}
		message.append(String.valueOf(MSG_END));
		return message.toString();
	}
}

