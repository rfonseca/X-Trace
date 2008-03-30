package source;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

//import edu.berkeley.xtrace.XTraceContext;
//import edu.berkeley.xtrace.XTraceMetadata;

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
		while (true) {
			/* get a message from client and show it on stdout */
			msgObjIn = (ChatMessage) in.readObject();
			String msg = msgObjIn.message;
			//XTraceContext.setThreadContext(XTraceMetadata.createFromBytes(msgObjIn.xtraceMD,0,16));
			//XTraceContext.logEvent("ChatServer", "ReceivedClientsMessage");
			System.out.println("CLIENT ("+ msgObjIn.getTime() + "): " + msg);
			
			/* either exit or create a generic response */
			String response;
			if (msg.equals("exit") || msg.equals("bye")){
				System.out.println("Received command to terminate connection");
				break;
			} else {
				if (Math.random() < 0.3)
					response = "Yes, I see.";
				else if (Math.random() < 0.5)
					response = "That is interesting.";
				else response = "Uh huh.";
			}
			
			/* send response to the client */
			ChatMessage msgObjOut = new ChatMessage(response);
			//XTraceContext.logEvent("ChatServer", "SendingMessage", "Message", msgObjOut.message);
			//msgObjOut.xtraceMD = XTraceContext.getThreadContext().pack();
			out.writeObject(msgObjOut);
			System.out.println("SERVER ("+ msgObjOut.getTime() + "): " + response + "\n");
			msgObjOut = null;			
		}
		out.close();
		in.close();
		cs.close();
		ss.close();
	}
}