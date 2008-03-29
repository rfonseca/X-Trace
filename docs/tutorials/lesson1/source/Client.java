import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import protocols.ChatProtocol;

import edu.berkeley.xtrace.XTraceContext;
import edu.berkeley.xtrace.XTraceMetadata;
import edu.berkeley.xtrace.reporting.*;

public class Client{
	public static int PORT=8888;
	public static void main(String argv[]) throws IOException{

		/* Setting up X-Tracing */
		XTraceContext.startTrace("Client", "Run Job: Tutorial 1" , "tutorial");
		
		/* Set up the connection to the server */
		Socket socket = new Socket("localhost", PORT);
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		BufferedReader in = new BufferedReader(
								new InputStreamReader(socket.getInputStream()));
		
		/* Setup up input from the client user */
		BufferedReader stdin = new BufferedReader(
									new InputStreamReader(System.in));
		
		/* Talk to the server */
		String inLine;
		String input;
		while (true){
			input = stdin.readLine();
			out.println(ChatProtocol.pack(input));
			
			inLine = in.readLine();
			System.out.println(ChatProtocol.unpack(inLine));
			if (input.equals("exit") || input.equals("bye")){
				break;
			}
		}
		
		/* clean up */
		out.close();
		in.close();
		stdin.close();
		socket.close();
	} 
	

}
