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

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import edu.berkeley.xtrace.XtraceException;

/**
 * @author George Porter
 *
 */
public class NullReportStore implements ReportStore {
	private static final Logger LOG = Logger.getLogger(NullReportStore.class);
	private BlockingQueue<String> q;

	/* (non-Javadoc)
	 * @see edu.berkeley.xtrace.reporting.Backend.ReportStore#getByTask(java.lang.String)
	 */
	public Iterator<String> getByTask(String task) throws XtraceException {
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.berkeley.xtrace.reporting.Backend.ReportStore#getTasksSince(java.lang.Long)
	 */
	public Iterator<String> getTasksSince(Long startTime)
			throws XtraceException {
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.berkeley.xtrace.reporting.Backend.ReportStore#initialize()
	 */
	public void initialize() throws XtraceException {
		LOG.info("NullReportStore initialized");
	}

	/* (non-Javadoc)
	 * @see edu.berkeley.xtrace.reporting.Backend.ReportStore#setReportQueue(java.util.concurrent.BlockingQueue)
	 */
	public void setReportQueue(BlockingQueue<String> q) {
		this.q = q;
	}

	/* (non-Javadoc)
	 * @see edu.berkeley.xtrace.reporting.Backend.ReportStore#shutdown()
	 */
	public void shutdown() {
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		LOG.info("NullReportSource waiting for reports");
		
		while (true) {
			String message = null;
			try {
				message = q.take();
			} catch (InterruptedException e) {
				LOG.warn("Internal error", e);
			}
			LOG.debug("ReportStore: " + message);
		}
	}

	public void sync() {
		// TODO Auto-generated method stub
		
	}
}
