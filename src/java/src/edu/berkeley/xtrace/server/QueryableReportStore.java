package edu.berkeley.xtrace.server;

import java.util.Iterator;
import java.util.List;

import edu.berkeley.xtrace.TaskID;
import edu.berkeley.xtrace.XTraceException;
import edu.berkeley.xtrace.reporting.Report;

// TODO: add limit and offset for pagination

public interface QueryableReportStore extends ReportStore {
	
	public Iterator<Report> getReportsByTask(TaskID task) throws XTraceException;
	
	public Iterator<TaskID> getTasksSince(long startTime);
	
	public List<TaskID> getLatestTasks(int num);
	
	public List<TaskID> getTasksByTag(String tag);

	public long lastUpdatedByTaskId(TaskID taskId);

	public int countByTaskId(TaskID taskId);

	public int numUniqueTasks();

	public int numReports();
	
	public long dataAsOf();
}
