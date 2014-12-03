package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class Messenger {
	final static int PORT = 5000;
	
	static private Messenger messengerInstance;
	Map<Integer, String> addrMap;


	/*OutputStream outputStream;
	InputStream inputStream;
	OutputStreamWriter outputStreamWriter;
	InputStreamReader inputStreamReader;
	BufferedReader bufferedReader;
	BufferedWriter bufferedWriter;*/
	private Messenger() {
		addrMap = new HashMap<Integer, String>();
	}
	
	public void setAddress(Map<Integer, String> ipMap) throws UnknownHostException, IOException {
		if (ipMap == null) return;
		for (Integer serNo : ipMap.keySet()) 
			addrMap.put(serNo, ipMap.get(serNo));
	}
	
	public Socket getSocket(int receiver) throws UnknownHostException, IOException {
		return new Socket(addrMap.get(receiver), PORT);
	} 
	
	public static Messenger getMessenger() {
		if (messengerInstance == null) 
			return messengerInstance = new Messenger();
		return messengerInstance;
	}
	
	public void sendMessage(Message message){
		try {
			Socket socket = getSocket(message.getReceiver());
			OutputStream out = socket.getOutputStream();
			OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
			BufferedWriter bufferedWriter = new BufferedWriter(writer);
			bufferedWriter.write(message.translate());
			bufferedWriter.flush();
		}
		catch(IOException ex) {
			System.out.println("Sending Message Error!");
		}
	}
	
	
	public Message parseMessage(String messageString) {
		String[] headerParts = messageString.split(
				String.valueOf(Message.DELIMIT), 4);
		int sender = Integer.parseInt(headerParts[0]);
		int receiver = Integer.parseInt(headerParts[1]);		
		String[] bodyParts = headerParts[3].split(
				String.valueOf(Message.DELIMIT));
		
		switch (headerParts[2]) {
		case "PREPARE":
			int ballotNumber = Integer.parseInt(bodyParts[0]);
			int serverNumber = Integer.parseInt(bodyParts[1]);
			Ballot ballot = new Ballot(ballotNumber, serverNumber);
			return new PrepareMessage(MessageType.PREPARE,sender,receiver,ballot);
			
		case "CONFIRM":
			int acceptBallotNumber = Integer.parseInt(bodyParts[0]);
			int acceptServerNumber = Integer.parseInt(bodyParts[1]);
			Ballot acceptBallot = new Ballot(acceptBallotNumber, acceptServerNumber);
			int recvBallotNumber = Integer.parseInt(bodyParts[2]);
			int recvServerNumber = Integer.parseInt(bodyParts[3]);
			Ballot recvBallot = new Ballot(recvBallotNumber, recvServerNumber);
			int acceptValue=Integer.parseInt(bodyParts[4]);
			return new ConfirmMessage(MessageType.CONFIRM,sender,receiver,acceptBallot,recvBallot,acceptValue);
		
		case "ACCEPT":			
			Ballot acceptedBallot = new Ballot(Integer.parseInt(bodyParts[0]),Integer.parseInt(bodyParts[1]));
			int logPosition= Integer.parseInt(bodyParts[2]);			
			return new AcceptMessage(MessageType.ACCEPT,sender,receiver,acceptedBallot,logPosition);
			
		case "DECIDE":
			int decidedLogPosition= Integer.parseInt(bodyParts[0]);			
			return new DecideMessage(MessageType.DECIDE,sender,receiver,decidedLogPosition);			
			break;
		case "SYNC_REQ":			
			int logLength= Integer.parseInt(bodyParts[0]);			
			return new DecideMessage(MessageType.SYNC_REQ,sender,receiver,logLength);
		case "SYNC_ACK":
			List<LogEntry> recentLog;				
			for (int i = 0; i<bodyParts.length; i=i+2) {
				LogEntry log = new LogEntry(Operation.getEnumFromString(bodyParts[i]),Double.parseDouble(bodyParts[i+1]));
				recentLog.add(log);				
			}
			return new SyncAckMessage(MessageType.SYNC_ACK,sender,receiver,recentLog);
		}
	}
}
