/* a simple protocol for including some metadata 
 * with chat messages, such as time stamp
 */
package source;

import java.io.Serializable; 
import java.util.Date;

public class ChatMessage implements Serializable{
	String message;
	Date currDate;
	
	public ChatMessage(){}
	public ChatMessage(String msg){
		load(msg);
	}
	
	public void load(String msg){
		message = msg;
		currDate = new Date();
	}

	public String getMessage(){
		return message;
	}
	
	public String getTime(){
		return currDate.getHours() + ":" + currDate.getMinutes();
	}
	
}