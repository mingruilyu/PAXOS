package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

public class Server {
	final static int TOTAL_SERVER = 3;
	final static int MAJORITY = TOTAL_SERVER / 2 + 1;
	final static long TRANSACTION_TIMEOUT = 5000;
	final static long ACKWAIT_TIMEOUT = 1000;
	State state;

	enum State {
		STATE_START, STATE_TIMEOUT, STATE_CONFIRM, STATE_PREPARE, STATE_PROPOSER_ACCEPT, STATE_DECIDE, STATE_ACCEPTOR_ACCEPT
	}

	Log log;
	int serverNo;
	List<LogEntry> currentOperation;
	Ballot currentBallot;
	boolean syncFlag;
	ServerTimer userTimer;
	ServerTimer waitTimer;
	double balance;
	int sequenceNo;

	boolean mode; // ISPAXOS or not

	Messenger messenger;
	Terminal terminal;

	List<ConfirmMessage> confirmList;
	int acceptCount;
	List<Message> recvMessageList;
	boolean serverSwitch;
	String command;
	Dispatcher dispatcher;
	Boolean lock;
	Message message = null;
	List<LogEntry> nextOperation = null;

	public Server(int serverNo) throws IOException {
		sequenceNo = 0;
		this.serverNo = serverNo;
		state = State.STATE_START;
		lock = new Boolean(true);
		serverSwitch = true;
		currentOperation = null;
		currentBallot = null;
		// initialize messagelist, confirmlist
		command = null;
		recvMessageList = new LinkedList<Message>();
		confirmList = new LinkedList<ConfirmMessage>();
		acceptCount = 0;
		// start messenger
		messenger = Messenger.getMessenger();
		// start dispatcher
		dispatcher = new Dispatcher(recvMessageList,
				messenger.getPort(serverNo), lock);
		dispatcher.start();
		// start terminal
		terminal = new Terminal(lock);
		terminal.start();
		syncFlag = true;
		// read in log
		log = new Log();
		balance = log.getBalance();
		mode = true;
	}

	private boolean hasMessage() {
		boolean flag;
		synchronized (this) {
			flag = recvMessageList.isEmpty();
		}
		return !flag;
	}

