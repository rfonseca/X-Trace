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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.log4j.Logger;

/**
 * A tracing context, containing the X-Trace metadata that is propagated along
 * a computation chain. This consists of three parts: a TaskID that is common
 * for the task (see {@link TaskID}), an OpID that is the ID of an event in the
 * computation chain, and a set of options (see {@link OptionField}). This
 * XTraceMetadata object must be passed from host to host and thread to thread
 * to capture chains of computation. The {@link XTraceContext} class contains static
 * utility functions for maintaining a per-thread context. 
 * 
 * @author George Porter
 */
public final class XTraceMetadata {
	//private static final Logger LOG = Logger.getLogger(XTraceMetadata.class);

	private static final byte[] INVALID_ID = { 0x00, 0x00, 0x00, 0x00 };
	private static final byte MetadataVersion = 1;
	
	private final TaskID taskid;
	private byte[] opId;
	private OptionField[] options;
	private int numOptions; 
	
	/**
	 * The default constructor produces an invalid XTraceMetadata object
	 */
	public XTraceMetadata() {
		taskid = TaskID.createFromBytes(INVALID_ID, 0, INVALID_ID.length);
		opId = new byte[] {0x00, 0x00, 0x00, 0x00};
		options = null;
		numOptions = 0;
	}
	
	/**
	 * Creates an X-Trace metadata object
	 * 
	 * @param id
	 *            the Task ID associated with this metadata
	 * @param op
	 *            a 4 or 8-byte operation Id
	 */
	public XTraceMetadata(TaskID id, byte[] op) {
		if (id != null && op != null && (op.length == 4 || op.length == 8)) {
			taskid = id;
			opId = op;
		} else {
			if (id == null || op == null) {
				//LOG.warn("TaskID or opId can't be null");
			} else {
				//LOG.warn("Invalid operation id length: " + op.length);
			}
			taskid = TaskID.createFromBytes(INVALID_ID, 0, INVALID_ID.length);
			opId = new byte[] {0x00, 0x00, 0x00, 0x00};
		}
		options = null;
		numOptions = 0;
	}

	/**
	 * Creates an X-Trace metadata object with a 4-byte operation ID
	 * 
	 * @param id
	 *            the Task ID associated with this metadata
	 * @param opId
	 *            a 4-byte operation Id
	 */
	public XTraceMetadata(final TaskID id, final int opId) {
		if (id != null) {
			taskid = id;
			this.opId = ByteBuffer.allocate(4).putInt(opId).array();
		} else {
			taskid = TaskID.createFromBytes(INVALID_ID, 0, INVALID_ID.length);
			this.opId = new byte[] {0x00, 0x00, 0x00, 0x00};
			//LOG.warn("The supplied TaskID was null");
		}
		options = null;
		numOptions = 0;
	}
	
	/**
	 * Creates an X-Trace metadata object with an 8-byte operation ID
	 * 
	 * @param id
	 *            the Task ID associated with this metadata
	 * @param opId
	 *            an 8-byte operation Id
	 */
	public XTraceMetadata(final TaskID id, final long opId) {
		if (id != null) {
			taskid = id;
			this.opId = ByteBuffer.allocate(8).putLong(opId).array();
		} else {
			taskid = TaskID.createFromBytes(INVALID_ID, 0, INVALID_ID.length);
			this.opId = new byte[] {0x00, 0x00, 0x00, 0x00};
			//LOG.warn("The supplied TaskID was null");
		}
		options = null;
		numOptions = 0;
	}
	
	/**
	 * Copy constructor
	 * 
	 * @param xtr
	 */
	public XTraceMetadata(XTraceMetadata xtr) {
		// TaskID
		this.taskid = new TaskID(xtr.taskid);
		
		// OpId
		this.opId = new byte[xtr.opId.length];
		System.arraycopy(xtr.opId, 0, this.opId, 0, xtr.opId.length);
		
		// Options
		this.numOptions = xtr.numOptions;
		this.options = new OptionField[xtr.numOptions];
		for (int i = 0; i < xtr.numOptions; i++) {
			this.options[i] = xtr.options[i];
		}
	}

