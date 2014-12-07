package server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

public class NewMessageTask implements Callable<Void>{
	private Socket connection;
	private List<Message> messageList;
	private Boolean lock;
	public NewMessageTask(Socket connection, List<Message> messageList, Boolean lock) {
		this.connection = connection;
		this.messageList = messageList;
		this.lock = lock;
	}
	public Void call() throws IOException {
		InputStreamReader reader = new InputStreamReader(connection.getInputStream());
		StringBuilder message = new StringBuilder();
		for (int ch = reader.read(); ch != '$'; ch = reader.read()) {
			message.append((char)ch);
		}
		Message newMessage = Message.parseMessage(message.toString());
		synchronized(this) {
			messageList.add(newMessage);
		}
		synchronized(lock) {
			lock.notify();
		}
		return null;
	}
}
