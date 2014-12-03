package server;

import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

public class NewMessageTask implements Callable<Void>{
	private Socket connection;
	private List<Message> messageList;
	public NewMessageTask(Socket connection, List<Message> messageList) {
		this.connection = connection;
		this.messageList = messageList;
	}
	public Void call() {
		InputStreamReader reader = new InputStreamReader(connection.getInputStream());
		StringBuilder message = new StringBuilder();
		for (int ch = reader.read(); ch != -1; ch = reader.read())
			message.append((char)ch);
		Message newMessage = Message.parseMessage(message.toString());
		synchronized(this) {
			messageList.add(newMessage);
		}
	}
}
