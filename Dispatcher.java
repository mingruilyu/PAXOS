package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Dispatcher extends Thread{
	final static int THREAD_POOL_SIZE = 10;
	ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
	List<Message> messageList;
	ServerSocket serverSocket;
	Boolean lock;
	public Dispatcher(List<Message> messageList, int port, Boolean lock) {
		this.messageList = messageList;
		this.lock = lock;
		try {
			serverSocket = new ServerSocket(port);
		}
		catch (IOException ex) {
			System.out.println("could not start server!");
		}
	}
	public void run() {
		while (true) {
			try {
				Socket connection = serverSocket.accept();
				Callable<Void> task = new NewMessageTask(connection, messageList, lock);
				threadPool.submit(task);
			} 
			catch (IOException ex) {}
		}
	}
}
