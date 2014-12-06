package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

public class Server {
	final static int TOTAL_SERVER = 3;
	final static int MAJORITY = TOTAL_SERVER / 2 + 1;
	final static long TRANSACTION_TIMEOUT = 1000 * 4800;
	State state;

	enum State {
		STATE_START, STATE_TIMEOUT, STATE_CONFIRM, STATE_PREPARE, STATE_PROPOSER_ACCEPT, STATE_DECIDE, STATE_ACCEPTOR_ACCEPT
	}

	Log log;
	int serverNo;
	LogEntry currentOperation;
	Ballot currentBallot;
	boolean syncFlag;
	ServerTimer userTimer;

	double balance;

	boolean mode; // ISPAXOS or not

	Messenger messenger;
	Terminal terminal;

	List<ConfirmMessage> confirmList;
	int acceptCount;
	List<Message> recvMessageList;
	boolean serverSwitch;
	String command;
	Dispatcher dispatcher;

	Message message = null;
	LogEntry nextOperation = null;

	public Server(int serverNo) throws IOException {
		this.serverNo = serverNo;
		state = State.STATE_START;
		serverSwitch = true;
		currentOperation = null;
		currentBallot = null;
		userTimer = new ServerTimer();
		// initialize messagelist, confirmlist
		command = null;
		recvMessageList = new LinkedList<Message>();
		confirmList = new LinkedList<ConfirmMessage>();
		acceptCount = 0;
		// start messenger
		messenger = Messenger.getMessenger();
		// start dispatcher
		dispatcher = new Dispatcher(recvMessageList,
				messenger.getPort(serverNo));
		dispatcher.start();
		// start terminal
		terminal = new Terminal();
		terminal.start();
		syncFlag = true;
		// read in log
		log = new Log();
		balance = log.getBalance();
	}
	public static void main(String[] args) throws IOException {
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
		}
		while (true) {
			server.run();
		}
	}
	
	private boolean checkRedundantMessage(Message message) {
		if (message == null) return false;
		switch(message.getType()) {
		case ACCEPT: 
			AcceptMessage acceptMessage = (AcceptMessage)message;
			return acceptMessage.getAcceptLog().getLogPosition() == log.getLogLength();
		case DECIDE: 
			DecideMessage decideMessage = (DecideMessage)message;
			return decideMessage.getValue().getLogPosition() == log.getLogLength();
		default:
			return true;
		}		
	}
	
	public void run() throws UnknownHostException, IOException {
		// keep the last message and command unchanged
		if (serverSwitch)
			message = checkMessage();
		switch (state) {
		case STATE_START:
			if (message != null) {
				switch (message.getType()) {
				case ACCEPT:
					if(checkRedundantMessage(message)) break;
					AcceptMessage acceptMessage = (AcceptMessage) message;
					// get an new operation to agree on
					acceptCount = 1;
					currentBallot = acceptMessage.getBallot();
					currentOperation = acceptMessage.getAcceptLog();
					// forward the message
					acceptMessage.setReceiver(Messenger.BROADCAST);
					acceptMessage.setSender(serverNo);
					messenger.sendMessage(acceptMessage);
					acceptCount = 1;
					state = State.STATE_ACCEPTOR_ACCEPT;
					break;
				case DECIDE:
					if(checkRedundantMessage(message)) break;
					DecideMessage decideMessage = (DecideMessage) message;
					makeDecision(decideMessage.getValue());
					break;
				case PREPARE:
					PrepareMessage prepareMessage = (PrepareMessage) message;
					currentBallot = prepareMessage.getBallot();
					Message reply = new ConfirmMessage(MessageType.CONFIRM,
							serverNo, message.getSender(),
							prepareMessage.getBallot(), null, null);
					messenger.sendMessage(reply);
					acceptCount = 0;
					state = State.STATE_CONFIRM;
					break;
				default:
					System.out.println("Undefined State!");
				}
			}
			break;
		case STATE_PREPARE:
			if (userTimer.isOn() && userTimer.getTime() > TRANSACTION_TIMEOUT) {
				// no accept request has been sent
				notifyTerminal(false);
				state = State.STATE_START;
				synchronized (this) {
					recvMessageList.add(0, message); // no message consume
				}
				break;
			} else {
				if (message != null) {
					switch (message.getType()) {
					case PREPARE:
						PrepareMessage prepareMessage = (PrepareMessage) message;
						if (prepareMessage.getBallot().compareTo(currentBallot) > 0) {
							notifyTerminal(false);
							currentBallot = prepareMessage.getBallot();
							Message reply = new ConfirmMessage(
									MessageType.CONFIRM, serverNo,
									message.getSender(),
									prepareMessage.getBallot(), 
									currentBallot,
									null);
							messenger.sendMessage(reply);
							acceptCount = 0;
							state = State.STATE_CONFIRM;
						}
						break;
					case DECIDE:
						DecideMessage decideMessage = (DecideMessage) message;
						makeDecision(decideMessage.getValue());
						state = State.STATE_START;
						break;
					case CONFIRM: // it get this message because it started a
									// proposal
						ConfirmMessage confirmMessage = (ConfirmMessage) message;
						synchronized (this) {
							confirmList.add(confirmMessage);
						}
						// check how many confirm Message that has ballot that
						// is the
						// same as the currentBallot
						if (confirmList.size() < MAJORITY)
							break;
						LogEntry maxBallotOperation = selectOperation();
						Message acceptRequest;
						if (maxBallotOperation == null) {
							// broadcast accept request with currentOperation
							acceptRequest = new AcceptMessage(
									MessageType.ACCEPT, serverNo,
									Messenger.BROADCAST, currentBallot,
									currentOperation);
							acceptCount = 1;
							state = State.STATE_PROPOSER_ACCEPT;
						} else {
							// help with propagation of the other proposal
							acceptRequest = new AcceptMessage(
									MessageType.ACCEPT, serverNo,
									Messenger.BROADCAST, currentBallot,
									maxBallotOperation);
							notifyTerminal(false);
							acceptCount = 1;
							state = State.STATE_ACCEPTOR_ACCEPT;
						}
						messenger.sendMessage(acceptRequest);
						break;
					case ACCEPT:
						AcceptMessage acceptMessage = (AcceptMessage) message;
						if (acceptMessage.getBallot().compareTo(currentBallot) < 0)
							// if the message contains a ballot less than the
							// current
							// ballot, no response
							break;
						else {
							notifyTerminal(false);
							// get an new operation to agree on
							currentBallot = acceptMessage.getBallot();
							currentOperation = acceptMessage.getAcceptLog();
							// forward the message
							acceptMessage.setReceiver(Messenger.BROADCAST);
							acceptMessage.setSender(serverNo);
							messenger.sendMessage(acceptMessage);
							acceptCount = 1;
							state = State.STATE_ACCEPTOR_ACCEPT;
						}
					default:
						System.out.println("Undefined Message!");
					}
				}
			}
			break;
		case STATE_CONFIRM:
			if (message != null) {
				switch (message.getType()) {
				case DECIDE:
					DecideMessage decideMessage = (DecideMessage) message;
					makeDecision(decideMessage.getValue());
					break;
				case ACCEPT:
					AcceptMessage acceptMessage = (AcceptMessage) message;
					if (acceptMessage.getBallot().compareTo(currentBallot) < 0)
						// if the message contains a ballot less than the
						// current
						// ballot, no respond
						break;
					else {
						// get an new operation to agree on
						currentBallot = acceptMessage.getBallot();
						currentOperation = acceptMessage.getAcceptLog();
						// forward the message
						acceptMessage.setReceiver(Messenger.BROADCAST);
						acceptMessage.setSender(serverNo);
						messenger.sendMessage(acceptMessage);
						acceptCount = 1;
						confirmList.clear();
						state = State.STATE_ACCEPTOR_ACCEPT;
					}
					break;
				case PREPARE:
					PrepareMessage prepareMessage = (PrepareMessage) message;
					if (currentBallot.compareTo(prepareMessage.getBallot()) < 0) {
							Message reply = new ConfirmMessage(MessageType.CONFIRM,
								serverNo, message.getSender(),
								prepareMessage.getBallot(), currentBallot, null);
							currentBallot = prepareMessage.getBallot();
						messenger.sendMessage(reply);
					}
					confirmList.clear();
					// state wont change
					break;
				default:
					System.out.println("Undefined Message!");
				}
			}
			break;
		case STATE_PROPOSER_ACCEPT:
			if (userTimer.isOn() && userTimer.getTime() > TRANSACTION_TIMEOUT) {
				notifyTerminal(false);
				synchronized (this) {
					recvMessageList.add(0, message); // no message consume
				}
				break;
			}
			if (message != null) {
				switch (message.getType()) {
				case PREPARE:
					PrepareMessage prepareMessage = (PrepareMessage) message;

					if (currentBallot.compareTo(prepareMessage.getBallot()) < 0) {
						notifyTerminal(false);
						
						Message reply = new ConfirmMessage(MessageType.CONFIRM,
								serverNo, message.getSender(),
								prepareMessage.getBallot(), currentBallot, null);
						currentBallot = prepareMessage.getBallot();
						acceptCount = 0;
						messenger.sendMessage(reply);
						state = State.STATE_CONFIRM;
					}
					break;
				case DECIDE:
					DecideMessage decideMessage = (DecideMessage) message;
					makeDecision(decideMessage.getValue());
					state = State.STATE_START;
					break;
				case ACCEPT:
					AcceptMessage acceptMessage = (AcceptMessage) message;
					if (currentBallot.compareTo(acceptMessage.getBallot()) < 0) {
						notifyTerminal(false);
						acceptCount = 1;
						state = State.STATE_ACCEPTOR_ACCEPT;
						currentBallot = acceptMessage.getBallot();
						currentOperation = acceptMessage.getAcceptLog();
					} else if (currentBallot.compareTo(acceptMessage
							.getBallot()) == 0) {
						if ((++acceptCount) >= MAJORITY) {
							// decide on the value and broadcast decide
							Message newDecideMessage = new DecideMessage(
									MessageType.DECIDE, this.serverNo,
									Messenger.BROADCAST, currentOperation);
							// we dont periodically send decide message
							messenger.sendMessage(newDecideMessage);
							makeDecision(currentOperation);
						}
					}
					break;
				default:
					System.out.println("Redundant Message!");
				}
			}
			break;
		case STATE_ACCEPTOR_ACCEPT:
			if (message != null) {
				switch (message.getType()) {
				case PREPARE:
					PrepareMessage prepareMessage = (PrepareMessage) message;
					if (currentBallot.compareTo(prepareMessage.getBallot()) < 0) {

						Message reply = new ConfirmMessage(MessageType.CONFIRM,
								serverNo, message.getSender(),
								prepareMessage.getBallot(), currentBallot,
								currentOperation);
						currentBallot = prepareMessage.getBallot();
						acceptCount = 0;
						messenger.sendMessage(reply);
						state = State.STATE_CONFIRM;
					}
					break;
				case DECIDE:
					DecideMessage decideMessage = (DecideMessage) message;
					makeDecision(decideMessage.getValue());
					state = State.STATE_START;
					break;
				case ACCEPT:
					AcceptMessage acceptMessage = (AcceptMessage) message;
					if (currentBallot.compareTo(acceptMessage.getBallot()) < 0) {
						acceptCount = 1;
						currentBallot = acceptMessage.getBallot();
						currentOperation = acceptMessage.getAcceptLog();
					} else if (currentBallot.compareTo(acceptMessage
							.getBallot()) == 0) {
						if ((++acceptCount) >= MAJORITY) {
							// decide on the value and broadcast decide
							Message newDecideMessage = new DecideMessage(
									MessageType.DECIDE, this.serverNo,
									Messenger.BROADCAST, currentOperation);
							// we dont periodically send decide message
							messenger.sendMessage(newDecideMessage);
							makeDecision(currentOperation);
						}
					}
					break;
				default:
					System.out.println("Undefined Message!");
				}
			}
			break;
		default:
			System.out.println("Undefined State!");
		}
		if ((nextOperation = checkCommand()) != null) {
			state = State.STATE_PREPARE;
			startProposal(nextOperation);
			terminal.command = null;
			nextOperation = null;
		}
	}

	private LogEntry selectOperation() {
		Ballot recvMaxBallot = null;
		LogEntry maxBallotValue = null;
		for (int i = 0; i < confirmList.size(); i++) {
			ConfirmMessage recvConfirm = confirmList.get(i);
			if (recvMaxBallot == null
					|| recvMaxBallot.compareTo(recvConfirm.getAcceptBallot()) > 0) {
				maxBallotValue = recvConfirm.getValue();
				recvMaxBallot = recvConfirm.getAcceptBallot();
			}
		}
		return maxBallotValue;
	}

	private LogEntry checkCommand() throws UnknownHostException, IOException {
		synchronized (this) {
			command = terminal.getCommand();
		}
		if (command != null)
			return interpret(command);
		return null;
	}

	private Message checkMessage() {
		Message message = null;
		synchronized (this) {
			if (!recvMessageList.isEmpty())
				message = recvMessageList.remove(0);
		}
		return message;
	}

	private void notifyTerminal(boolean success) {
		String indicator = success ? "SUCCEED" : "FAIL";
		userTimer.turnOff();
		System.out
				.println("The Last Operation " + currentOperation + indicator);
		currentBallot = null;
		currentOperation = null;
		confirmList.clear();
	}

	public void startProposal(LogEntry nextOperation)
			throws UnknownHostException, IOException {
		if (!syncFlag)
			return;
		state = State.STATE_PREPARE;
		updateBallot();
		currentOperation = nextOperation;
		Message newProposal = new PrepareMessage(MessageType.PREPARE, serverNo,
				Messenger.BROADCAST, currentBallot);
		confirmList.clear();
		confirmList.add(new ConfirmMessage(MessageType.CONFIRM, serverNo,
				this.serverNo, this.currentBallot, null, null));
		acceptCount = 0;
		messenger.sendMessage(newProposal);
	}

	private void startSynchronization() throws UnknownHostException,
			IOException {
		Message syncReq = new SyncReqMessage(MessageType.SYNC_REQ, serverNo,
				Messenger.BROADCAST, log.getLogPosition());
		messenger.sendMessage(syncReq);
	}

	private void updateBallot() {
		if (currentBallot == null)
			currentBallot = new Ballot(1, serverNo);
		else
			currentBallot = new Ballot(currentBallot.getBallotNumber() + 1,
					serverNo);
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
		notifyTerminal(true);
		acceptCount = 0;
		state = State.STATE_START;
	}

	public LogEntry interpret(String s) throws UnknownHostException,
			IOException {
		if (s == null || s.length() == 0)
			return null;
		String command = s.trim().toLowerCase();
		char firstChar = command.charAt(0);
		userTimer.turnOn();
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
					currentOperation = new LogEntry("deposit", value,
							log.getLogPosition());
					if (!syncFlag)
						System.out.println("Unsynchronized!");
					else {
						return currentOperation;
						// startProposal();
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
					currentOperation = new LogEntry("deposit", value,
							log.getLogPosition());
					if (!syncFlag)
						System.out.println("Unsynchronized!");
					else {
						return currentOperation;
						// startProposal();
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
				} else
					System.out.println("unsynchronized");
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
		command = null;
		terminal.command = null;
		return null;
	}

	public void handleInvalidInput() {
		// ?? handle invalid input
		System.out.println("invalid input");
	}
}
