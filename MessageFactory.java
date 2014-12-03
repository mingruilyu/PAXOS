package server;
public class MessageFactory {
	public static Message create(MessageType type, int sender, int receiver) {
		Message message = null;
		switch(type) {
		case ACCEPT:
			message = new AcceptMessage(type, sender, receiver);
			break;
		case PREPARE:
			message = new PrepareMessage(type, sender, receiver);
			break;
		case DECIDE:
			message = new DecideMessage(type, sender, receiver);
			break;
		case CONFIRM:
			message = new ConfirmMessage(type, sender, receiver);
			break;
		case SYNC_REQ:
			message = new SyncReqMessage(type, sender, receiver);
			break;
		case SYNC_ACK:
			message = new SyncAckMessage(type, sender, receiver);
			break;
		default:
		}
		return message;
	}
}


