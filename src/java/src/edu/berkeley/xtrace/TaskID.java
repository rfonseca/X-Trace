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

package edu.berkeley.xtrace;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

/**
 * X-trace task ID.
 * 
 * @author George Porter
 */
public final class TaskID implements Serializable {
	private static volatile Random r = null;
	
	private byte[] id;
	
	/**
	 * Constructs a new "Invalid" task ID
	 * 
	 */
	public TaskID() {
		try {
			initialize(4);
		} catch (IllegalArgumentException e) { }
		
		Arrays.fill(id, (byte)0);
	}
	
	/**
	 * Constructs a new task ID of length <code>length</code> with random
	 * contents
	 * 
	 * @param length
	 *            length of the task id (in bytes)
	 * @throws IllegalArgumentException
	 *             if <code>length</code> is not one of the lengths supported
	 *             by the X-Trace specification.
	 */
	public TaskID(final int length) throws IllegalArgumentException {
		initialize(length);
		
		if (r == null)
			r = new Random();

		r.nextBytes(id);
	}
	
	/**
	 * Constructs a new task ID of length <code>length</code> with the
	 * given bytes as a prefix and random bytes thereafter.
	 * 
	 * @param length
	 *             length of the task id (in bytes)
	 * @throws IllegalArgumentException
	 *             if <code>length</code> is not one of the lengths supported
	 *             by the X-Trace specification, or if prefix is too long.
	 */
	public TaskID(final byte[] prefix, final int length) 
			throws IllegalArgumentException {
		
		if (prefix.length > length) {
			throw new IllegalArgumentException("Prefix longer than length: "
					+ length);
		}
		initialize(length);
		
		if (r == null)
			r = new Random();

		byte[] randomBytes = new byte[length - prefix.length];
		r.nextBytes(randomBytes);
		
		System.arraycopy(prefix, 0, id, 0, prefix.length);
		System.arraycopy(randomBytes, 0, id, prefix.length, length - prefix.length);
	}

	/**
	 * Copy constructor
	 * 
	 * @param taskid
	 */
	public TaskID(TaskID taskid) {
		this.id = new byte[taskid.id.length];
		System.arraycopy(taskid.id, 0, this.id, 0, taskid.id.length);
	}

	/**
	 * Constructs a new task ID, based on the given byte array.  If
	 * the provided byte array does not contain a valid
	 * TaskID, then the "Invalid" taskId is returned
	 * 
	 * @param bytes
	 *            the byte array holding the contents of the new task ID
	 * @param offset
	 *            the offset into <code>bytes</code> holding the desired task
	 *            ID contents
	 * @param length
	 *            the length of bytes to use to create this task ID (in bytes)
	 * @throws IndexOutOfBoundsException
	 *             if <code>offset</code> or <code>length</code> point
	 *             outside the given array
	 */
	public static TaskID createFromBytes(final byte[] bytes, final int offset, final int length)
			throws IndexOutOfBoundsException {
		
		if (bytes == null)
			return new TaskID(); // the Invalid Task

		if (offset < 0 || offset >= bytes.length)
			throw new IndexOutOfBoundsException("offset is negative or "
					+ "points past the given byte array: " + offset);

		if (length < 0 || offset + length > bytes.length)
			throw new IndexOutOfBoundsException("length is negative or "
					+ "(offset + length) points past given array: " + length);
		
		if (length == 4 || length == 8 || length == 12 || length == 20) {
			TaskID id = new TaskID();
			id.id = new byte[length];
			System.arraycopy(bytes, offset, id.id, 0, length);
			return id;
			
		} else {
			System.err.println("Invalid length for a task: " + length);
			return new TaskID();
		}
	}

	/**
	 * Creates a new TaskID from the given String
	 * 
	 * @param s the String holding the taskID
	 * @return a new TaskID
	 */
	public static TaskID createFromString(String s) {
		byte[] bytes = null;
		
		try {
			bytes = IoUtil.stringToBytes(s);
		} catch (IOException e) {
			System.err.println("Invalid taskid: " + s);
			return new TaskID();
		}
		
		return createFromBytes(bytes, 0, bytes.length);
	}
	
	/**
	 * Initializes this task ID to the given length, which must
	 * be a supported X-Trace task id length or an IllegalArgumentException
	 * is thrown
	 * 
	 * @param length
	 * 			   length of the task id (in bytes)
	 * @throws IllegalArgumentException if the length is invalid
	 */
	private void initialize(final int length) throws IllegalArgumentException {
		switch (length) {
		case 4:
			id = new byte[4];
			break;
		case 8:
			id = new byte[8];
			break;
		case 12:
			id = new byte[12];
			break;
		case 20:
			id = new byte[20];
			break;
		default:
			throw new IllegalArgumentException("Invalid length: " + length);
		}
	}

	/**
	 * Returns the value of this task id as a byte array
	 * 
	 * @return the task ID as a byte array
	 */
	public byte[] get() {
		return id;
	}

	/**
	 * Returns the value of this task id as a byte array
	 * 
	 * @return the task ID as a byte array
	 */
	public byte[] pack() {
		return id;
	}

	@Override
	public String toString() {
		String s = null;

		try {
			s = IoUtil.bytesToString(id);

			// This exception shouldn't occur since we control the constructor
		} catch (final IOException e) {
			s = "00000000";
		}

		return s;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(id);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final TaskID other = (TaskID) obj;
		if (!Arrays.equals(id, other.id))
			return false;
		return true;
	}
}
