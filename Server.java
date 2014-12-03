package server;

import java.util.LinkedList;
import java.util.List;

public class Server {
	Log log;
	int serverNo;
	Ballot currentBallot;
	int currentVal;
	LogEntry currentOperation;
	boolean syncFlag;
	Messenger messenger;
	List<Message> messageList = new LinkedList<Message>();
	List<String> commandList = new LinkedList<String>();
	Terminal commandInterpreter;
	public Server(int serverNo){
		this.serverNo = serverNo;
		currentBallot = null;
		currentVal = log.getLogPosition();
		messenger = Messenger.getMessenger();
		Thread dispatcher = new Dispatcher(messageList);
		dispatcher.run();
		Thread commandInterpreter = new Terminal(commandList);
		commandInterpreter.run();
	}
	
	public void run() {
		Message message = null;
		String command = null;
		Message reply = null;
		synchronized(this) {
			if(!messageList.isEmpty()) 
				message = messageList.remove(0);
		}
		
		synchronized(this) {
			if(!commandList.isEmpty())
				command = commandList.remove(0);
		}
		if (message == null && command == null) return;
		else if (command != null)
			interpret(command);
		switch(message.getType()) {
		case ACCEPT:
			AcceptMessage acceptMessage = (AcceptMessage)message;

			currentVal = acceptMessage.getLogPosition();
			break;
		case PREPARE: 
			PrepareMessage prepareMessage = (PrepareMessage)message;
			if (currentBallot == null) {
				reply = new ConfirmMessage(MessageType.CONFIRM, 
								   serverNo, 
								   message.getSender(),
								   prepareMessage.getBallot(), 
								   null, 
								   currentVal);
				currentBallot = prepareMessage.getBallot();
			}
			else {
				reply = new ConfirmMessage(MessageType.CONFIRM, 
						   serverNo, 
						   message.getSender(),
						   prepareMessage.getBallot(), 
						   currentBallot, 
						   currentVal);
				if (currentBallot.compareTo(prepareMessage.getBallot()) < 0)
					currentBallot = prepareMessage.getBallot();
			}
			break;
		case SYNC_REQ: 
			SyncReqMessage syncReqMessage = (SyncReqMessage)message;
			List<LogEntry> complement = log.compareLog(syncReqMessage.getLogLength());
			reply = new SyncAckMessage(MessageType.SYNC_ACK,
									serverNo, 
									syncReqMessage.getSender(),
									complement);
			break;
		case SYNC_ACK: 
			SyncAckMessage syncAckMessage = (SyncAckMessage)message;
			log.synchronizeLogLists(syncAckMessage.getRecentLog());
			break;
		case CONFIRM:
			ConfirmMessage confirmMessage = (ConfirmMessage)message;
			if (confirmMessage.getAcceptValue() == Message.NULL_VALUE) {
				if (confirmMessage.getAcceptBallot().compareTo(currentBallot) > 0) {
					updateBallot(confirmMessage.getAcceptBallot());
					reply = new PrepareMessage(MessageType.PREPARE,
								serverNo,
								message.getSender(),
								currentBallot);
				}
				else {
					reply = new AcceptMessage(MessageType.ACCEPT,
							serverNo,
							message.getSender(),
							currentBallot,
							currentVal);
				}
			}
			else {
				if (confirmMessage.getAcceptBallot().compareTo(currentBallot) < 0) {
					reply = new AcceptMessage(MessageType.ACCEPT, 
							serverNo,
							Messenger.BROADCAST,
							currentBallot, 
							confirmMessage.getAcceptValue());
				}
			}
			break;
		case DECIDE: 
			DecideMessage decideMessage = (DecideMessage)message;
			reply = decideMessage;
			break;
		}
		if (reply != null)
			messenger.sendMessage(reply);
	}
	
	public static void main(String[] args) {
		
		Server server = new Server();
		while(true) {
			server.run();
		}
	}
	
	private void updateBallot(Ballot ballot) {
		currentBallot.ballotNumber = ballot.getBallotNumber() + 1;
	}
	
	public void interpret(String s) {
		if (s == null || s.length() == 0)
			return;
		String command = s.trim().toLowerCase();
		char firstChar = command.charAt(0);
		switch (firstChar) {
		case 'd':
			String[] depositCommand = command.split("\\(");
			if (depositCommand.length == 2
					&& depositCommand[0].equals("deposit")) {
				String valueString = depositCommand[1].trim().substring(0,
						depositCommand[1].trim().length() - 1);
				try {
					double value = Double.parseDouble(valueString);
					LogEntry currentOperation = value;
					Message newProposal = new PrepareMessage(MessageType.PREPARE,
							serverNo,
							Messenger.BROADCAST,
							currentBallot);
					messenger.sendMessage(newProposal);
					System.out.println("deposit  " + value);
				} catch (NumberFormatException ex) {
					handleInvalidInput();
				}

			} else
				handleInvalidInput();
			break;
		case 'w':
			String[] withdrawCommand = command.split("\\(");
			if (withdrawCommand.length == 2
					&& withdrawCommand[0].equals("withdraw")) {
				String valueString = withdrawCommand[1].trim().substring(0,
						withdrawCommand[1].trim().length() - 1);
				try {
					double value = Double.parseDouble(valueString);
					// ?? call withdraw method
					System.out.println("withdraw  " + value);
				} catch (NumberFormatException ex) {
					handleInvalidInput();
				}

			} else
				handleInvalidInput();
			break;

		case 'f':
			if (s.equals("fail()")) {
				// ?? fail the server
				System.out.println("fail the server");
			} else
				handleInvalidInput();

			break;
		case 'b':
			if (s.equals("balance()")) {
				// ?? get Balance
				System.out.println("get Balance ");
			} else {
				handleInvalidInput();
			}
			break;
		case 'u':
			if (s.equals("unfail()")) {
				// ?? unfail the server
				System.out.println("unfail the server");
			} else {
				handleInvalidInput();
			}
			break;
		default:
			handleInvalidInput();
		}
	}

	public void handleInvalidInput() {
		//  ?? handle invalid input
		System.out.println("invalid input");
	}

}

