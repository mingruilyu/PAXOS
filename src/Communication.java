package server;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


class Communication {
	Socket socket;
	String lastDes;
	int lastPort;
	OutputStream output;
	InputStream input;
	BufferedReader socketInputReader;
	BufferedReader cmdInputReader;
}
