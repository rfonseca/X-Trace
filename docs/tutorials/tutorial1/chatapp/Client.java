package chatapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

//import edu.berkeley.xtrace.XTraceContext;
//import edu.berkeley.xtrace.XTraceMetadata;

public class Client{
	public static int PORT=8888;
	public static void main(String argv[]) throws IOException, ClassNotFoundException{

		/* Setting up X-Tracing */
		//XTraceContext.startTrace("ChatClient", "Run Job: Tutorial 1" , "tutorial");
		
		/* Set up the connection to the server */
		Socket s = new Socket("localhost", PORT);
		ObjectInputStream in = new ObjectInputStream(s.getInputStream());
		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
		
		/* Setup up input from the client user */
		BufferedReader stdin = new BufferedReader(
									new InputStreamReader(System.in));
		
		/* Talk to the server */
		ChatMessage msgObjIn = new ChatMessage();
		String input;
		while (true){
			/* Get input from user and send it */
			System.out.print("YOU: ");
			ChatMessage msgObjOut = new ChatMessage(stdin.readLine());
			//XTraceContext.logEvent("ChatClient", "SendUsersMessage", "Message", msgObjOut.message);
			//msgObjOut.xtraceMD = XTraceContext.getThreadContext().pack();
			out.writeObject(msgObjOut);
			
			msgObjOut = null;
			
			/* Collect reply message from server and display it to user */
			try{
				msgObjIn = (ChatMessage) in.readObject();
				//XTraceContext.setThreadContext(XTraceMetadata.createFromBytes(msgObjIn.xtraceMD,0,16));
				//XTraceContext.logEvent("ChatClient", "ReceivedServersMessage");
				input = msgObjIn.message;
				System.out.println("SERVER: " + input);
			} catch (EOFException e){
				System.out.println("Server terminated your connection");
				break;
			}
		}
		
		/* clean up */
		in.close();
		stdin.close();
		s.close();
	} 
	

}
