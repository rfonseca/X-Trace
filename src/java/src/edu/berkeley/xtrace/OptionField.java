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
import java.util.Arrays;

import org.apache.log4j.Logger;


/**
 * An X-trace option field.  Options are designed to support extensions
 * to the X-Trace metadata, and should be passed during propagation
 * operations.
 * 
 * @author George Porter
 */
public class OptionField {
	public static final byte NOP = 0;
	public static final byte SEVERITY = (byte)0xCE;

	private static final Logger LOG = Logger.getLogger(OptionField.class);
	
	private final byte type;
	private final byte[] payload;
	
	/**
	 * Create an 'nop' X-Trace Option
	 */
	private OptionField() {
		this.type = 0;
		this.payload = null;
	}
	
	private OptionField(byte type, byte[] payloadbytes, int payloadoffset, int payloadlength) {
		if (payloadlength > 256) {
			LOG.warn("Option payloads cannot exceed 256 bytes");
			this.type = 0;
			this.payload = null;
		} else {
			this.type = type;
			if (payloadbytes != null && payloadlength > 0) {
				this.payload = new byte[payloadlength];
				System.arraycopy(payloadbytes, payloadoffset, this.payload, 0, payloadlength);
			} else {
				this.payload = null;
			}
		}
	}
	
	public OptionField(byte type, byte[] payload) {
		this.type = type;
		if (payload != null) {
			this.payload = new byte[payload.length];
			System.arraycopy(payload, 0, this.payload, 0, payload.length);
		} else {
			this.payload = null;
		}
	}
	
	public static OptionField createFromBytes(byte[] bytes, int offset, int length) {
		if (bytes == null) {
			LOG.warn("'bytes' cannot be null");
			return new OptionField();
		}
		
		if (bytes.length - offset < length) {
			LOG.warn("'length' field too large for the bytes provided");
			return new OptionField();
		}
		
		if (length > 258) {
			LOG.warn("Length of Option payload cannot exceed 256 bytes");
			return new OptionField();
		}
		return new OptionField(bytes[offset-2], bytes, offset, length);
	}
	
	public static OptionField createFromString(String s) {
		byte[] bytes = null;
		
		try {
			bytes = IoUtil.stringToBytes(s);
		} catch (IOException e) {
			LOG.warn("Invalid String: " + s);
			return new OptionField();
		}
		
		return createFromBytes(bytes, 0, bytes.length);
	}

	public byte getType() {
		return type;
	}

	public byte[] getPayload() {
		return payload;
	}
	
	public byte[] pack() {
		if (payload == null) {
			byte[] ret = new byte[2];
			ret[0] = type;
			ret[1] = 0x02;
			return ret;
		}
		
		byte[] buf = new byte[payload.length + 2];
		buf[0] = type;
		buf[1] = (byte) (payload.length);
		System.arraycopy(payload, 0, buf, 2, payload.length);
		return buf;
	}
	
	public String toString() {
		try {
			return IoUtil.bytesToString(pack());
		} catch (IOException e) {
			// shouldn't happen
			LOG.warn("Internal I/O error");
			return "0002";
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + type;
		result = prime * result + Arrays.hashCode(payload);
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
		final OptionField other = (OptionField) obj;
		if (!Arrays.equals(payload, other.payload))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
}


