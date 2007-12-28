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

package edu.berkeley.xtrace.reporting;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import edu.berkeley.xtrace.Metadata;
import edu.berkeley.xtrace.TaskID;

public final class ReportLoadTest {
	private static final Logger LOG = Logger.getLogger(ReportLoadTest.class);
	
	private static int testDuration;
	private static int numThreads;
	
	private static CyclicBarrier barrier;

	private static Worker[] workers;

	public static void main(String[] args) {
		BasicConfigurator.configure();
		
		out("X-Trace report infrastructure load test");
		
		if (args.length == 0 || args[0].equalsIgnoreCase("--help")) {
			usage();
			System.exit(1);
			
		} else if (args.length == 2) {
			testDuration = Integer.parseInt(args[0]);
			numThreads = Integer.parseInt(args[1]);
		} else {
			usage();
			System.exit(1);
		}
		
		performTest();
	}
	
	private static void performTest() {
		barrier = new CyclicBarrier(numThreads);
		workers = new Worker[numThreads];
		
		for (int i = 0; i < numThreads; i++) {
			workers[i] = new Worker();
			workers[i].start();
		}
		
		try {
			Thread.sleep(testDuration * 1000);
		} catch (InterruptedException e) {
			LOG.warn("Interrupted", e);
		}
		
		for (int i = 0; i < workers.length; i++) {
			workers[i].shutdown();
		}
		for (int i = 0; i < workers.length; i++) {
			try {
				workers[i].join();
			} catch (InterruptedException e) {
				LOG.warn("Interrupted", e);
			}
		}
		for (int i = 0; i < workers.length; i++) {
			out("Worker " + i + ": " + workers[i].numSent());
		}
	}
	
	private static void usage() {
		out("ReportLoadTest --help");
		out("ReportLoadTest <testDuration> <numThreads>");
	}
	
	private static void out(String s) {
		System.out.println(s);
	}
	
	static class Worker extends Thread {
		
		private Random r;
		private TaskID task;
		private boolean shouldStop;
		private int numSent;

		Worker() {
			r = new Random();
			task = new TaskID(4);
			shouldStop = false;
		}
		
		public void shutdown() {
			shouldStop = true;
		}
		
		public int numSent() {
			return numSent;
		}
		
		public void run() {
			ReportingContext c = ReportingContext.getReportCtx();
			
			try {
				barrier.await();
			} catch (InterruptedException e) {
				LOG.fatal("Interrupted", e);
			} catch (BrokenBarrierException e) {
				LOG.fatal("Broken", e);
			}
			
			numSent = 0;
			while (!shouldStop) {
				Report r = randomReport(task);
				c.sendReport(r);
			}
		}
		
		private Report randomReport(TaskID task) {
			Report report = new Report();
			
			final int numKeys = r.nextInt(15);
			for (int i = 0; i < numKeys; i++) {
				report.put("Key"+i, randomString(10 + r.nextInt(20)));
			}
			report.put("Sequence", ""+numSent++);
			report.put("X-Trace", new Metadata(task, r.nextInt()).toString());
			return report;
		}
		
		private String randomString(int length) {
			char[] ar = new char[length];
			
			for (int i = 0; i < length; i++) {
				ar[i] = (char)((int)'a' + r.nextInt(25));
			}

			return new String(ar);
		}
	}
}
