package source;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

public class Server {
	public static int PORT = 8888;
	public static void main(String[] args) throws IOException, ClassNotFoundException{
		/* Set up a server */
		ServerSocket ss = new ServerSocket();
		try {
			ss = new ServerSocket(PORT);
		} catch (IOException e) {
			System.err.println("problem listening on port " + PORT);
			System.exit(1);
		}
		
		/* Get a connection from a client */
		System.out.println("Waiting for connection from client");
		Socket cs = null;
		try {
			cs = ss.accept();
		} catch (IOException e) {
			System.err.println("problem accepting client connection");
		}
		System.out.println("Client connection established");
		
		/* Setup the input and output for the client connection */
		ObjectOutputStream out = new ObjectOutputStream(cs.getOutputStream());
		ObjectInputStream in = new ObjectInputStream(cs.getInputStream());

		/* Start talking to the client */
		ChatMessage msgObjIn = new ChatMessage();
		ChatMessage msgObjOut = new ChatMessage();
		while (true) {
			/* get a message from client */
			msgObjIn = (ChatMessage) in.readObject();
			String msg = msgObjIn.getMessage();
			System.out.println("CLIENT: "+ msg);
			String response;
			if (msg.equals("exit") || msg.equals("bye")){
				response = "See you later.";
				break;
			} else {
				if (Math.random() < 0.3)
					response = "Yes, I see.";
				else if (Math.random() < 0.5)
					response = "That is interesting.";
				else response = "Uh huh.";
			}
			msgObjOut.load(response);
			out.writeObject(msgObjOut);
			System.out.println("SERVER: " + response);
		}
		out.close();
		in.close();
		cs.close();
		ss.close();
	}
}