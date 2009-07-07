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

import java.util.concurrent.BlockingQueue;

import edu.berkeley.xtrace.XTraceException;

public interface ReportStore extends Runnable {
	
	/**
	 * Sets a BlockingQueue that is used to pass reports from the rest of the server
	 * into this ReportStore.
	 * 
	 * @param q the queue to receive reports from
	 * @see BlockingQueue
	 */
	public void setReportQueue(BlockingQueue<String> q);
	
	/**
	 * Initializes this ReportStore.  This must be called before
	 * starting and using the store.
	 * 
	 * @throws XTraceException if initialization fails
	 */
	public void initialize() throws XTraceException;
	
	/**
	 * Commits any buffered reports to stable storage.
	 */
	public void sync();
	
	/**
	 * Closes any resources obtained by this ReportStore.  Once shutdown
	 * is called, reports will no longer be stored or retrieved from
	 * the incoming BlockingQueue
	 */
	public void shutdown();
}