	/**
	 * Creates an X-Trace metadata object from an array of bytes
	 * 
	 * @param bytes
	 *            the byte array to use
	 * @param offset
	 *            the number of bytes to skip in the given array
	 * @param length
	 *            the length of bytes to read from the array
	 * @return a new <code>XtrMetadata</code> object based on the given byte
	 *         array
	 * @throws XTraceException
	 *             if the given bytes are invalid (do not follow the X-Trace
	 *             specification
	 */
	public static XTraceMetadata createFromBytes(final byte[] bytes,
			final int offset, final int length) {

		if (bytes == null) {
			//LOG.warn("'bytes' was null");
			return new XTraceMetadata();
		}
		
		if (offset < 0) {
			//LOG.warn("Invalid 'offset' argument: " + offset);
			return new XTraceMetadata();
		}
		
		if (length < 9) {
			//LOG.warn("XTraceMetadata length too short: " + length);
			return new XTraceMetadata();
		}
		
		if (bytes.length - offset < length) {
			//LOG.warn("Fewer than 'length' bytes given to constructor");
			return new XTraceMetadata();
		}
		
		// Task ID length
		int taskIdLength = 0;
		switch (bytes[0] & 0x03) {
		case 0x00:
			taskIdLength = 4;
			break;
		case 0x01:
			taskIdLength = 8;
			break;
		case 0x02:
			taskIdLength = 12;
			break;
		case 0x03:
			taskIdLength = 20;
			break;
		default:
				// Can't happen
		}
		
		// OpId length
		int opIdLength;
		if ((bytes[0] & 0x08) != 0) {
			opIdLength = 8;
		} else {
			opIdLength = 4;
		}
		
		// Make sure the flags don't imply a length that is too long
		if (taskIdLength + opIdLength > length) {
			//LOG.warn("TaskID length plus OpId length is longer than total length");
			return new XTraceMetadata();
		}
		
		// Create the TaskID and opId fields
		TaskID taskid = TaskID.createFromBytes(bytes, 1, taskIdLength);
		byte[] opIdBytes = new byte[opIdLength];
		System.arraycopy(bytes, 1+taskIdLength, opIdBytes, 0, opIdLength);
		
		XTraceMetadata md = new XTraceMetadata(taskid, opIdBytes);
		
		// If no options, we're done
		if ( (bytes[0] & 0x04) == 0 ) {
			return md;
		}
		
		// Otherwise, read in the total option length
		if (length <= 1 + taskIdLength + opIdLength) {
			//LOG.warn("Options present in flags byte, but no total option length given");
			return new XTraceMetadata();
		}
		int totOptLen = bytes[1 + taskIdLength + opIdLength];
		int optPtr = offset + 1 + taskIdLength + opIdLength + 1;
		
		while (totOptLen >= 2) {
			byte type = bytes[optPtr++];
			byte len = bytes[optPtr++];
			if (len > totOptLen) {
				//LOG.warn("Invalid option length");
				break;
			}
			
			OptionField o;
			if (len > 0) {
				o = OptionField.createFromBytes(bytes, optPtr, len);
			} else {
				o = new OptionField(type, null);
			}
			md.addOption(o);
			totOptLen -= (2 + len);
			optPtr += (2 + len);
		}
		return md;
	}

	/**
	 * Creates an X-Trace metadata object from a String, in the format provided
	 * by the X-Trace specification.
	 * 
	 * @param str
	 *            the String that represents the metadata
	 * @return a new <code>XtrMetadata</code> object based on the given String
	 */
	public static XTraceMetadata createFromString(final String str) {
		
		if (str == null) {
			//LOG.warn("null String passed to createFromString");
			return new XTraceMetadata();
		}
		
		byte[] bytes;
		try {
			bytes = IoUtil.stringToBytes(str);
		} catch (IOException e) {
			//LOG.warn("Invalid X-Trace metadata string: " + str);
			return new XTraceMetadata();
		}
		return createFromBytes(bytes, 0, bytes.length);
	}

	/**
	 * Serializes the metadata as an array of bytes, according to the X-Trace
	 * specification. If the resulting byte array is passed to
	 * <code>createFromBytes</code>, the original object will be recovered.
	 * 
	 * @return the metadata as an array of bytes
	 */
	public byte[] pack() {
		final ByteBuffer buf = packToBuffer();
		final byte[] ar = new byte[buf.limit()];
		buf.get(ar);
		return ar;
	}

