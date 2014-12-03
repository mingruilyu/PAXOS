package server;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class Server {
	static final int NULL_VALUE = -1;
	Log log;
	int serverNo;
	Ballot currentBallot;
	int currentVal;
	boolean syncFlag;
	Messenger messenger;
	List<Message> messageList = new LinkedList<Message>();
	List<String> commandList = new LinkedList<String>();
	CommandInterpreter commandInterpreter;
	public Server(){
		messenger = Messenger.getMessenger();
		Thread dispatcher = new Dispatcher(messageList);
		dispatcher.run();
		Thread commandInterpreter = new CommandInterpreter();
		commandInterpreter.run();
	}
	
	public void run() {
		Message message;
		String command;
		Message reply;
		synchronized(this) {
			if(!messageList.isEmpty()) 
				message = messageList.remove(0);
		}
		
		synchronized(this) {
			if(!commandList.isEmpty())
				command = commandList.remove(0);
		}
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
								   acceptMessage.getSender(),
								   prepareMessage.getBallot(), 
								   null, 
								   currentVal);
				currentBallot = prepareMessage.getBallot();
			}
			else if (currentBallot.compareTo(acceptMessage.getBallot()) < 0) {
				reply = new ConfirmMessage(MessageType.CONFIRM, 
						   serverNo, 
						   acceptMessage.getSender(),
						   prepareMessage.getBallot(), 
						   currentBallot, 
						   currentVal);
				currentBallot = prepareMessage.getBallot();
			}
			else {
				reply
			}
			messenger.sendMessage(reply);
			break;
		case SYNC_REQ: break;
		case SYNC_ACK: break;
		case CONFIRM: break;
		case DECIDE: break;
		}
		}
	}

	public static void main(String[] args) {
		Server server = new Server();
		while(true) {
			
		}
	}
	
	private void updateBallot() {
		
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
					// ?? call deposit method
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

