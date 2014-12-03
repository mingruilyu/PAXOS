package server;

import java.io.*;
import java.net.*;
import java.util.*;


class Communication {
	final static int PORT = 5000;
	static private Communication messengerInstance;
	Map<Integer, String> addrMap;
	/*OutputStream outputStream;
	InputStream inputStream;
	OutputStreamWriter outputStreamWriter;
	InputStreamReader inputStreamReader;
	BufferedReader bufferedReader;
	BufferedWriter bufferedWriter;*/
	private Communication() {
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
	
	public static Communication getMessenger() {
		if (messengerInstance == null) 
			return messengerInstance = new Communication();
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
	
	public void 
	
	public Message parseMessage(String message) {
		//fdff
	}
}