	private boolean hasCommand() {
		boolean flag;
		synchronized (this) {
			flag = (command != null);
		}
		return flag;
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
			if (!server.hasMessage() && !server.hasCommand())
				server.doSuspend();
			server.run();
		}
	}

	private void doSuspend() {
		synchronized (lock) {
			try {
				lock.wait();
			} catch (InterruptedException ex) {
			}
		}
	}
	private boolean compareLists(List<LogEntry> list1, List<LogEntry> list2) {
		if (list1 == null && list2 != null) return false;
		if (list1 != null && list2 == null) return false;
		if (list1 == null && list2 == null) return true;
		if (list1.size() != list2.size()) return false;
		
		for (int i = 0; i < list1.size();i ++) {
			if (list1.get(i).compareTo(list2.get(i)) != 0)
				return false;
			
		}
		return true;
	}
	private boolean checkRedundantMessage(Message message) {
		if (message == null)
			return false;
		switch (message.getType()) {
		case ACCEPT:
			AcceptMessage acceptMessage = (AcceptMessage) message;
			return log.checkOperations(acceptMessage.getAcceptLog());
		case DECIDE:
			DecideMessage decideMessage = (DecideMessage) message;
			return log.checkOperations(decideMessage.getValue());
		default:
			return false;
		}
	}

	public void run() throws UnknownHostException, IOException {
		// keep the last message and command unchanged
		// System.out.println("running server");
		if (serverSwitch) 
			message = getMessage();
		if (message != null && checkRedundantMessage(message)) {
			message = null;
		}
		switch (state) {
		case STATE_START:
			if (message != null) {
				System.out.println("STATE: " + state);
				System.out.println("MESSAGE: " + message.translate());
				System.out.println("ACCEPTCOUNT: " + acceptCount);
				if (currentBallot != null)
					System.out.println("CURRENTBALLOT: "
							+ currentBallot.toString());
				else
					System.out.println("CURRENTBALLOT: " + "NULL");
				System.out.println("CURRENTVAL: " + currentOperation);
				System.out.println("CONFIRMLIST" + confirmList.size());
				switch (message.getType()) {
				case ACCEPT:
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
					//if (checkRedundantMessage(message))
						//break;
					DecideMessage decideMessage = (DecideMessage) message;
					currentOperation = decideMessage.getValue();
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
				case SYNC_REQ:
					sendSynAck(message);
					break;
				case SYNC_ACK:
					if(!syncFlag)
						appendSynAck(message);
					break;
				
				default:
					System.out.println("Redundant Message!");
				}
			}
			break;
		case STATE_PREPARE:
			if (userTimer.notification) {
				// no accept request has been sent
				notifyTerminal(false);
				state = State.STATE_START;
				synchronized (this) {
					recvMessageList.add(0, message); // no message consume
				}
				break;
			} else {
				if (message != null) {
					System.out.println("STATE: " + state);
					System.out.println("MESSAGE: " + message.translate());
					System.out.println("ACCEPTCOUNT: " + acceptCount);
					if (currentBallot != null)
						System.out.println("CURRENTBALLOT: "
								+ currentBallot.toString());
					else
						System.out.println("CURRENTBALLOT: " + "NULL");
					System.out.println("CURRENTVAL: " + currentOperation);
					System.out.println("CONFIRMLIST" + confirmList.size());

					switch (message.getType()) {
					case PREPARE:
						PrepareMessage prepareMessage = (PrepareMessage) message;
						if (prepareMessage.getBallot().compareTo(currentBallot) > 0) {
							notifyTerminal(false);
							currentBallot = prepareMessage.getBallot();
							Message reply = new ConfirmMessage(
									MessageType.CONFIRM, serverNo,
									message.getSender(),
									prepareMessage.getBallot(), currentBallot,
									null);
							messenger.sendMessage(reply);
							acceptCount = 0;
							state = State.STATE_CONFIRM;
						}
						break;
					case DECIDE:
						//if (checkRedundantMessage(message))
							//break;
						DecideMessage decideMessage = (DecideMessage) message;
						currentOperation = decideMessage.getValue();
						notifyTerminal(false);
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
						if (confirmList.size() < TOTAL_SERVER  - 1 && !waitTimer.notification) {
							break;
						}
						if(waitTimer!=null)
							waitTimer.cancel();
						boolean nullFlag = true;
						for (int i = 0; i < confirmList.size(); i ++) {
							if(confirmList.get(i).getValue() != null)
								nullFlag = false;
						}
						Message acceptRequest = null;
						if (nullFlag) {
							acceptRequest = new AcceptMessage(
									MessageType.ACCEPT, serverNo,
									Messenger.BROADCAST, currentBallot,
									currentOperation);

							acceptCount = 1;
							state = State.STATE_PROPOSER_ACCEPT;
						}
						else {
							int[] entryMap = {1, 0, 0, 0, 0};
							for(int i = 1; i < confirmList.size(); i ++){
								for(int j = 0; j < i; j ++) {
									if (compareLists(confirmList.get(i).getValue(), confirmList.get(j).getValue()))
										entryMap[j] ++;
									else entryMap[i] = 1;
								}
							}
							int maxPos = 0;
							for (int i = 0; i < entryMap.length; i ++){
								if (entryMap[i] > entryMap[maxPos]) {
									maxPos = i;
								}
							}
							if(!mode && entryMap[maxPos] + TOTAL_SERVER - confirmList.size() <= TOTAL_SERVER / 2){
								generateCombinedValue(confirmList.get(maxPos).getValue());
								acceptRequest = new AcceptMessage(
										MessageType.ACCEPT, serverNo,
										Messenger.BROADCAST, currentBallot,
										currentOperation);

								acceptCount = 1;
								state = State.STATE_PROPOSER_ACCEPT;
							}
								
							else if (confirmList.size() >= MAJORITY - 1){	 
								List<LogEntry> maxBallotOperation = selectOperation();
									acceptRequest = new AcceptMessage(
											MessageType.ACCEPT, serverNo,
											Messenger.BROADCAST, currentBallot,
											maxBallotOperation);
									currentOperation = maxBallotOperation;

									acceptCount = 1;
									state = State.STATE_PROPOSER_ACCEPT;
							}
							else {
								notifyTerminal(false);
								state = State.STATE_START;
							}

						}
						// help with propagation of the other proposal
						if (acceptRequest != null)
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
							break;
						}
					case SYNC_REQ:
						sendSynAck(message);
						break;
					case SYNC_ACK:
						if(!syncFlag)
							appendSynAck(message);
						break;

					default:
						System.out.println("Undefined Message!");
					}
				}
			}
			break;
		case STATE_CONFIRM:
			if (message != null) {
				System.out.println("STATE: " + state);
				System.out.println("MESSAGE: " + message.translate());
				System.out.println("ACCEPTCOUNT: " + acceptCount);
				if (currentBallot != null)
					System.out.println("CURRENTBALLOT: "
							+ currentBallot.toString());
				else
					System.out.println("CURRENTBALLOT: " + "NULL");
				System.out.println("CURRENTVAL: " + currentOperation);
				System.out.println("CONFIRMLIST" + confirmList.size());

				switch (message.getType()) {
				case DECIDE:
					//if (checkRedundantMessage(message))
						//break;
					DecideMessage decideMessage = (DecideMessage) message;
					currentOperation = decideMessage.getValue();
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
				case SYNC_REQ:
					sendSynAck(message);
					break;
				case SYNC_ACK:
					if(!syncFlag)
						appendSynAck(message);
					break;
				default:
					System.out.println("Undefined Message!");
				}
			}
			break;
		case STATE_PROPOSER_ACCEPT:
			synchronized (this) {
					recvMessageList.add(0, message); // no message consume
			}
			if (message != null) {
				System.out.println("STATE: " + state);
				System.out.println("MESSAGE: " + message.translate());
				System.out.println("ACCEPTCOUNT: " + acceptCount);
				System.out
						.println("CURRENTBALLOT: " + currentBallot.toString());
				System.out.println("CURRENTVAL: " + currentOperation);
				System.out.println("CONFIRMLIST" + confirmList.size());

				switch (message.getType()) {
				case PREPARE:
					PrepareMessage prepareMessage = (PrepareMessage) message;

					if (currentBallot.compareTo(prepareMessage.getBallot()) < 0) {
						notifyTerminal(false);

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
					//if (checkRedundantMessage(message))
						//break;
					DecideMessage decideMessage = (DecideMessage) message;
					currentOperation = decideMessage.getValue();
					if (!compareLists(currentOperation, decideMessage.getValue()))
						notifyTerminal(false);
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
							//if (checkRedundantMessage(message))
								//break;
							DecideMessage newDecideMessage = new DecideMessage(
									MessageType.DECIDE, this.serverNo,
									Messenger.BROADCAST, currentOperation);
							// we dont periodically send decide message
							messenger.sendMessage(newDecideMessage);
							currentOperation = newDecideMessage.getValue();
							makeDecision(currentOperation);
						}
					}
					break;
				case SYNC_REQ:
					sendSynAck(message);
					break;
				case SYNC_ACK:
					if(!syncFlag)
						appendSynAck(message);
					break;
				default:
					System.out.println("Redundant Message!");
				}
			}
			break;
		case STATE_ACCEPTOR_ACCEPT:
			if (message != null) {
				System.out.println("STATE: " + state);
				System.out.println("MESSAGE: " + message.translate());
				System.out.println("ACCEPTCOUNT: " + acceptCount);
				if (currentBallot != null)
					System.out.println("CURRENTBALLOT: "
							+ currentBallot.toString());
				else
					System.out.println("CURRENTBALLOT: " + "NULL");
				System.out.println("CURRENTVAL: " + currentOperation);
				System.out.println("CONFIRMLIST" + confirmList.size());

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
					//if (checkRedundantMessage(message))
						//break;
					DecideMessage decideMessage = (DecideMessage) message;
					currentOperation = decideMessage.getValue();
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
							//if (checkRedundantMessage(message))
								//break;
							DecideMessage newDecideMessage = new DecideMessage(
									MessageType.DECIDE, this.serverNo,
									Messenger.BROADCAST, currentOperation);
							// we dont periodically send decide message
							messenger.sendMessage(newDecideMessage);
							currentOperation = newDecideMessage.getValue();
							makeDecision(currentOperation);
						}
					}
					break;
				case SYNC_REQ:
					sendSynAck(message);
					break;
				case SYNC_ACK:
					if(!syncFlag)
						appendSynAck(message);
					break;
				default:
					System.out.println("Undefined Message!");
				}
			}
			break;
		default:
			System.out.println("STATE: " + state);
			System.out.println("Undefined State!");
		}
		if ((nextOperation = getCommand()) != null) {
			state = State.STATE_PREPARE;
			startProposal(nextOperation);
			terminal.command = null;
			nextOperation = null;
		}
	}

	private void appendSynAck(Message message2) {
		SyncAckMessage synAckMessage = (SyncAckMessage) message;
		for(LogEntry e:synAckMessage.recentLog)
			log.appendLogEntry(e);
		updateBalance(synAckMessage.getRecentLog());
		syncFlag = true;
		
	}
	
	private void generateCombinedValue(List<LogEntry> votedValue) {
		if (balance + checkValue(votedValue) + checkValue(currentOperation) > 0)
			currentOperation.addAll(votedValue);
	}
	private double checkValue(List<LogEntry> list) {
		double total = 0;
		for (LogEntry logEntry : list) {
			if(logEntry.operation.equals("withdraw"))
				total -= logEntry.operand;
			else if(logEntry.operation.equals("deposit")) 
				total += logEntry.operand;
		}
		return total;
	}
	private void updateBalance(List<LogEntry> list){
		balance += checkValue(list);
	}
	

	private void sendSynAck(Message message) throws IOException {
		SyncReqMessage synReqMessage = (SyncReqMessage) message;
		int logLength = synReqMessage.logLength;
		List<LogEntry> recentLogs = new LinkedList<LogEntry>();
		recentLogs.addAll(log.logs.subList(logLength, log.getLogLength()));
		SyncAckMessage synAckMessage = new SyncAckMessage(MessageType.SYNC_ACK,
				serverNo, synReqMessage.sender, recentLogs);
		messenger.sendMessage(synAckMessage);
	}

	private List<LogEntry> selectOperation() {
		Ballot recvMaxBallot = null;
		List<LogEntry> maxBallotValue = null;
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

	private List<LogEntry> getCommand() throws UnknownHostException, IOException {
		synchronized (this) {
			command = terminal.getCommand();
		}
		if (command != null)
			return interpret(command);
		return null;
	}

	private Message getMessage() {
		Message message = null;
		synchronized (this) {
			if (recvMessageList!=null && !recvMessageList.isEmpty())
				
				message = recvMessageList.remove(0);
		}
		return message;
	}

	public void notifyTerminal(boolean success) {
		String indicator = success ? "SUCCEED" : "FAIL";
		if (userTimer != null)
			userTimer.cancel();
		if (waitTimer != null)
			waitTimer.cancel();
		System.out.println("The Last Operation " + currentOperation + indicator);
		currentBallot = null;
		currentOperation = null;
		confirmList.clear();
	}

	public void startProposal(List<LogEntry> nextOperation)
			throws UnknownHostException, IOException {
		if (!syncFlag)
			return;
		state = State.STATE_PREPARE;
		updateBallot();
		currentOperation = nextOperation;
		Message newProposal = new PrepareMessage(MessageType.PREPARE, serverNo,
				Messenger.BROADCAST, currentBallot);
		confirmList.clear();
		userTimer = new ServerTimer(this, TRANSACTION_TIMEOUT, true);
		waitTimer = new ServerTimer(this, ACKWAIT_TIMEOUT, false);
		//confirmList.add(new ConfirmMessage(MessageType.CONFIRM, serverNo,
			//	this.serverNo, this.currentBallot, null, null));
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

	private void makeDecision(List<LogEntry> operation) {
		for (LogEntry entry : operation) {
			log.appendLogEntry(entry);
			
		}	
		updateBalance(operation);
		notifyTerminal(true);
		acceptCount = 0;
		state = State.STATE_START;
	}

	public List<LogEntry> interpret(String s) throws UnknownHostException,
			IOException {
		if (s == null || s.length() == 0)
			return null;
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
					// start proposal with currentBallot and currentOperation
					currentOperation = new LinkedList<LogEntry>();
					currentOperation.add(new LogEntry("deposit", value,
							log.getLogPosition(), serverNo, sequenceNo++));
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
					if (balance - value < 0) {
						notifyTerminal(false);
						break;
					}
					// start proposal with currentBallot and currentOperation
					currentOperation = new LinkedList<LogEntry>();
					currentOperation.add(new LogEntry("withdraw", value,
							log.getLogPosition(), serverNo, sequenceNo++));
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
				state = State.STATE_START;
				recvMessageList.clear();
				startSynchronization();				
				System.out.println("unfail the server");
			} else {
				handleInvalidInput();
			}
			break;
		case 'p':
			if(s.equals("print()"))
				printAllLogs();
			break;
		default:
			handleInvalidInput();
		}
		this.command = null;
		terminal.command = null;
		return null;
	}

	private void printAllLogs() {
		System.out.println("log history:");
		List<LogEntry> history = log.logs;
		for(int i = history.size()-1; i>=0; i--)
			System.out.println(history.get(i).operation +"\t"+ history.get(i).operand);		
	}

	public void handleInvalidInput() {
		// ?? handle invalid input
		System.out.println("invalid input");
	}
}
