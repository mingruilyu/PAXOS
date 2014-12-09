package server;

import java.io.*;
import java.net.*;
import java.util.*;


public class Messenger {
	//final static int PORT = 5000;
	final static int BROADCAST = -1;
	static private Messenger messengerInstance;
	Map<Integer, String> addrMap;
	Map<Integer, Integer> portMap;

	public int getPort(Integer receiver) {
		return portMap.get(receiver);
	}
	private Messenger() throws IOException {
		addrMap = new HashMap<Integer, String>();
		portMap = new HashMap<Integer, Integer>();
		readAddress();
	}
	public void setAddress(int serverNo, String ip) throws UnknownHostException, IOException {
		addrMap.put(serverNo, ip);
	}
	
	private void setPort(int serverNo, int port) {
		portMap.put(serverNo, port);
	}
	public void readAddress() throws IOException {
		String path = "ip.txt";
		BufferedReader in = new BufferedReader(new FileReader(path));
		String text;
		while ((text = in.readLine()) != null) {
			String[] serverAddr = text.split("\t");
			setAddress(Integer.parseInt(serverAddr[0]), serverAddr[1]);
			setPort(Integer.parseInt(serverAddr[0]), Integer.parseInt(serverAddr[2]));
		} 
		in.close();
	}
	
	public Socket getSocket(int receiver) throws UnknownHostException, IOException {
		return new Socket(addrMap.get(receiver), portMap.get(receiver));
	} 
	
	public static Messenger getMessenger() throws IOException {
		if (messengerInstance == null) 
			return messengerInstance = new Messenger();
		return messengerInstance;
	}
	
	public void sendMessage(Message message) throws UnknownHostException, IOException{
		if (message.getReceiver() == BROADCAST) {
			for (Integer receiver : addrMap.keySet()) {
				if (receiver.equals(message.getSender()))
					continue;
				message.setReceiver(receiver);
					Socket socket = getSocket(receiver);
					OutputStream out = socket.getOutputStream();
					OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
					BufferedWriter bufferedWriter = new BufferedWriter(writer);
					bufferedWriter.write(message.translate());
					bufferedWriter.flush();
					//System.out.println("MESSAGE: " + message.translate());
			}
		}
		else {
		try {
			Socket socket = getSocket(message.getReceiver());
			OutputStream out = socket.getOutputStream();
			OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
			BufferedWriter bufferedWriter = new BufferedWriter(writer);
			bufferedWriter.write(message.translate());
			bufferedWriter.flush();
			//System.out.println("MESSAGE: " + message.translate());
		}
		catch(IOException ex) {
			System.out.println("Sending Message Error!");
		}
		}
	}	
}
