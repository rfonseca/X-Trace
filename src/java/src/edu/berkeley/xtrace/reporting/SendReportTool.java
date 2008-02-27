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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import edu.berkeley.xtrace.XTraceMetadata;
import edu.berkeley.xtrace.TaskID;

/**
 * This tool can be used to send reports from the command line directly to
 * the reporting infrastructure.  Its usage is:
 * <p>
 * SendReportTool key1 value1 ?key2 value2? ?key3 value3?
 * <p>
 * or,
 * <p>
 * SendReportTool -file filename
 * <p>
 * where <code>filename</code> contains a valid X-Trace report.
 * 
 * You can also select which instance of the reporter to use
 * by specifying the -Dxtrace.reporter=edu.berkeley.reporting.classname
 * 
 * @author George Porter <gporter@cs.berkeley.edu>
 *
 */
public final class SendReportTool {
	private static final Logger LOG = Logger.getLogger(SendReportTool.class);
	private static Random r;

	public static void main(String[] args) {
		BasicConfigurator.configure();
		r = new Random();
		
		out("X-Trace command line reporter");
		
		if (args.length == 0 || args[0].equalsIgnoreCase("--help")) {
			usage();
			System.exit(1);
			
		} else if (args[0].equalsIgnoreCase("-file")) {
			if (args.length != 2) {
				usage();
				System.exit(1);
			}
			reportFile(args[1]);
			
		} else if (args[0].equalsIgnoreCase("-random")) {
			reportRandom();
			
		} else {
			if (args.length % 2 != 0) {
				usage();
				System.exit(1);
			}
			reportArgs(args);
		}
	}
	
	private static void reportArgs(String[] args) {
		Reporter ctx = Reporter.getReporter();
		Report rpt = new Report();
		
		for (int i = 0; i < args.length; i += 2) {
			rpt.put(args[i], args[i+1]);
		}
		
		LOG.info("Sending the report:\n" + rpt);
		ctx.sendReport(rpt);
		ctx.close();
	}
	
	private static void reportRandom() {
		Reporter ctx = Reporter.getReporter();
		Report rpt = randomReport(new TaskID(8));
		rpt.put("Agent", "RandomReport");
		rpt.put("Label", "report");
		rpt.put("Tag", "rnd");
		rpt.put("Tag", "rpt");
		rpt.put("Title", "RandomReport");
		rpt.put("Host", "localhost.localdomain");
		LOG.info("Sending the report:\n" + rpt);
		ctx.sendReport(rpt);
		ctx.close();
	}

	private static void reportFile(String f) {
		Reporter ctx = Reporter.getReporter();
		FileInputStream fin;
		try {
			fin = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			LOG.warn("Input file not found", e);
			return;
		}
		
		try {
			if (fin.available() <= 0) {
			   LOG.info("File " + f + " is empty, skipping.");
			   fin.close();
			   return;
			}
		} catch (IOException e) {
			LOG.warn("I/O error: ", e);
			return;
		}

		StringBuilder buf = new StringBuilder();
		byte[] contents = new byte[1024];
		int len;
		try {
			while ((len = fin.read(contents)) > 0) {
			   buf.append(new String(contents, 0, len));
			}
		} catch (IOException e) {
			LOG.warn("I/O error", e);
		}
		
		try {
			fin.close();
		} catch (IOException e) {
			LOG.warn("I/O error", e);
		}

		// This isn't very efficient
		String strcontents = buf.toString();
		String[] reports = strcontents.split("\n\n+");
		
		int i;
		for (i = 0; i < reports.length; i++) {
		      Report r = Report.createFromString(reports[i]);
		      ctx.sendReport(r);
		}
		System.out.println("Processed " + i + " reports");

		ctx.close();
	}

	private static void usage() {
		out("CmdLineReporter key1 value1 ?key2 value2? ?key3 value3? ...");
		out("or CmdLineReporter -file <filename>");
		out("or CmdLineReporter -random");
		out("   where <filename> contains valid X-Trace reports, separated by newlines");
	}
	
	private static void out(String s) {
		System.out.println(s);
	}
	
	private static Report randomReport(TaskID task) {
		Report report = new Report();
		
		final int numKeys = r.nextInt(15);
		for (int i = 0; i < numKeys; i++) {
			report.put("Key"+i, randomString(10 + r.nextInt(20)));
		}
		
		report.put("X-Trace", new XTraceMetadata(task, r.nextInt()).toString());
		return report;
	}
	
	private static String randomString(int length) {
		char[] ar = new char[length];
		
		for (int i = 0; i < length; i++) {
			ar[i] = (char)((int)'a' + r.nextInt(25));
		}

		return new String(ar);
	}
}
