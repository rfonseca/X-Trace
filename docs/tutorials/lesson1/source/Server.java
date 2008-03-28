import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;


public class Server {
	public static void main(String[] args) throws IOException{
			ServerThread t = new ServerThread();
			t.start();
	}
}

class ServerThread {
	  public static int PORT=8888;
	  
	  public void start() throws IOException{
	    System.out.println("In thread start()");
		Socket socket = new Socket("localhost", PORT);
	    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    System.out.println("Data grabbed from port # " + PORT + ": "
	      + in.readLine());
	    socket.close();
	  }
	}
