package server;
public class MessageFactory {
	public static Message create(MessageType type) {
		Message message = null;
		switch(type) {
		case ACCEPT:
			message = new AcceptMessage();
			break;
		case PREPARE:
			message = new PrepareMessage();
			break;
		case DECIDE:
			message = new DecideMessage();
			break;
		case CONFIRM:
			message = new ConfirmMessage();
			break;
		case SYNC_REQ:
			message = new SyncReqMessage();
			break;
		case SYNC_ACK:
			message = new SyncAckMessage();
			break;
		default:
		}
		return message;
	}
}