	/**
	 * Serializes the metadata into a ByteBuffer.
	 * 
	 * @return the metadata as a ByteBuffer.
	 */
  private ByteBuffer packToBuffer() {
    final ByteBuffer buf = ByteBuffer.allocate(1024);

		/* flags */
		byte flags = 0;

		switch (taskid.get().length) {
		case 4:
			flags = (byte) 0x00;
			break;
		case 8:
			flags = (byte) 0x01;
			break;
		case 12:
			flags = (byte) 0x02;
			break;
		case 20:
			flags = (byte) 0x03;
			break;
		default:
			// shouldn't happen
		}
		
		flags |= (MetadataVersion << 4);
		
		if (getOptions() != null && getOptions().length > 0)
			flags |= 0x04;
		
		if (opId.length == 8)
			flags |= 0x08;

		buf.put(flags);

		/* TaskID */
		buf.put(taskid.pack());

		/* OpId */
		buf.put(opId);

		/* Options */
		if (getOptions() != null) {
			OptionField[] opts = getOptions();
			int optLenPosition = buf.position();
			byte totalOptLength = 0;

			/* A placeholder for the total options length byte */
			if (opts != null && opts.length > 0)
				buf.put((byte)0);

			/* Options */
			for (int i = 0; opts != null && i < opts.length; i++) {
				OptionField opt = opts[i];
			    byte[] optBytes = opt.pack();
				totalOptLength += optBytes.length;
				buf.put(optBytes);
			}

			/* Now go back and figure out how big the total options length is */
			buf.put(optLenPosition, totalOptLength);
		}

		/* return the flipped buffer */
		buf.flip();
        return buf;
  }

	/**
	 * Indicates whether this X-Trace metadata is invalid, which means that it
	 * is uninitialized or otherwise not usable.
	 * 
	 * @return whether this metadata is invalid
	 */
	public boolean isValid() {
		final byte[] taskidbytes = this.taskid.get();
		for (byte element : taskidbytes) {
			if (element != 0)
				return true;
		}
		return false;
	}

	/**
	 * Returns the number of bytes that this <code>XtrMetadata</code> object
	 * would take up in its serialized form.
	 * 
	 * @return the number of bytes <code>pack</code> would return
	 * @see #pack()
	 */
	public int sizeAsBytes() {
		return packToBuffer().limit();
	}

	/**
	 * Accesses the set of X-Trace options in this metadata, if any are present.
	 * 
	 * @return an Iterator representing the set of any X-Trace options present
	 */
	public OptionField[] getOptions() {
		if (numOptions > 0) {
			return options;
		} else {
			return null;
		}
	}
	public int getNumOptions() {
		return numOptions;
	}

	/**
	 * Adds a new X-Trace option to this metadata object.
	 * 
	 * @param option
	 *            the option to add to this metadata object
	 */
	public void addOption(final OptionField option) {
		
		// We could do a dynamically resizable array here where the size
		// doubles each time it fills up, leading to a constant
		// time amortized insertion.  However, that is probably overkill
		
		if (numOptions == 0) {
			options = new OptionField[1];
		} else if (numOptions == options.length) {
			OptionField[] tmp = options;
			options = new OptionField[options.length + 1];
			System.arraycopy(tmp, 0, options, 0, tmp.length);
		}
		options[numOptions++] = option;
	}

	/**
	 * Returns the X-Trace metadata version
	 * 
	 * @return X-Trace metadata version
	 */
	public int getVersion() {
		return (int) MetadataVersion;
	}
	
	/**
	 * Returns the X-Trace Task ID
	 * 
	 * @return the taskID part of this metadata
	 */
	public TaskID getTaskId() {
		return taskid;
	}
	
	/**
	 * Returns the length of the opID field in bytes
	 * 
	 * @return the length of the opID field in bytes
	 */
	public int getOpIdLength() {
		return opId.length;
	}

	/**
	 * Returns the X-Trace opId field
	 * 
	 * @return the opId part of this metadata
	 */
	public byte[] getOpId() {
		return opId;
	}
	
	/**
	 * Returns the X-Trace opId field, formatted as a hexadecimal string
	 * of the appropriate length
	 * 
	 * @return the opId part of this metadata formatted as a hexadecimal string
	 */
	public String getOpIdString() {
		try {
			return IoUtil.bytesToString(opId);
		} catch (IOException e) {
			//LOG.warn("Internal I/O error", e);
		}
		return "0";
	}
	
