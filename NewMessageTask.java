package server;

import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

public class NewMessageTask implements Callable<Void>{
	private Socket connection;
	public NewMessageTask(Socket connection) {
		this.connection = connection;
	}
	public Void call() {
		InputStreamReader reader = new InputStreamReader(connection.getInputStream());
		StringBuilder message = new StringBuilder();
		for (int ch = reader.read(); ch != -1; ch = reader.read())
			message.append((char)ch);
		Message newMessage = Message.parseMessage(message.toString());
		synchronized(this) {
			List<Message> messageList = Server.getServer().getMessageList();
			messageList.add(newMessage);
		}
	}
}
