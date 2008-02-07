/*
 * Copyright (c) 2005,2006,2007 The Regents of the University of California.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the University of California, nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY OF CALIFORNIA ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package edu.berkeley.xtrace.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.berkeley.xtrace.Metadata;
import edu.berkeley.xtrace.TaskID;
import edu.berkeley.xtrace.XtraceException;
import edu.berkeley.xtrace.reporting.Report;

public final class FileTreeReportStore implements QueryableReportStore {
	private static final Logger LOG = Logger.getLogger(FileTreeReportStore.class);
	// TODO: a title field that replaces any previous title in that task
	// TODO: support user-specified jdbc connect strings
	// TODO: when the FileTreeReportStore loads up, fill in the derby database with
	//       metdata about the already stored reports
	
	private String dataDirName;
	private File dataRootDir;
	private BlockingQueue<String> incomingReports;
	private LRUFileHandleCache fileCache;
	private Connection conn;
	private PreparedStatement querytestps, insertps, updateps, updatedSincePs, numByTask;
	private PreparedStatement totalNumReports, totalNumTasks, lastUpdatedByTask, lastNtasks;
	private PreparedStatement gettagsps;
	private boolean shouldOperate = false;
	private boolean databaseInitialized = false;

	private static final Pattern XTRACE_LINE = 
		Pattern.compile("^X-Trace:\\s+([0-9A-Fa-f]+)$", Pattern.MULTILINE);
	
	public synchronized void setReportQueue(BlockingQueue<String> q) {
		this.incomingReports = q;
	}
	
	@SuppressWarnings("serial")
	public synchronized void initialize() throws XtraceException {
		// Directory to store reports into
		dataDirName = System.getProperty("xtrace.server.storedirectory");
		if (dataDirName == null) {
			throw new XtraceException("FileTreeReportStore selected, but no xtrace.server.storedirectory specified");
		}
		dataRootDir = new File(dataDirName);
		
		if (!dataRootDir.isDirectory()) {
			throw new XtraceException("Data Store location isn't a directory: " + dataDirName);
		}
		if (!dataRootDir.canWrite()) {
			throw new XtraceException("Can't write to data store directory");
		}
		
		// 25-element LRU file handle cache.  The report data is stored here
		fileCache = new LRUFileHandleCache(25, dataRootDir);
		
		// the embedded database keeps metadata about the reports
		initializeDatabase();
		
		shouldOperate = true;
	}
	
	private void initializeDatabase() throws XtraceException {
		// This embedded SQL database contains metadata about the reports
		System.setProperty("derby.system.home", dataDirName);
		try {
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
		} catch (InstantiationException e) {
			throw new XtraceException("Unable to instantiate internal database", e);
		} catch (IllegalAccessException e) {
			throw new XtraceException("Unable to access internal database class", e);
		} catch (ClassNotFoundException e) {
			throw new XtraceException("Unable to locate internal database class", e);
		}
		try {
			conn = DriverManager.getConnection("jdbc:derby:tasks;create=true");
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			throw new XtraceException("Unable to connect to interal database: " + e.getSQLState(), e);
		}
		LOG.info("Successfully connected to the internal Derby database");
		
		// Create the table(s) if necessary.
		try {
			Statement s = conn.createStatement();
			s.executeUpdate("create table tasks(taskid varchar(40) not null primary key, " +
					"firstSeen timestamp default current_timestamp not null, " +
					"lastUpdated timestamp default current_timestamp not null, " +
					"numreports integer default 1 not null, " +
					"tags varchar(512), " +
					"title varchar(128))");
			s.executeUpdate("create index idx_tasks on tasks(taskid)");
			s.executeUpdate("create index idx_firstseen on tasks(firstSeen)");
			s.executeUpdate("create index idx_lastUpdated on tasks(lastUpdated)");
			s.executeUpdate("create index idx_tags on tasks(tags)");
			s.executeUpdate("create index idx_title on tasks(title)");
			s.close();
			conn.commit();
		} catch (SQLException e) { }
		
		try {
			querytestps = conn.prepareStatement("select count(taskid) as rowcount from tasks where taskid = ?");
			insertps = conn.prepareStatement("insert into tasks (taskid, tags) values (?, ?)");
			updateps = conn.prepareStatement("update tasks set lastUpdated = current_timestamp, " +
					"numreports = numreports + 1, " +
					"tags = tags || ? where taskid = ?");
			updatedSincePs = conn.prepareStatement("select taskid from tasks where firstseen >= ?");
			numByTask = conn.prepareStatement("select numreports from tasks where taskid = ?");
			totalNumReports = conn.prepareStatement("select sum(numreports) as totalreports from tasks");
			totalNumTasks = conn.prepareStatement("select count(distinct taskid) as numtasks from tasks");
			lastUpdatedByTask = conn.prepareStatement("select lastUpdated from tasks where taskid = ?");
			lastNtasks = conn.prepareStatement("select taskid from tasks order by lastUpdated desc");
			gettagsps = conn.prepareStatement("select taskid from tasks where tags like '%'||?||'%'");
		} catch (SQLException e) {
			throw new XtraceException("Unable to setup prepared statement", e);
		}
		databaseInitialized = true;
	}

	public void sync() {
		fileCache.flushAll();
	}
	
	public synchronized void shutdown() {
		LOG.info("Shutting down the FileTreeReportStore");
		if (fileCache != null)
		    fileCache.closeAll();
		
		if (databaseInitialized) {
			try {
				DriverManager.getConnection("jdbc:derby:tasks;shutdown=true");
			} catch (SQLException e) {
				if (!e.getSQLState().equals("08006")) {
					LOG.warn("Unable to shutdown embedded database", e);
				}
			}
			databaseInitialized = false;
		}
	}

	void receiveReport(String msg) {
		Matcher matcher = XTRACE_LINE.matcher(msg);
		if (matcher.find()) {
			Report r = Report.createFromString(msg);
			String xtraceLine = matcher.group(1);
			Metadata meta = Metadata.createFromString(xtraceLine);
			
			if (meta.getTaskId() != null) {
				TaskID task = meta.getTaskId();
				String taskstr = task.toString().toUpperCase();
				BufferedWriter fout = fileCache.getHandle(task);
				if (fout == null) {
					LOG.warn("Discarding a report due to internal fileCache error: " + msg);
					return;
				}
				try {
					fout.write(msg);
					fout.newLine();
					fout.newLine();
					LOG.debug("Wrote " + msg.length() + " bytes to the stream");
				} catch (IOException e) {
					LOG.warn("I/O error while writing the report", e);
				}
				
				// Update index
				try {
					
					// Extract the tag, if present
					String tag = "";
					List<String> tags = r.get("Tag");
					for (int i = 0; tags != null && i < tags.size(); i++) {
						tag = tag + tags.get(i);
						if (i < tags.size() - 1) {
							tag = tag + ",";
						}
					}
					
					
					// Find out whether to do an insert or an update
					querytestps.setString(1, taskstr);
					ResultSet rs = querytestps.executeQuery();
					rs.next();
					if (rs.getInt("rowcount") == 0) {
						insertps.setString(1, taskstr);
						insertps.setString(2, tag);
						insertps.executeUpdate();
					} else {
						// TODO: If the tag already exists, we shouldn't append the new tag
						updateps.setString(1, tag);
						updateps.setString(2, taskstr);
						updateps.executeUpdate();
					}
					conn.commit();
					
				} catch (SQLException e) {
					LOG.warn("Unable to update metadata about task " + task.toString(), e);
				}
			} else {
				LOG.debug("Ignoring a report without an X-Trace taskID: " + msg);
			}
		}
	}
	
	public void run() {
		LOG.info("FileTreeReportStore running with datadir " + dataDirName);
		
		while (true) {
			if (shouldOperate) {
				String msg;
				try {
					msg = incomingReports.take();
				} catch (InterruptedException e1) {
					continue;
				}
				receiveReport(msg);
			}
		}
	}

	public Iterator<Report> getReportsByTask(TaskID task) {
		return new FileTreeIterator(taskIdtoFile(task.toString()));
	}
	
	public Iterator<TaskID> getTasksSince(long milliSecondsSince1970) {
		ArrayList<TaskID> lst = new ArrayList<TaskID>();
		
		try {
			updatedSincePs.setString(1, (new Timestamp(milliSecondsSince1970)).toString());
			ResultSet rs = updatedSincePs.executeQuery();
			while (rs.next()) {
				lst.add(TaskID.createFromString(rs.getString("taskid")));
			}
		} catch (SQLException e) {
			LOG.warn("Internal SQL error", e);
		}
		
		return lst.iterator();
	}

	public List<TaskID> getTasksByTag(String tag) {
		List<TaskID> lst = new ArrayList<TaskID>();
		
		try {
			gettagsps.setString(1, tag);
			ResultSet rs = gettagsps.executeQuery();
			while (rs.next()) {
				lst.add(TaskID.createFromString(rs.getString("taskid")));
			}
		} catch (SQLException e) {
			LOG.warn("Internal SQL error", e);
		}
		
		return lst;
	}
	
	public int countByTaskId(TaskID taskId) {
		try {
			numByTask.setString(1, taskId.toString().toUpperCase());
			ResultSet rs = numByTask.executeQuery();
			if (rs.next()) {
				return rs.getInt("numreports");
			}
		} catch (SQLException e) {
			LOG.warn("Internal SQL error", e);
		}
		return 0;
	}

	public long lastUpdatedByTaskId(TaskID taskId) {
		long ret = 0L;
		
		try {
			lastUpdatedByTask.setString(1, taskId.toString());
			ResultSet rs = lastUpdatedByTask.executeQuery();
			if (rs.next()) {
				Timestamp ts = rs.getTimestamp("lastUpdated");
				ret = ts.getTime();
			}
		} catch (SQLException e) {
			LOG.warn("Internal SQL error", e);
		}
		
		return ret;
	}

	public int numReports() {
		int total = 0;
		
		try {
			ResultSet rs = totalNumReports.executeQuery();
			rs.next();
			total = rs.getInt("totalreports");
		} catch (SQLException e) {
			LOG.warn("Internal SQL error", e);
		}
		
		return total;
	}

	public int numUniqueTasks() {
		int total = 0;
		
		try {
			ResultSet rs = totalNumTasks.executeQuery();
			rs.next();
			total = rs.getInt("numtasks");
		} catch (SQLException e) {
			LOG.warn("Internal SQL error", e);
		}
		
		return total;
	}
	
	private File taskIdtoFile(String taskId) {
		File l1 = new File(dataDirName, taskId.substring(0, 2));
		File l2 = new File(l1, taskId.substring(2, 4));
		File l3 = new File(l2, taskId.substring(4, 6));
		File taskFile = new File(l3, taskId + ".txt");
		return taskFile;
	}

	public long dataAsOf() {
		return fileCache.lastSynched();
	}
	
	public List<TaskID> getLatestTasks(int num) {
		List<TaskID> lst = new ArrayList<TaskID>();
		
		try {
			ResultSet rs = lastNtasks.executeQuery();
			while (rs.next() && num > 0) {
				String taskid = rs.getString("taskid");
				lst.add(TaskID.createFromString(taskid));
				num -= 1;
			}
		} catch (SQLException e) {
			LOG.warn("Internal SQL error", e);
		}
		return lst;
	}
	
	private final static class LRUFileHandleCache {
		
		private File dataRootDir;
		private final int CACHE_SIZE;
		
		private Map<String, BufferedWriter> fCache = null;
		private long lastSynched;

		@SuppressWarnings("serial")
		public LRUFileHandleCache(int size, File dataRootDir) 
		throws XtraceException {
			CACHE_SIZE = size;
			lastSynched = System.currentTimeMillis();
			this.dataRootDir = dataRootDir;
			
			// a 25-entry, LRU file handle cache
			fCache = new LinkedHashMap<String, BufferedWriter>(CACHE_SIZE, .75F, true) {
				protected boolean removeEldestEntry(java.util.Map.Entry<String, BufferedWriter> eldest) {
					if (size() > CACHE_SIZE) {
						BufferedWriter evicted = eldest.getValue();
						try {
							evicted.flush();
							evicted.close();
						} catch (IOException e) {
							LOG.warn("Error evicting file for task: " + eldest.getKey(), e);
						}
					}
					
					return size() > CACHE_SIZE;
				}
			};
		}
		
		public synchronized BufferedWriter getHandle(TaskID task) throws IllegalArgumentException {
			String taskstr = task.toString();
			LOG.debug("Getting handle for task: " + taskstr);
			if (taskstr.length() < 6) {
				throw new IllegalArgumentException("Invalid task id: " + taskstr);
			}
			
			if (!fCache.containsKey(taskstr)) {
				// Create the appropriate three-level directories (l1, l2, and l3)
				File l1 = new File(dataRootDir, taskstr.substring(0, 2));
				File l2 = new File(l1, taskstr.substring(2, 4));
				File l3 = new File(l2, taskstr.substring(4, 6));
				
				if (!l3.exists()) {
					LOG.debug("Creating directory for task " + taskstr + ": " + l3.toString());
					if (!l3.mkdirs()) {
						LOG.warn("Error creating directory " + l3.toString());
						return null;
					}
				} else {
					LOG.debug("Directory " + l3.toString() + " already exists; not creating");
				}
				
				// create the file
				File taskFile = new File(l3, taskstr + ".txt");
				
				// insert the PrintWriter into the cache
				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter(taskFile, true));
					fCache.put(taskstr, writer);
					LOG.debug("Inserting new BufferedWriter into the file cache for task " + taskstr);
				} catch (IOException e) {
					LOG.warn("Interal I/O error", e);
					return null;
				}
			} else {
				LOG.debug("Task " + taskstr + " was already in the cache, no need to insert");
			}
			
			return fCache.get(taskstr);
		}
		
		public synchronized void flushAll() {
			Iterator<BufferedWriter> iter = fCache.values().iterator();
			while (iter.hasNext()) {
				BufferedWriter writer = iter.next();
				try {
					writer.flush();
				} catch (IOException e) {
					LOG.warn("I/O error while flushing file", e);
				}
			}

			lastSynched = System.currentTimeMillis();
		}
		
		public synchronized void closeAll() {
			flushAll();
			
			/* We can't use an iterator since we would be modifying it
			 * when we call fCache.remove()
			 */
			String[] taskIds = fCache.keySet().toArray(new String[0]);
			
			for (int i = 0; i < taskIds.length; i++) {
				LOG.debug("Closing handle for file of " + taskIds[i]);
				BufferedWriter writer = fCache.get(taskIds[i]);
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						LOG.warn("I/O error closing file for task " + taskIds[i], e);
					}
				}
					
				fCache.remove(taskIds[i]);
			}
		}
		
		public long lastSynched() {
			return lastSynched;
		}
	}

	final static class FileTreeIterator implements Iterator<Report> {
		
		private BufferedReader in = null;
		private Report nextReport = null;

		FileTreeIterator(File taskfile) {
			
			if (taskfile.exists() && taskfile.canRead()) {
				try {
					in = new BufferedReader(new FileReader(taskfile), 4096);
				} catch (FileNotFoundException e) { }
			}
			
			nextReport = calcNext();
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return nextReport != null;
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public Report next() {
			Report ret = nextReport;
			nextReport = calcNext();
			return ret;
		}
		
		private Report calcNext() {
			
			if (in == null) {
				return null;
			}
			
			// Remember where we are if there isn't a complete report in the stream
			try {
				in.mark(4096);
			} catch (IOException e) {
				LOG.warn("I/O error", e);
				return null;
			}
			
			// Find the line starting with "X-Trace Report ver"
			String line = null;
			do {
				try {
					line = in.readLine();
				} catch (IOException e) {
					LOG.warn("I/O error", e);
				}
			} while (line != null && !line.startsWith("X-Trace Report ver"));
			
			if (line == null) {
				// There wasn't a complete 
				try {
					in.reset();
				} catch (IOException e) {
					LOG.warn("I/O error", e);
				}
				return null;
			}
			
			StringBuilder reportbuf = new StringBuilder();
			do {
				reportbuf.append(line + "\n");
				try {
					line = in.readLine();
				} catch (IOException e) {
					LOG.warn("I/O error", e);
					return null;
				}
			} while (line != null && !line.equals(""));
			
			// Find the end of the report (an empty line)
			return Report.createFromString(reportbuf.toString());
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}