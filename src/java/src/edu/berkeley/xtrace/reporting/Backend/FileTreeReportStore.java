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


package edu.berkeley.xtrace.reporting.Backend;

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
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.berkeley.xtrace.Metadata;
import edu.berkeley.xtrace.TaskID;
import edu.berkeley.xtrace.XtraceException;

public final class FileTreeReportStore implements QueryableReportStore {
	private static final Logger LOG = Logger.getLogger(FileTreeReportStore.class);
	
	private String dataDirName;
	private File dataRootDir;
	private BlockingQueue<String> incomingReports;
	private LRUFileHandleCache fileCache;
	private Connection conn;
	private PreparedStatement querytestps, insertps, updateps, updatedSincePs;
	private PreparedStatement numByTask, totalNumReports, totalNumTasks;
	
	private static final Pattern XTRACE_LINE = 
		Pattern.compile("^X-Trace:\\s+([0-9A-Fa-f]+)$", Pattern.MULTILINE);
	
	public synchronized void setReportQueue(BlockingQueue<String> q) {
		this.incomingReports = q;
	}
	
	@SuppressWarnings("serial")
	public synchronized void initialize() throws XtraceException {
		// Directory to store reports into
		dataDirName = System.getProperty("xtrace.backend.storedirectory");
		if (dataDirName == null) {
			throw new XtraceException("FileTreeReportStore selected, but no xtrace.backend.storedirectory specified");
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
					"numreports integer default 1 not null)");
			// GEO TODO: create indices
			s.close();
			conn.commit();
		} catch (SQLException e) { LOG.warn("warning: ", e); }
		
		try {
			querytestps = conn.prepareStatement("select count(taskid) as rowcount from tasks where taskid = ?");
			insertps = conn.prepareStatement("insert into tasks (taskid) values (?)");
			updateps = conn.prepareStatement("update tasks set lastUpdated = current_timestamp, " +
					"numreports = numreports + 1 where taskid = ?");
			updatedSincePs = conn.prepareStatement("select taskid from tasks where firstseen >= ?");
			numByTask = conn.prepareStatement("select numreports from tasks where taskid = ?");
			totalNumReports = conn.prepareStatement("select sum(numreports) as totalreports from tasks");
			totalNumTasks = conn.prepareStatement("select count(distinct taskid) as numtasks from tasks");
		} catch (SQLException e) {
			throw new XtraceException("Unable to setup prepared statement", e);
		}
	}

	public void sync() {
		fileCache.flushAll();
	}
	
	public synchronized void shutdown() {
		LOG.info("Shutting down the FileTreeReportStore");
		fileCache.closeAll();
		try {
			DriverManager.getConnection("jdbc:derby:tasks;shutdown=true");
		} catch (SQLException e) {
			if (!e.getSQLState().equals("08006")) {
				LOG.warn("Unable to shutdown embedded database", e);
			}
		}
	}

	void receiveReport(String msg) {
		Matcher matcher = XTRACE_LINE.matcher(msg);
		if (matcher.find()) {
			
			String xtraceLine = matcher.group(1);
			Metadata meta = Metadata.createFromString(xtraceLine);
			
			if (meta.getTaskId() != null) {
				TaskID task = meta.getTaskId();
				String taskstr = task.toString();
				BufferedWriter fout = fileCache.getHandle(taskstr);
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
					// Find out whether to do an insert or an update
					querytestps.setString(1, taskstr.toUpperCase());
					ResultSet rs = querytestps.executeQuery();
					rs.next();
					if (rs.getInt("rowcount") == 0) {
						insertps.setString(1, taskstr.toUpperCase());
						insertps.executeUpdate();
					} else {
						updateps.setString(1, taskstr.toUpperCase());
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
			String msg;
			try {
				msg = incomingReports.take();
			} catch (InterruptedException e1) {
				continue;
			}
			receiveReport(msg);
		}
	}

	public Iterator<String> getReportsByTask(String task) {
		return new FileTreeIterator(taskIdtoFile(task));
	}
	
	public Iterator<String> getTasksSince(Long milliSecondsSince1970) {
		ArrayList<String> lst = new ArrayList<String>();
		
		try {
			updatedSincePs.setString(1, (new Timestamp(milliSecondsSince1970)).toString());
			ResultSet rs = updatedSincePs.executeQuery();
			while (rs.next()) {
				lst.add(rs.getString("taskid"));
			}
		} catch (SQLException e) {
			LOG.warn("Internal SQL error", e);
		}
		
		return lst.iterator();
	}
	
	public int countByTaskId(String taskId) {
		try {
			numByTask.setString(1, taskId.toUpperCase());
			ResultSet rs = numByTask.executeQuery();
			if (rs.next()) {
				return rs.getInt("numreports");
			}
		} catch (SQLException e) {
			LOG.warn("Internal SQL error", e);
		}
		return 0;
	}

	public long lastUpdatedByTaskId(String taskId) {
		// GEOTODO
		return 0;
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

	public Date dataAsOf() {
		return fileCache.lastSynched();
	}
	
	public Iterator<TaskID> getLatestTasks(int num) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private final static class LRUFileHandleCache {
		
		private File dataRootDir;
		private final int CACHE_SIZE;
		
		private Map<String, BufferedWriter> fCache = null;
		private Date lastSynched;

		@SuppressWarnings("serial")
		public LRUFileHandleCache(int size, File dataRootDir) 
		throws XtraceException {
			CACHE_SIZE = size;
			lastSynched = new Date();
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
		
		public synchronized BufferedWriter getHandle(String task) throws IllegalArgumentException {
			LOG.debug("Getting handle for task: " + task);
			if (task.length() < 6) {
				throw new IllegalArgumentException("Invalid task id: " + task);
			}
			
			if (!fCache.containsKey(task)) {
				// Create the appropriate three-level directories (l1, l2, and l3)
				File l1 = new File(dataRootDir, task.substring(0, 2));
				File l2 = new File(l1, task.substring(2, 4));
				File l3 = new File(l2, task.substring(4, 6));
				
				if (!l3.exists()) {
					LOG.debug("Creating directory for task " + task + ": " + l3.toString());
					if (!l3.mkdirs()) {
						LOG.warn("Error creating directory " + l3.toString());
						return null;
					}
				} else {
					LOG.debug("Directory " + l3.toString() + " already exists; not creating");
				}
				
				// create the file
				File taskFile = new File(l3, task + ".txt");
				
				// insert the PrintWriter into the cache
				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter(taskFile, true));
					fCache.put(task, writer);
					LOG.debug("Inserting new BufferedWriter into the file cache for task " + task);
				} catch (IOException e) {
					LOG.warn("Interal I/O error", e);
					return null;
				}
			} else {
				LOG.debug("Task " + task + " was already in the cache, no need to insert");
			}
			
			return fCache.get(task);
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

			lastSynched = new Date();
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
		
		public Date lastSynched() {
			return lastSynched;
		}
	}

	final static class FileTreeIterator implements Iterator<String> {
		
		private BufferedReader in = null;
		private String nextReport = null;

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
		public String next() {
			String ret = nextReport;
			nextReport = calcNext();
			return ret;
		}
		
		private String calcNext() {
			
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
			return reportbuf.toString();
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
