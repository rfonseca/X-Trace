package source;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import edu.berkeley.xtrace.XTraceContext;

public class Client{
	public static int PORT=8888;
	public static void main(String argv[]) throws IOException, ClassNotFoundException{

		/* Setting up X-Tracing */
		XTraceContext.startTrace("Client", "Run Job: Tutorial 1" , "tutorial");
		
		/* Set up the connection to the server */
		Socket s = new Socket("localhost", PORT);
		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
		ObjectInputStream in = new ObjectInputStream(s.getInputStream());
		
		/* Setup up input from the client user */
		BufferedReader stdin = new BufferedReader(
									new InputStreamReader(System.in));
		
		/* Talk to the server */
		ChatMessage msgObjIn = new ChatMessage();
		ChatMessage msgObjOut = new ChatMessage();
		String input;
		while (true){
			/* Get input from user and send it */
			msgObjOut.load(stdin.readLine());
			out.writeObject(msgObjOut);
			
			/* Collect reply message from server and display it to user */
			msgObjIn = (ChatMessage) in.readObject();
			input = msgObjIn.getMessage();
			System.out.println(input);
			if (input.equals("exit") || input.equals("bye")){
				break;
			}
		}
		
		/* clean up */
		out.close();
		in.close();
		stdin.close();
		s.close();
	} 
	

}
