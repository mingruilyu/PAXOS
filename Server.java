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
	final static int MAJORITY = 3;
	final static int TOTAL_SERVER = 5;
	final static long WAIT_TIMEOUT = 1000;
	final static long TRANSACTION_TIMEOUT = 5000;
	Log log;
	int serverNo;
	LogEntry currentOperation;
	Ballot currentBallot;
	LogEntry decidedOperation;
	boolean syncFlag;
	ServerTimer userTimer;
	
	double balance;
	
	boolean mode; // ISPAXOS or not
	
	Messenger messenger;
	Terminal terminal;
	ServerTimer timer;
	
	List<ConfirmMessage> confirmList;
	int acceptCount;
	List<Message> recvMessageList;
	boolean serverSwitch;
	String command;
	Dispatcher dispatcher;
	//Semaphore terminalLock = new Semaphore(1);
	
	public Server(int serverNo) {
		this.serverNo = serverNo;
		//isProposer = false;	
		serverSwitch =true;
		currentOperation = null;
		currentBallot = null;
		timer = new ServerTimer();
		// initialize messagelist, confirmlist
		command = null;
		recvMessageList = new LinkedList<Message>();
		confirmList = new LinkedList<ConfirmMessage>();
		acceptCount = 0;
		// start messenger
		messenger = Messenger.getMessenger();
		// start dispatcher
		dispatcher = new Dispatcher(recvMessageList, messenger.getPort(serverNo));
		dispatcher.run();
		// start terminal
		terminal = new Terminal();
		terminal.run();
		syncFlag = true;
		// read in log
		log = new Log();
		balance = log.getBalance();
	}
	public void run() {
		Message message = null;
		Message reply = null;
		synchronized(this) {
			if(!recvMessageList.isEmpty()) 
				message = recvMessageList.remove(0);
		}
		
		synchronized(this) {
			command = terminal.getCommand();
		}
		if (syncFlag && message == null && command == null) return;
		if (command != null)
			interpret(command);
		if (message != null && serverSwitch) {
		switch(message.getType()) {
		case ACCEPT:
			if (userTimer.getTime() > TRANSACTION_TIMEOUT) {
				notifyTerminal(false);
				acceptCount = 0;
				break;
			}
			AcceptMessage acceptMessage = (AcceptMessage)message;
			if (acceptMessage.getBallot().compareTo(currentBallot) < 0)
				// if the message contains a ballot less than the current ballot, no respond
				break;
			else if (acceptMessage.getBallot().compareTo(currentBallot) == 0 
					&& acceptMessage.getAcceptLog().compareTo(currentOperation) == 0) {
				// get an support from the other server
				if ((++ acceptCount) >= MAJORITY - 1) {
					// decide on the value and broadcast decide
					makeDecision(currentOperation);
					Message decideMessage = new DecideMessage(MessageType.DECIDE,
														this.serverNo, 
														Messenger.BROADCAST,
														currentOperation);
					// we dont periodically send decide message
					acceptCount = 0;
					messenger.sendMessage(decideMessage);
				}
			}
			else {
				// ballot number equal but operation unequal is not possible
			//	if (!isProposer) {
					// get an new operation to agree on
					acceptCount = 0;
					currentBallot = acceptMessage.getBallot();
					currentOperation = acceptMessage.getAcceptLog();
					// forward the message
					acceptMessage.setReceiver(Messenger.BROADCAST);
					messenger.sendMessage(acceptMessage);
			//	}
			}
			
			break;
		case PREPARE:
			PrepareMessage prepareMessage = (PrepareMessage)message;
			if (currentBallot == null) {
				// this server is not proposer
				currentBallot = prepareMessage.getBallot();
				reply = new ConfirmMessage(MessageType.CONFIRM, 
								   serverNo, 
								   message.getSender(),
								   prepareMessage.getBallot(), 
								   null, 
								   null);
			}
			else {
				if (currentBallot.compareTo(prepareMessage.getBallot()) < 0) {
						reply = new ConfirmMessage(MessageType.CONFIRM, 
							   		serverNo, 
							   		message.getSender(),
							   		prepareMessage.getBallot(), 
							   		currentBallot, 
									currentOperation);
				}
			}
			messenger.sendMessage(reply);
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
			if (!syncFlag) {
				log.synchronizeLogLists(syncAckMessage.getRecentLog());
				syncFlag = true;
			}
			break;
		case CONFIRM:
			// it get this message because it start a proposal
			ConfirmMessage confirmMessage = (ConfirmMessage)message;
			synchronized(this) {
				confirmList.add(confirmMessage);
				if (confirmList.size() != TOTAL_SERVER && timer.getTime() < WAIT_TIMEOUT)
					break;
			}
			// check how many confirm Message that has ballot that is the same as the currentBallot 
			Ballot recvMaxBallot = null;
			LogEntry maxBallotValue = null;
			for (int i = 0; i < confirmList.size(); i++) {
				ConfirmMessage recvConfirm = confirmList.get(i);
					if (recvMaxBallot.compareTo(recvConfirm.getAcceptBallot()) > 0) {
							maxBallotValue = recvConfirm.getValue();
							recvMaxBallot = recvConfirm.getAcceptBallot();
					}
			}
			if (confirmList.size() >= MAJORITY - 1) {
				Message acceptRequest;
				if (maxBallotValue == null){
					//broadcast accept request with currentOperation
					acceptRequest = new AcceptMessage(MessageType.ACCEPT,
							serverNo, 
							Messenger.BROADCAST,
							currentBallot, 
							currentOperation);
				}
				else {
					//broadcast accept with recvMaxBallot
					acceptRequest = new AcceptMessage(MessageType.ACCEPT,
							serverNo, 
							Messenger.BROADCAST,
							currentBallot,
							maxBallotValue);
				}
				messenger.sendMessage(acceptRequest);
			}
			else {
				//return failure
				notifyTerminal(false);
			}
			break;
		case DECIDE: 
			DecideMessage decideMessage = (DecideMessage)message;
			// two reception of decision on the same operation is not possible
			if (decideMessage.getValue().compareTo(decidedOperation) != 0)
				makeDecision(decideMessage.getValue());
			break;
		}
	}
	}
	
	private void notifyTerminal(boolean success) {
		terminal.clearCommand();
		String indicator = success ? "SUCCEED" : "FAIL";
		System.out.println("The Last Operation " + currentOperation + indicator);
	}
	
	public void startProposal() {
		if (!syncFlag) return;
		updateBallot();
		Message newProposal = new PrepareMessage(MessageType.PREPARE, 
											serverNo, 
											Messenger.BROADCAST,
											currentBallot);
		timer.resetTimer();
		acceptCount = 0;
		messenger.sendMessage(newProposal);
	}

	private void startSynchronization() {
		Message syncReq = new SyncReqMessage(MessageType.SYNC_REQ, 
				serverNo, 
				Messenger.BROADCAST,
				log.getLogPosition());
		messenger.sendMessage(syncReq);
	}
	
	private void updateBallot() {
		if (currentBallot == null)
			currentBallot = new Ballot(1, serverNo);
		else 
			currentBallot = new Ballot(currentBallot.getBallotNumber() + 1, serverNo);
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
	
	private void makeDecision(LogEntry operation) {
		log.appendLogEntry(operation);
		currentBallot = null;		
		currentOperation = null;
		System.out.println(operation);
		timer.resetTimer();
		confirmList.clear();
		decidedOperation = operation;
	}

	public void interpret(String s) {
		if (s == null || s.length() == 0)
			return;
		String command = s.trim().toLowerCase();
		char firstChar = command.charAt(0);
		userTimer.resetTimer();
		switch (firstChar) {
		case 'd':
			String[] depositCommand = command.split("\\(");
			if (depositCommand.length == 2
					&& depositCommand[0].equals("deposit")) {
				String valueString = depositCommand[1].trim().substring(0,
						depositCommand[1].trim().length() - 1);
				try {
					double value = Double.parseDouble(valueString);
					// start proposal with currentBallot and currentOperation
					currentOperation = new LogEntry("deposit", value, log.getLogPosition());
					if (!syncFlag) 
						System.out.println("Unsynchronized!");
					else {
						startProposal();
						System.out.println("deposit  " + value);
					}
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
					// start proposal with currentBallot and currentOperation
					currentOperation = new LogEntry("deposit", value, log.getLogPosition());
					if (!syncFlag) 
						System.out.println("Unsynchronized!");
					else {
						startProposal();
						System.out.println("withdraw  " + value);
					}
				} catch (NumberFormatException ex) {
					handleInvalidInput();
				}

			} else
				handleInvalidInput();
			break;

		case 'f':
			if (s.equals("fail()")) {
				syncFlag = false;
				serverSwitch = false;
				System.out.println("fail the server");
			} else
				handleInvalidInput();

			break;
		case 'b':
			if (s.equals("balance()")) {
				if (syncFlag) {
					System.out.println("get Balance ");
					System.out.println(balance);
				}
				else System.out.println("unsynchronized");
			} else {
				handleInvalidInput();
			}
			break;
		case 'u':
			if (s.equals("unfail()")) {
				serverSwitch = true;
				startSynchronization();
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
