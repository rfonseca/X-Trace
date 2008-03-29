/* a simple protocol for including some metadata 
 * with chat messages, such as time stamp
 */
package protocols;

import java.util.Date;

public class ChatMessage{
	String message;
	Date currTime;
	
	public ChatMessage(String msg){
		
		myDate = new Date();
	}

	public String getMessage(String msg){
		return message;
	}
	
	public String getTime(){
		return currTime.getHours() + ":" + currTime.getMinutes();
	}
	
}