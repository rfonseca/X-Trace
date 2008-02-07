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

import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import edu.berkeley.xtrace.XTraceMetadata;
import edu.berkeley.xtrace.TaskID;
import edu.berkeley.xtrace.XTraceException;
import edu.berkeley.xtrace.reporting.Report;

/**
 * @author George Porter
 *
 */
public final class DummyReportSource implements ReportSource {
	private static final Logger LOG = Logger.getLogger(DummyReportSource.class);

	private int reportsPerSecond;
	private int reportsPerTask;
	private AtomicReference<TaskID[]> taskList;
	private BlockingQueue<String> q;

	private Timer updaterTask;

	private ReportGenerator generator;

	public DummyReportSource() {
		this.taskList = new AtomicReference<TaskID[]>();
		this.taskList.set(new TaskID[0]);
		this.updaterTask = new Timer("TaskUpdater");
	}

	public void initialize() throws XTraceException {
		TaskListUpdater updater = new TaskListUpdater();
		updaterTask.scheduleAtFixedRate(updater, 0, 1000);
		
		String pps = System.getProperty("xtrace.dummyreportsource.packetspersecond");
		if (pps == null) {
			this.reportsPerSecond = 1;
		} else {
			this.reportsPerSecond = Integer.parseInt(pps);
		}
		
		String rpt = System.getProperty("xtrace.dummyreportsource.reportspertask");
		if (rpt == null) {
			this.reportsPerTask = 1;
		} else {
			this.reportsPerTask = Integer.parseInt(rpt);
		}
		
		this.generator = new ReportGenerator();
		this.generator.start();
	}

	public void setReportQueue(BlockingQueue<String> q) {
		this.q = q;
	}

	public void shutdown() {
		updaterTask.cancel();
	}
	
	private final class TaskListUpdater extends TimerTask {
		@Override
		public void run() {
			LOG.debug("Updating tasklist");
			TaskID[] newList = new TaskID[Math.max(1, reportsPerSecond / reportsPerTask)];
			for (int i = 0; i < newList.length; i++) {
				newList[i] = new TaskID(8);
			}
			taskList.set(newList);
		}
	}
	
	private final class ReportGenerator extends Thread {
		private Random rnd;

		public ReportGenerator() {
			rnd = new Random();
		}
		
		public void run() {
			LOG.info("ReportGenerator starting");
			
			long interReqDelay = 1000000000L / reportsPerSecond;
			
			while (true) {
				try {
					Thread.sleep(interReqDelay / 1000000, (int) (interReqDelay % 1000));
				} catch (InterruptedException e) {
					LOG.warn("ReportGenerator interrupted");
				}
				sendReport();
			}
		}
		
		private void sendReport() {
			TaskID id = taskList.get()[rnd.nextInt(taskList.get().length)];
			
			Report r = new Report();
			r.put("X-Trace", new XTraceMetadata(id, 0).toString());
			r.put("Time", (new Date().toString()));
			r.put("Key1", "Value1");
			r.put("Key2", "Value2");
			r.put("Key3", "Value3.1");
			r.put("Key3", "Value3.2");
			
			LOG.debug("Sending report");
			q.offer(r.toString());
		}
	}

	public void run() {
		// TODO Auto-generated method stub
		
	}
}
