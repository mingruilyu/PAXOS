package server;

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
	 * 
	 * 
	 * */
	int sender;
	int receiver;
	public abstract String translate();
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
	 * 	
	 * 		
	 * */
	Ballot ballot;
	int logPosition;
	public String translate() {
		String
		return 
	}
}

class DecideMessage extends Message {
	int logPosition;
	public String translate() {
		
	}
}

class PrepareMessage extends Message {
	Ballot ballot;
	public String translate() {
		
	}
}

class ConfirmMessage extends Message {
	Ballot acceptBallot;
	Ballot recvBallot;
	int acceptValue;
	public String translate() {
		
	}
}

class SyncReqMessage extends Message {
	int logLength;
	public String translate() {
		
	}
}

class SyncAckMessage extends Message {
	List<LogEntry> recentLog; 
	public String translate() {
		
	}
}

