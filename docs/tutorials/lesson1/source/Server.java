import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Server {
	public static int PORT = 8888;
	public static void main(String[] args) throws IOException{
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
		PrintWriter out = new PrintWriter(cs.getOutputStream(), true);
		BufferedReader in = new BufferedReader(
								new InputStreamReader(
										cs.getInputStream()));
		String inLine;
		
		/* Start talking to the client */
		while ((inLine = in.readLine()) != null) {
			System.out.println("CLIENT: "+ inLine);
			String response;
			if (inLine.equals("exit") || inLine.equals("bye")){
				response = "See you later.";
				break;
			} else {
				if (Math.random() < 0.3)
					response = "Yes, I see.";
				else if (Math.random() < 0.5)
					response = "That is interesting.";
				else response = "Uh huh.";
			}
			out.println(response);
			System.out.println("SERVER: " + response);
		}
		out.close();
		in.close();
		cs.close();
		ss.close();
	}
}