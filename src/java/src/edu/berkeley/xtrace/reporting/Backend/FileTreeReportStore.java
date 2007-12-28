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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.berkeley.xtrace.Metadata;
import edu.berkeley.xtrace.TaskID;
import edu.berkeley.xtrace.XtraceException;

public class FileTreeReportStore implements ReportStore {
	private static final Logger LOG = Logger.getLogger(FileTreeReportStore.class);
	
	private String dataDirName;
	private BlockingQueue<String> incomingReports;
	private LRUFileHandleCache fileCache;
	private Map<TaskID, TaskStatistics> recentTasks;

	private Timer saverTimer;
	
	private static final Pattern XTRACE_LINE = 
		Pattern.compile("^X-Trace:\\s+([0-9A-Fa-f]+)$", Pattern.MULTILINE);

	public void setReportQueue(BlockingQueue<String> q) {
		this.incomingReports = q;
	}
	
	@SuppressWarnings("serial")
	public void initialize() throws XtraceException {
		// Directory to store reports into
		dataDirName = System.getProperty("xtrace.backend.storedirectory");
		if (dataDirName == null) {
			throw new XtraceException("FileTreeReportStore selected, but no xtrace.backend.storedirectory specified");
		}
		
		// 25-element LRU file handle cache
		fileCache = new LRUFileHandleCache(25, dataDirName);
		
		// In-memory data structure of recently added tasks (1000 elements long)
		// We try to read one off the disk, but if that doesn't exist, we
		// create a new one.
		if (!readInRecentTasks()) {
			recentTasks = new LinkedHashMap<TaskID, TaskStatistics>(1000, .75F, true) {
				protected boolean removeEldestEntry(java.util.Map.Entry<TaskID, TaskStatistics> eldest) {
					return size() > 1000;
				}
			};
		}
		
		// Autosave every 10 seconds
		AutoSaver saver = new AutoSaver();
		saverTimer = new Timer("autosaver");
		saverTimer.schedule(saver, 10000, 10000);
	}
	
