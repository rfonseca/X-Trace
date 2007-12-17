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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.berkeley.xtrace.Metadata;

/**
 * An X-Trace report.
 * 
 * X-Trace reports consist of a header line, and then a set of
 * key-value pairs:
 * 
 * <pre>
 * X-Trace Report ver 1.0
 * Key1: Value1
 * Key2: Value2
 * Key3: Value3
 * </pre>
 * 
 * While this class can be used to construct X-Trace reports, it is
 * recommended that they should be created through
 * the {@link edu.berkeley.xtrace.Event} class or the higher-level 
 * {@link edu.berkeley.xtrace.Context} API.
 *
 * @see edu.berkeley.xtrace.Event
 * @see edu.berkeley.xtrace.Context
 * 
 * @author George Porter
 */
public class Report {
	private static final Logger LOG = Logger.getLogger(Report.class);

	private StringBuilder buf;
	private HashMap<String, List<String>> map; // lazily-built to enhance performance

	/**
	 * Build a new, empty report
	 */
	public Report() {
		buf = new StringBuilder("X-Trace Report ver 1.0\n");
		map = null;
	}

	/**
	 * This private constructor is used for createFromString()
	 * All it does it store the string locally.
	 */
	private Report(String s) {
		buf = new StringBuilder(s);

		// Check to make sure that we're ready to start
		// appending the next key-value pair
		try {
			if (s.charAt(s.length() - 1) != '\n') {
				buf.append("\n");
			}
		} catch (IndexOutOfBoundsException e) {
		}

		map = null;
	}

	/**
	 * Set the given header with a value.  A given key can have
	 * multiple values if this method is called multiple times with the
	 * append parameter set to true.
	 * 
	 * @param key the key to set
	 * @param value the value to associate with the given key
	 * @param append if true, also keep previous values
	 */
	public void put(final String key, final String value, boolean append) {
		if (append && map == null) {
			buf.append(key + ": " + value + "\n");
			return;
		}

		if (map == null) {
			convertToMap();
		}

		List<String> values;

		if (map.containsKey(key) && append) {
			values = map.get(key);
		} else {
			values = new ArrayList<String>();
		}

		values.add(value);
		map.put(key, values);
	}

	/**
	 * This put() method sets the given key and value,
	 * appending if the key already exists
	 * 
	 * @param key
	 * @param value
	 */
	public void put(final String key, final String value) {
		put(key, value, true);
	}

	public void remove(final String key) {
		convertToMap();
		map.remove(key);
	}

	/**
	 * Returns the <code>X-Trace</code> metadata field of this report,
	 * if it exists.  Otherwise, this returns null.
	 * 
	 * @return the metadata associated with this report, or null if there
	 * is no such association.
	 */
	public Metadata getMetadata() {
		convertToMap();

		List<String> xtrlist = map.get("X-Trace");
		if (xtrlist == null || xtrlist.size() < 1)
			return null;

		String xtrstr = xtrlist.iterator().next();
		Metadata xtr = null;
		try {
			xtr = Metadata.createFromString(xtrstr);
		} catch (Exception e) {
			LOG.info("Corrupt metadata: " + xtrstr);
			return null;
		}

		return xtr;
	}

	public List<String> get(String key) {
		convertToMap();
		return map.get(key);
	}

	/**
	 * Converts this report into a String, according to the X-Trace
	 * report specification.  The resulting string is suitable for sending
	 * to a report daemon or other X-Trace element expecting a properly
	 * formatted string
	 *
	 * @return the String representing this report
	 */
	@Override
	public String toString() {
		if (map == null) {
			return buf.toString();
		}

		StringBuilder buf = new StringBuilder("X-Trace Report ver 1.0\n");

		final Iterator<Map.Entry<String, List<String>>> iter = map.entrySet()
				.iterator();
		while (iter.hasNext()) {
			final Map.Entry<String, List<String>> entry = iter.next();
			final Iterator<String> values = entry.getValue().iterator();

			while (values.hasNext()) {
				final String v = values.next();
				buf.append(entry.getKey() + ": " + v + "\n");
			}
		}

		return buf.toString();
	}

	/**
	 * Creates a new <code>Report</code> from a String
	 *
	 * @param s the String to build this report from
	 * @throws IOException if the String is not a properly formatted report
	 */
	public static Report createFromString(final String s) {
		return new Report(s);
	}

	private void convertToMap() {
		if (map != null) {
			return;
		}

		map = new HashMap<String, List<String>>();

		BufferedReader in = new BufferedReader(new StringReader(buf.toString()));

		try {
			if (!in.readLine().equals("X-Trace Report ver 1.0")) {
				LOG.warn("Corrupt report can't be converted to a map");
				buf = new StringBuilder("X-Trace Report ver 1.0\n");
				map = null;
			}
		} catch (IOException e) {
			LOG.warn("Internal I/O Error", e);
			buf = new StringBuilder("X-Trace Report ver 1.0\n");
			map = null;
		}

		String line = null;
		try {
			while ((line = in.readLine()) != null) {
				int idx = line.indexOf(":");
				String key = line.substring(0, idx).trim();
				String value = line.substring(idx + 1, line.length()).trim();
				put(key, value);
			}
		} catch (IOException e) {
			LOG.warn("Internal I/O Error", e);
			buf = new StringBuilder("X-Trace Report ver 1.0\n");
			map = null;
		}
	}
}