	/**
	 * Sets the operation id
	 * 
	 * @param newid the new operation ID
	 */
	public void setOpId(byte[] newid) {
		if (newid.length != 4 && newid.length != 8) {
			//LOG.warn("Asked to set an OpId with invalid length: " + newid.length);
			return;
		}
		this.opId = new byte[newid.length];
		System.arraycopy(newid, 0, this.opId, 0, newid.length);
	}
	
	/**
	 * Sets the operation id
	 * 
	 * @param newid the new operation ID
	 */
	public void setOpId(int newid) {
		this.opId = ByteBuffer.allocate(4).putInt(newid).array();
	}
	
	/**
	 * Sets the operation id
	 * 
	 * @param newid the new operation ID
	 */
	public void setOpId(long newid) {
		this.opId = ByteBuffer.allocate(8).putLong(newid).array();
	}
	/**
	* Sets the severity via an option field
	* creates new option field if one for severity doesn't already exist
	* @param severity level to be stored in metadata
	* (see xtrace specification for levels)
	*/
	public void setSeverity(int severity) {
		byte type = OptionField.SEVERITY;
		byte[] payload = { new Integer(severity).byteValue() };
		if (numOptions > 0) { // use existing option field spot if it exists
			for (int i=0; i <numOptions; i++) {
				if (options[i].getType()-OptionField.SEVERITY == 0) {
					options[i]=new OptionField(type, payload);
				}
			}
		}
		else { // create a new option field
			addOption(new OptionField(type, payload));
		}
	}
	/**
	 * Returns the String representation of this X-Trace metadata, in the format
	 * specified by the X-Trace spec.
	 * 
	 * @return the String representation of this metadata
	 */
	@Override
	public String toString() {
		String s = null;

		try {
			s = IoUtil.bytesToString(pack());
		} catch (final IOException e) {
			//LOG.warn("Internal I/O error: ", e);
			return "000000000000000000";
		}

		return s;
	}

	/**
	 * Write this metadata to a DataOutput object in a format that can be
	 * read by {@link #read(DataInput)}. This writes the
	 * length of the object as an int, followed by the data obtained from
	 * {@link #pack()}.
	 * 
	 * @param out
	 *                object to write to
	 */
    public void write(DataOutput out) throws IOException {
		byte[] buf = pack();
		out.writeInt(buf.length);
		out.write(buf);
    }

    /**
	 * Read a metadata object in the format written by
	 * {@link #write(DataOutput)} from a DataInput object.
	 * 
	 * @param in object to read from
	 * @return the XtrMetadata object read
	 * @throws IOException if an XtrMetadata object as formatted by
	 *                     {@link #write(DataOutput)} cannot be read
	 */
	public static XTraceMetadata read(DataInput in)
			throws IOException {
		int length = in.readInt();
		if (length <= 0 || length > 4096) {
			throw new IOException("Invalid X-Trace metadata length: " + length);
		}
		byte[] buf = new byte[length];
		in.readFully(buf);
		return createFromBytes(buf, 0, length);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + numOptions;
		result = prime * result + Arrays.hashCode(opId);
		result = prime * result + Arrays.hashCode(options);
		result = prime * result + ((taskid == null) ? 0 : taskid.hashCode());
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
		final XTraceMetadata other = (XTraceMetadata) obj;
		if (numOptions != other.numOptions)
			return false;
		if (!Arrays.equals(opId, other.opId))
			return false;
		if (!Arrays.equals(options, other.options))
			return false;
		if (taskid == null) {
			if (other.taskid != null)
				return false;
		} else if (!taskid.equals(other.taskid))
			return false;
		return true;
	}

	/**
	 * Write a given XTraceMetadata context to a DataOutput object, or send an 
	 * invalid metadata object if context==null. 
	 * @param context
	 * @param out
	 * @throws IOException
	 */
	public static void write(XTraceMetadata context, DataOutput out)
			throws IOException {
		if (context != null) {
			context.write(out);
		} else {
			// write an invalid XTraceMetadata to represent no context
			new XTraceMetadata().write(out);
		}
	}
}
