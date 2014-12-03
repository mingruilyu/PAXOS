package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


class Communication {
	final static int PORT = 5000;
	final static int THREAD_POOL_SIZE = 10;
	static private Communication messengerInstance;
	Map<Integer, String> addrMap;
	ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
	ServerSocket serverSocket;
	/*OutputStream outputStream;
	InputStream inputStream;
	OutputStreamWriter outputStreamWriter;
	InputStreamReader inputStreamReader;
	BufferedReader bufferedReader;
	BufferedWriter bufferedWriter;*/
	private Communication() {
		try {
			serverSocket = new ServerSocket(PORT);
		}
		catch (IOException ex) {
			System.out.println("could not start server!");
		}
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
	
	public void receiveMessage() {
		while (true) {
			try {
				Socket connection = serverSocket.accept();
				Callable<Void> task = new DaytimeTask(connection);
				pool.submit(task);
			} 
			catch (IOException ex) {}
		}
	}
	
	public Message parseMessage(String message) {
		//fdff
	}
}
