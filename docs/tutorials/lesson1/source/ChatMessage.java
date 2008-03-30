/* a simple protocol for including some metadata 
 * with chat messages, such as time stamp
 */
package source;

import java.io.Serializable; 
import java.util.Date;

public class ChatMessage implements Serializable{
	String message;
	Date currDate;
	byte[] xtraceMD;
	
	public ChatMessage(){}
	
	public ChatMessage(String msg){
		message = msg;
		currDate = new Date();
	}
	
	public String getTime(){
		return currDate.getHours() + ":" + currDate.getMinutes() + ":" + currDate.getSeconds();
	}
	
}