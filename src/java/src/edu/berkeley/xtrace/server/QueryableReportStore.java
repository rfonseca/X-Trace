package edu.berkeley.xtrace.server;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import edu.berkeley.xtrace.TaskID;
import edu.berkeley.xtrace.XTraceException;
import edu.berkeley.xtrace.reporting.Report;

// TODO: add limit and offset for pagination

public interface QueryableReportStore extends ReportStore {
	public Iterator<Report> getReportsByTask(TaskID task) throws XTraceException;
	
	public List<TaskRecord> getTasksSince(long startTime);
	
	public List<TaskRecord> getLatestTasks(int num);
	
	public List<TaskRecord> getTasksByTag(String tag);
	
	public List<TaskRecord> getTasksByTitle(String title);

	public List<TaskRecord> getTasksByTitleSubstring(String title);
	
	public int numTasks();
	
	public int numReports();
	
	public long dataAsOf();
}
