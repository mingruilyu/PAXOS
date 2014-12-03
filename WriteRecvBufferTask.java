package server;

import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.*;

public class WriteRecvBufferTask implements Callable<Void>{
	private Socket connection;
	public WriteRecvBufferTask(Socket connection) {
		this.connection = connection;
	}
	public Void call() {
		InputStreamReader reader = new InputStreamReader(connection.getInputStream());
		StringBuilder message = new StringBuilder();
		for (int ch = reader.read(); ch != -1; ch = reader.read())
			message.append((char)ch);
		message
		synchronized(this) {
			List<Message>Server.getServer().getMessageList();
		}
	}
}
