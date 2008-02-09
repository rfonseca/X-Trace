package edu.berkeley.xtrace.server;

import java.util.Date;
import java.util.List;

import edu.berkeley.xtrace.TaskID;

public class TaskRecord {
	private TaskID taskId;
	private Date firstSeen;
	private Date lastUpdated;
	private int numReports;
	private String title;
	private List<String> tags;
	
	public TaskRecord(TaskID taskId, Date firstSeen, Date lastUpdated,
			int numReports, String title, List<String> tags) {
		super();
		this.taskId = taskId;
		this.firstSeen = firstSeen;
		this.lastUpdated = lastUpdated;
		this.numReports = numReports;
		this.title = title;
		this.tags = tags;
	}

	public TaskID getTaskId() {
		return taskId;
	}

	public Date getFirstSeen() {
		return firstSeen;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public int getNumReports() {
		return numReports;
	}

	public String getTitle() {
		return title;
	}

	public List<String> getTags() {
		return tags;
	}
}
