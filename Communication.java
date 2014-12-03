package server;

import java.io.*;
import java.net.*;
import java.util.*;


class Communication {
	final static int PORT_BASE = 5000;
	static private Communication messengerInstance;
	Map<Integer, Socket> socketMap;
	/*OutputStream outputStream;
	InputStream inputStream;
	OutputStreamWriter outputStreamWriter;
	InputStreamReader inputStreamReader;
	BufferedReader bufferedReader;
	BufferedWriter bufferedWriter;*/
	private Communication() {
		socketMap = new HashMap<Integer, Socket>();
	}
	
	public void setAddress(Map<Integer, String> ipMap) throws UnknownHostException, IOException {
		if (ipMap == null) return;
		for (Integer serNo : ipMap.keySet()) 
			socketMap.put(serNo, new Socket(ipMap.get(serNo), PORT_BASE + serNo));
	}
	
	public static Communication getMessenger() {
		if (messengerInstance == null) 
			return messengerInstance = new Communication();
		return messengerInstance;
	}
	
	public void sendMessage(Message message) {
		int a;
	}

	public Message parseMessage(String message) {
		//fdff
	}
}