	public void shutdown() {
		fileCache.closeAll();
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
			
			Matcher matcher = XTRACE_LINE.matcher(msg);
			if (matcher.find()) {
				
				String xtraceLine = matcher.group(1);
				Metadata meta = Metadata.createFromString(xtraceLine);
				if (meta.getTaskId() != null) {
					TaskID task = meta.getTaskId();
					BufferedWriter fout = fileCache.getHandle(task.toString());
					if (fout == null) {
						continue;
					}
					try {
						fout.write(msg);
						fout.newLine();
						fout.newLine();
					} catch (IOException e) {
						LOG.warn("I/O error while writing the report", e);
					}
					
					// Update index
					if (!recentTasks.containsKey(task)) {
						recentTasks.put(task, new TaskStatistics());
					}
					TaskStatistics stats = recentTasks.get(task);
					stats.lastUpdated = System.currentTimeMillis();
					stats.numReports += 1;
					recentTasks.put(task, stats);
				}
			}
		}
	}

	public Iterator<String> getByTask(String task) throws XtraceException {
		return new FileTreeIterator(taskIdtoFile(task));
	}
	
	public Iterator<String> getTasksSince(Long startTime)
			throws XtraceException {
		
		ArrayList<String> tasklst = new ArrayList<String>();
		
		Iterator<Map.Entry<TaskID, TaskStatistics>> iter = recentTasks.entrySet().iterator();
		
		while (iter.hasNext()) {
			Map.Entry<TaskID, TaskStatistics> entry = iter.next();
			TaskID task = entry.getKey();
			TaskStatistics stats = entry.getValue();
			
			if (stats.lastUpdated >= startTime) {
				tasklst.add(task.toString());
			}
		}
		return tasklst.iterator();
	}
	
	public int countByTaskId(String taskId) {
		File taskFile = taskIdtoFile(taskId);
		
		int count = 0;
		
		Iterator<String> iter = new FileTreeIterator(taskFile);
		while (iter.hasNext()) {
			iter.next();
			count++;
		}
		
		return count;
	}

	public long lastUpdatedByTaskId(String taskId) {
		if (recentTasks.containsKey(taskId)) {
			TaskStatistics stats = recentTasks.get(taskId);
			return stats.lastUpdated;
		} else {
			File taskFile = taskIdtoFile(taskId);
			if (!taskFile.exists() || !taskFile.canRead())
				return 0;
			return taskFile.lastModified();
		}
	}

	public int numReports() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int numUniqueTasks() {
		// TODO Auto-generated method stub
		return 0;
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
	
	@SuppressWarnings("unchecked")
	private boolean readInRecentTasks() {
		recentTasks = new LinkedHashMap<TaskID, TaskStatistics>(1000, .75F, true) {
			protected boolean removeEldestEntry(java.util.Map.Entry<TaskID, TaskStatistics> eldest) {
				return size() > 1000;
			}
		};
		
		File recentTaskFile = new File(dataDirName, "recentTasks.dat");
		if (recentTaskFile.exists() && recentTaskFile.canRead()) {
			try {
				ObjectInputStream in =
					new ObjectInputStream(new FileInputStream(recentTaskFile));
				
				int numEntries = in.readInt();
				
				for (int i = 0; i < numEntries; i++) {
					int taskLength = in.readInt();
					byte[] taskBytes = new byte[taskLength];
					in.readFully(taskBytes);
					TaskID task = TaskID.createFromBytes(taskBytes, 0, taskBytes.length);
					
					TaskStatistics stats = new TaskStatistics();
					stats.lastUpdated = in.readLong();
					stats.numReports = in.readLong();
					
					recentTasks.put(task, stats);
				}
				
				return true;
			} catch (FileNotFoundException e) {
				LOG.warn("Unable to open recent tasks cache", e);
			} catch (IOException e) {
				LOG.warn("Error reading from recent tasks cache", e);
			}
		}
		return false;
	}
	
	private synchronized void flushRecentTasks() {
		File recentTaskFile = new File(dataDirName, "recentTasks.dat");
		try {
			ObjectOutputStream out =
				new ObjectOutputStream(new FileOutputStream(recentTaskFile));
			Iterator<Map.Entry<TaskID, TaskStatistics>> iter = recentTasks.entrySet().iterator();
			
			out.writeInt(recentTasks.size());
			
			while (iter.hasNext()) {
				Map.Entry<TaskID, TaskStatistics> entry = iter.next();
				TaskID task = entry.getKey();
				TaskStatistics stats = entry.getValue();
				
				out.writeInt(task.get().length);
				out.write(task.get());
				out.writeLong(stats.lastUpdated);
				out.writeLong(stats.numReports);
			}
			
			out.flush();
			out.close();
			
		} catch (FileNotFoundException e) {
			LOG.warn("Cannot open recent tasks cache file: cache is not persistent", e);
			return;
		} catch (IOException e) {
			LOG.warn("Error while opening " +
					"or writing recent tasks cache file: cache is not persistent", e);
			return;
		}
	}
	
	@SuppressWarnings("serial")
	private final static class TaskStatistics implements Serializable {
		public long lastUpdated = Long.MIN_VALUE;
		public long numReports = 0;
	}
	
	final class AutoSaver extends TimerTask {
		@Override
		public void run() {
			LOG.debug("Autosaving FileTree data");
			fileCache.flushAll();
			flushRecentTasks();
		}
	}
	
	private final static class LRUFileHandleCache {
		
		private File dataRootDir;
		private final int CACHE_SIZE;
		
		private Map<String, BufferedWriter> fCache = null;
		private Date lastSynched;

		@SuppressWarnings("serial")
		public LRUFileHandleCache(int size, String dataStoreDirectory) 
		throws XtraceException {
			dataRootDir = new File(dataStoreDirectory);
			CACHE_SIZE = size;
			lastSynched = new Date();
			
			if (!dataRootDir.isDirectory()) {
				throw new XtraceException("Data Store location isn't a directory: " + dataStoreDirectory);
			}
			if (!dataRootDir.canWrite()) {
				throw new XtraceException("Can't write to data store directory");
			}
			
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
				}
				
				// create the file
				File taskFile = new File(l3, task + ".txt");
				
				// insert the PrintWriter into the cache
				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter(taskFile, true));
					fCache.put(task, writer);
				} catch (IOException e) {
					LOG.warn("Interal I/O error", e);
					return null;
				}
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
			
			Iterator<String> iter = fCache.keySet().iterator();
			while (iter.hasNext()) {
				String taskid = iter.next();
				LOG.debug("Closing handle for file of " + taskid);
				BufferedWriter writer = fCache.get(taskid);
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						LOG.warn("I/O error closing file for task " + taskid, e);
					}
				}
					
				fCache.remove(taskid);
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
