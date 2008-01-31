package edu.berkeley.xtrace.server;

import java.util.Date;

import edu.berkeley.xtrace.TaskID;

public class TaskInfo {
	private TaskID taskId;
	private Date date;
	private int reportCount;

	public TaskInfo(TaskID taskId, Date date, int reportCount) {
		super();
		this.taskId = taskId;
		this.date = date;
		this.reportCount = reportCount;
	}

	public TaskID getTaskId() {
		return taskId;
	}

	public void setTaskId(TaskID taskId) {
		this.taskId = taskId;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public int getReportCount() {
		return reportCount;
	}

	public void setReportCount(int reportCount) {
		this.reportCount = reportCount;
	}
}
