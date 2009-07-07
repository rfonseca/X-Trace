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

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import edu.berkeley.xtrace.XTraceException;

/**
 * @author George Porter
 *
 */
public class NullReportStore implements ReportStore {
	private static final Logger LOG = Logger.getLogger(NullReportStore.class);
	private BlockingQueue<String> q;

	public Iterator<String> getByTask(String task) throws XTraceException {
		return null;
	}

	public Iterator<String> getTasksSince(Long startTime)
			throws XTraceException {
		return null;
	}

	public void initialize() throws XTraceException {
		LOG.info("NullReportStore initialized");
	}

	public void setReportQueue(BlockingQueue<String> q) {
		this.q = q;
	}

	public void shutdown() {
	}

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
