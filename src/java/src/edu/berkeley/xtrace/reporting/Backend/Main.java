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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import edu.berkeley.xtrace.XtraceException;

/**
 * @author George Porter
 *
 */
public final class Main {
	private static final Logger LOG = Logger.getLogger(Main.class);

	private static ReportSource[] sources;
	
	private static BlockingQueue<String> incomingReportQueue, reportsToStorageQueue;

	private static ThreadPerTaskExecutor sourcesExecutor;

	private static ExecutorService storeExecutor;
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		
		setupReportSources();
		setupReportStore();
		setupBackplane();
	}

	private static void setupBackplane() {
		new Thread(new Runnable() {
			public void run() {
				LOG.info("Backplane waiting for packets");
				
				while (true) {
					String msg = null;
					try {
						msg = incomingReportQueue.take();
					} catch (InterruptedException e) {
						LOG.warn("Interrupted", e);
						continue;
					}
					reportsToStorageQueue.offer(msg);
				}
			}
		}).start();
	}

	private static void setupReportSources() {
		
		incomingReportQueue = new ArrayBlockingQueue<String>(1024, true);
		sourcesExecutor = new ThreadPerTaskExecutor();
		
		// Default input sources
		String sourcesStr = "edu.berkeley.xtrace.reporting.Backend.UdpReportSource," +
		                    "edu.berkeley.xtrace.reporting.Backend.TcpReportSource";
		
		if (System.getProperty("xtrace.backend.sources") != null) {
			sourcesStr = System.getProperty("xtrace.backend.sources");
		} else {
			LOG.warn("No backend report sources specified... using defaults (Udp,Tcp)");
		}
		String[] sourcesLst = sourcesStr.split(",");
		
		sources = new ReportSource[sourcesLst.length];
		for (int i = 0; i < sourcesLst.length; i++) {
			try {
				sources[i] = (ReportSource) Class.forName(sourcesLst[i]).newInstance();
			} catch (InstantiationException e1) {
				LOG.fatal("Could not instantiate report source", e1);
				System.exit(-1);
			} catch (IllegalAccessException e1) {
				LOG.fatal("Could not access report source", e1);
				System.exit(-1);
			} catch (ClassNotFoundException e1) {
				LOG.fatal("Could not find report source class", e1);
				System.exit(-1);
			}
			sources[i].setReportQueue(incomingReportQueue);
			try {
				sources[i].initialize();
			} catch (XtraceException e) {
				LOG.warn("Unable to initialize report source", e);
				// TODO: gracefully shutdown any previously started threads?
				System.exit(-1);
			}
			sourcesExecutor.execute((Runnable) sources[i]);
		}
	}
	
	private static void setupReportStore() {
		reportsToStorageQueue = new ArrayBlockingQueue<String>(1024);
		
		String storeStr = "edu.berkeley.xtrace.reporting.Backend.NullReportStore";
		if (System.getProperty("xtrace.backend.store") != null) {
			storeStr = System.getProperty("xtrace.backend.store");
		} else {
			LOG.warn("No backend report store specified... using default (NullReportStore)");
		}
		
		ReportStore store = null;
		try {
			store = (ReportStore) Class.forName(storeStr).newInstance();
		} catch (InstantiationException e1) {
			LOG.fatal("Could not instantiate report store", e1);
			System.exit(-1);
		} catch (IllegalAccessException e1) {
			LOG.fatal("Could not access report store class", e1);
			System.exit(-1);
		} catch (ClassNotFoundException e1) {
			LOG.fatal("Could not find report store class", e1);
			System.exit(-1);
		}
		
		store.setReportQueue(reportsToStorageQueue);
		try {
			store.initialize();
		} catch (XtraceException e) {
			LOG.fatal("Unable to start report store", e);
			System.exit(-1);
		}
		
		storeExecutor = Executors.newSingleThreadExecutor();
		storeExecutor.execute(store);
	}
	
	private static class ThreadPerTaskExecutor implements Executor {
	     public void execute(Runnable r) {
	         new Thread(r).start();
	     }
	 }
}
