package server;

import java.io.*;
import java.net.*;
import java.util.*;


class Communication {
	final static int PORT_BASE = 5000;
	static private Communication communicationInstance;
	Map<Integer, Socket> socketMap;
	/*OutputStream outputStream;
	InputStream inputStream;
	OutputStreamWriter outputStreamWriter;
	InputStreamReader inputStreamReader;
	BufferedReader bufferedReader;
	BufferedWriter bufferedWriter;*/	
	private Communication(Map<Integer, String> ipMap) throws UnknownHostException, IOException {
		if (ipMap == null) return;
		socketMap = new HashMap<Integer, Socket>();
		for (Integer serNo : ipMap.keySet()) 
			socketMap.put(serNo, new Socket(ipMap.get(serNo), PORT_BASE + serNo));
		
	}
	
	public void sendMessage(Message message) {
		
	}

	public Message parseMessage(String message) {
		//fdff
	}
}
