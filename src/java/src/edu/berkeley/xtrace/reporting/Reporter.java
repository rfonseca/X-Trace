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

//import org.apache.log4j.Logger;

/**
 * X-trace reporting context framework.  This context is the abstract
 * base class for different report contexts
 * 
 * By default, this uses the "classic" front-end daemon report context.
 * To change this behavior, specify a different class using the
 * 'xtrace.reportctx' system property.  You can do this on the
 * command line via:
 *     
 *    java -Dxtrace.reportctx=edu.berkeley.reporting.classname
 *
 * @author George Porter
 */
public abstract class Reporter
{
	//private static final Logger LOG = Logger.getLogger(Reporter.class);

	static Reporter reporter;

	/**
	 * Retrieve a handle to the reporter.  Once a reporter
	 * is created, this operation is very cheap.
	 * 
	 * @return a handle to a reporter (creating one if necessary)
	 */
	public final static synchronized Reporter getReporter() {
		if (reporter == null) {
			String systemprop = System.getProperty("xtrace.reporter");

			if (systemprop == null) {
				systemprop = "edu.berkeley.xtrace.reporting.UdpReporter";
			}

			try {
				reporter = (Reporter) (Class.forName(systemprop)).newInstance();
				
			} catch (InstantiationException e) {
				//LOG.warn("Unable to instantiate reporting class: " + systemprop, e);
				System.exit(1);
				
			} catch (IllegalAccessException e) {
				//LOG.warn("Unable to access reporting class: " + systemprop, e);
				System.exit(1);
				
			} catch (ClassNotFoundException e) {
				//LOG.warn("Unable to find reporting class: " + systemprop, e);
				System.exit(1);
			}

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					if (reporter != null) {
					   reporter.close();
					}
				}
			});
		}
		return reporter;
	}

	/**
	 * Sends a report to the reporting domain
	 * 
	 * @param r
	 *            the report to send
	 */
	public abstract void sendReport(Report r);

	/**
	 * Closes this reporter, releasing any resources
	 */
	public abstract void close();

	/**
	 * Send any pending reports, blocking if necessary.
	 * Default implementation does nothing.
	 */
	public void flush() {}
}
