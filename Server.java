package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Semaphore;

public class Server {
	Log log;
	int serverNo;
	Ballot currentBallot;
	LogEntry currentOperation;
	boolean syncFlag;
	int lock = 0;
	final static int MAJORITY = 3;
	final static int TOTAL_SERVER = 5;
	final static long TIMEOUT = 100000;
	boolean mode;
	Messenger messenger;
	long confirmTimerStart;
	long prepareTimerStart;
	List<ConfirmMessage> confirmList = new LinkedList<ConfirmMessage>();
	List<Message> messageList = new LinkedList<Message>();
	List<String> commandList = new LinkedList<String>();
	Terminal commandInterpreter;

	Set<Integer> acceptMSources = new HashSet<Integer>();
	Ballot acceptedBallot = null;
	LogEntry acceptedValue = null;

	public Server(int serverNo) {
		this.serverNo = serverNo;
		currentBallot = null;
		currentVal = log.getLogPosition();
		messenger = Messenger.getMessenger();
		Thread dispatcher = new Dispatcher(messageList);
		dispatcher.run();
		Thread commandInterpreter = new Terminal(commandList);
		commandInterpreter.run();
	}

	private void resetConfirmTimer() {
		confirmTimerStart = System.currentTimeMillis();
	}

	private long getConfirmTimerPass() {
		return System.currentTimeMillis() - confirmTimerStart;
	}

	private long getLockTimerPass() {
		return System.currentTimeMillis() - lockTimerStart;
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
			int messageBallotNum = acceptMessage.ballot.ballotNumber;
			LogEntry messageValue =  acceptMessage.getAcceptLog();
			if(acceptedBallot == null || acceptedValue == null || messageBallotNum>acceptedBallot.ballotNumber){
				acceptedValue = messageValue;
				acceptedBallot = acceptMessage.ballot;
				acceptMSources.clear();
				acceptMSources.add(acceptMessage.getSender());
				// broadcast accept     update sender				
				
			}
			else if(messageBallotNum==acceptedBallot.ballotNumber){
				acceptMSources.add(acceptMessage.getSender());
				if(acceptMSources.size()>=MAJORITY-1){
					//broadcast decide;	
				}
									
			}			
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
				if (currentBallot.compareTo(prepareMessage.getBallot()) < 0) {
					reply = new ConfirmMessage(MessageType.CONFIRM, 
							   serverNo, 
							   message.getSender(),
							   prepareMessage.getBallot(), 
							   currentBallot, 
							   currentVal);
				}
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
			synchronized(this) {
				confirmList.add(confirmMessage);
				if (confirmList.size() != TOTAL_SERVER && getConfirmTimerPass() < TIMEOUT)
					break;
			}
			// check how many confirm Message that has ballot that is the same of the currentBallot 
			int	confirmRecvCount = 0;
			Ballot recvMaxBallot = null;
			LogEntry MaxBallotValue = null;
				for (int i = 0; i < confirmList.size(); i++) {
					Ballot temp = confirmList.get(i).getAcceptBallot();
					if (temp == null) {
						confirmRecvCount ++;
						continue;
					}
					else if (temp.compareTo(confirmList.get(i).getRecvBallot()) < 0){
						// get a fake reply, do nothing
						confirmRecvCount ++;
						if (recvMaxBallot == null || recvMaxBallot.compareTo(temp) < 0) {
							MaxBallotValue = confirmList.get(i).getAcceptValue();
							recvMaxBallot = temp;
						}
					}
				}
				
				if (confirmRecvCount > MAJORITY - 1) {
					if (MaxBallotValue == null){}
						//broadcast accept with currentOperation
					else {
						//broadcast accept with recvMaxBallot
					}
				}
				else {
					// startnew proposal
					
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
	public void startPreposal() {
		prepareTimerStart = System.currentTimeMillis();
		Message newProposal = new PrepareMessage();
		messenger.sendMessage(newProposal);
		lock++;
	}

	public static void main(String[] args) {
		Server server;
		String serverNumberString = null;
		if (args.length == 1) {
			serverNumberString = args[0];
		}
		while (true) {
			if (serverNumberString != null) {
				try {
					int value = Integer.parseInt(serverNumberString);
					server = new Server(value);
					break;
				} catch (NumberFormatException ex) {
					serverNumberString = getCorrestInput();
				}
			} else
				serverNumberString = getCorrestInput();
			;
		}
		while (true) {
			server.run();
		}
	}

	private static String getCorrestInput() {
		System.out.println("Please enter the server number:");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			String command = null;
			try {
				command = br.readLine();
				return command;
			} catch (IOException ioe) {
				System.out.println("IO error trying to read your command!");
			}
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
					Message newProposal = new PrepareMessage(
							MessageType.PREPARE, serverNo, Messenger.BROADCAST,
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
		// ?? handle invalid input
		System.out.println("invalid input");
	}

}
