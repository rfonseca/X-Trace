package edu.berkeley.xtrace.reporting.Backend;

import java.util.Date;
import java.util.Iterator;

import edu.berkeley.xtrace.TaskID;
import edu.berkeley.xtrace.XtraceException;

public interface QueryableReportStore extends ReportStore {
	
	public Iterator<String> getReportsByTask(String task) throws XtraceException;
	
	public Iterator<String> getTasksSince(Long startTime);
	
	public Iterator<TaskID> getLatestTasks(int num);

	public long lastUpdatedByTaskId(String taskId);

	public int countByTaskId(String taskId);

	public int numUniqueTasks();

	public int numReports();
	
	public Date dataAsOf();
}
