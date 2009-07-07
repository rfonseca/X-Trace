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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.varia.NullAppender;
import org.junit.Before;
import org.junit.Test;

/**
 * @author George Porter
 *
 */
public class MetadataTest {
	
	private static final int BigStochasticTests = 10000;
	private static final int SmallStochasticTests = 100;

	private byte[][] goodOps;
	private byte[][] badOps;
	private TaskID[] goodTasks;
	private Random rnd;
	private String[] validStrings;
	private byte[][] validBytes;
	private byte[][] goodTaskBytes;
	private String[] invalidStrings;
	private byte[][] invalidBytes;
	private String[] goodOptions;
	private String[] badOptions;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		BasicConfigurator.configure(new NullAppender());
		
		rnd = new Random();
		
		goodOps = new byte[4][];
		goodOps[0] = new byte[] {0x00, 0x00, 0x00, 0x00};
		goodOps[1] = new byte[] {0x01, 0x02, 0x03, 0x04};
		goodOps[2] = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
		goodOps[3] = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
		
		badOps = new byte[4][];
		badOps[0] = new byte[] {0x00, 0x00, 0x00};
		badOps[1] = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04};
		badOps[2] = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
		badOps[2] = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
		
		goodTaskBytes = new byte[4][];
		goodTaskBytes[0] = IoUtil.stringToBytes("CAFEBABE");
		goodTaskBytes[1] = IoUtil.stringToBytes("CAFEBABECAFEBABE");
		goodTaskBytes[2] = IoUtil.stringToBytes("CAFEBABECAFEBABECAFEBABE");
		goodTaskBytes[3] = IoUtil.stringToBytes("CAFEBABECAFEBABECAFEBABECAFEBABECAFEBABE");
		
		goodTasks = new TaskID[4];
		goodTasks[0] = new TaskID(4);
		goodTasks[1] = new TaskID(8);
		goodTasks[2] = new TaskID(12);
		goodTasks[3] = new TaskID(20);
		
		validStrings = new String[8];
		validStrings[0] = "10CAFEBABE01020304"; // taskid: 4; opid: 4
		validStrings[1] = "18CAFEBABE0102030405060708"; // taskid: 4; opid: 8
		validStrings[2] = "11CAFEBABECAFEBABE01020304"; // taskid: 8; opid: 4
		validStrings[3] = "19CAFEBABECAFEBABE0102030405060708"; // taskid: 8; opid: 8
		validStrings[4] = "12CAFEBABECAFEBABECAFEBABE01020304"; // taskid: 12; opid: 4
		validStrings[5] = "1ACAFEBABECAFEBABECAFEBABE0102030405060708"; // taskid: 12; opid: 8
		validStrings[6] = "13CAFEBABECAFEBABECAFEBABECAFEBABECAFEBABE01020304"; // taskid: 20; opid: 4
		validStrings[7] = "1BCAFEBABECAFEBABECAFEBABECAFEBABECAFEBABE0102030405060708"; // task: 20; op: 8
		
		validBytes = new byte[validStrings.length][];
		for (int i = 0; i < validStrings.length; i++) {
			validBytes[i] = IoUtil.stringToBytes(validStrings[i]);
		}
		
		invalidStrings = new String[9];
		invalidStrings[0] = "10CAFEBA01020304"; // task too short
		invalidStrings[1] = "1001020304"; // no task
		invalidStrings[2] = ""; // empty contents
		invalidStrings[3] = "10"; // nothing but flags
		invalidStrings[4] = "10CAFEBABE"; // no Op ID
		invalidStrings[5] = "10CAFEBABE0102"; // op ID too short
		invalidStrings[6] = "11CAFEBABE01020304"; // task length mismatch
		invalidStrings[7] = "14CAFEBABE01020304"; // options indicated, but not present
		invalidStrings[8] = "18CAFEBABE01020304"; // opId length mismatch
		
		invalidBytes = new byte[invalidStrings.length][];
		for (int i = 0; i < invalidStrings.length; i++) {
			invalidBytes[i] = IoUtil.stringToBytes(invalidStrings[i]);
		}
		
		goodOptions = new String[5];
		goodOptions[0] = "0002";
		goodOptions[1] = "0102";
		goodOptions[2] = "02040001";
		goodOptions[3] = "030600010203";
		goodOptions[4] = "04070001020304";
		
		badOptions = new String[4];
		badOptions[0] = "";
		badOptions[1] = "00";
		badOptions[2] = "0003";
		badOptions[3] = "01050001";
		
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#Metadata()}.
	 */
	@Test
	public void testMetadata() {
		XTraceMetadata xtr = new XTraceMetadata();
		assertNotNull(xtr);
		assertFalse(xtr.isValid());
		assertNotNull(xtr.getOpId());
		assertNull(xtr.getOptions());
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#Metadata(edu.berkeley.xtrace.TaskID, byte[])}.
	 */
	@Test
	public void testMetadataXtrTaskIDByteArray() {
		// Test good cases
		for (int i = 0; i < goodTasks.length; i++) {
			for (int j = 0; j < goodOps.length; j++) {
				XTraceMetadata md = new XTraceMetadata(goodTasks[i], goodOps[j]);
				assertNotNull(md);
				assertTrue(md.isValid());
			}
		}

		for (int i = 0; i < goodOps.length; i++) {
			XTraceMetadata md = new XTraceMetadata(null, goodOps[i]);
			assertNotNull(md);
			assertFalse(md.isValid());
		}
		
		// opId is invalid
		for (int i = 0; i < badOps.length; i++) {
			XTraceMetadata md = new XTraceMetadata(goodTasks[0], badOps[i]);
			assertNotNull(md);
			assertFalse(md.isValid());
		}
		
		// task is null
		for (int i = 0; i < goodOps.length; i++) {
			XTraceMetadata md = new XTraceMetadata(null, goodOps[i]);
			assertNotNull(md);
			assertFalse(md.isValid());
		}
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#Metadata(edu.berkeley.xtrace.TaskID, int)}.
	 */
	@Test
	public void testMetadataXtrTaskIDInt() {
		for (int i = 0; i < goodTasks.length; i++) {
			for (int j = 0; j < BigStochasticTests; j++) {
				XTraceMetadata md = new XTraceMetadata(goodTasks[i], rnd.nextInt());
				assertNotNull(md);
				assertTrue(md.isValid());
			}
		}
		
		for (int j = 0; j < SmallStochasticTests; j++) {
			XTraceMetadata md = new XTraceMetadata(null, rnd.nextInt());
			assertNotNull(md);
			assertFalse(md.isValid());
		}
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#Metadata(edu.berkeley.xtrace.TaskID, long)}.
	 */
	@Test
	public void testMetadataXtrTaskIDLong() {
		for (int i = 0; i < goodTasks.length; i++) {
			for (int j = 0; j < BigStochasticTests; j++) {
				XTraceMetadata md = new XTraceMetadata(goodTasks[i], rnd.nextLong());
				assertNotNull(md);
				assertTrue(md.isValid());
			}
		}
		
		for (int j = 0; j < SmallStochasticTests; j++) {
			XTraceMetadata md = new XTraceMetadata(null, rnd.nextLong());
			assertNotNull(md);
			assertFalse(md.isValid());
		}
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#createFromBytes(byte[], int, int)}.
	 */
	@Test
	public void testCreateFromBytes() {
		// Try all 4 x 2 good combinations
		XTraceMetadata md = XTraceMetadata.createFromBytes(validBytes[0], 0, validBytes[0].length);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(md.getTaskId(), TaskID.createFromBytes(goodTaskBytes[0], 0, goodTaskBytes[0].length));
		assertEquals(4, md.getOpIdLength());
		assertArrayEquals(goodOps[1], md.getOpId());
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromBytes(validBytes[1], 0, validBytes[1].length);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(TaskID.createFromBytes(goodTaskBytes[0], 0, goodTaskBytes[0].length), md.getTaskId());
		assertEquals(8, md.getOpIdLength());
		assertArrayEquals(goodOps[3], md.getOpId());
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromBytes(validBytes[2], 0, validBytes[2].length);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(md.getTaskId(), TaskID.createFromBytes(goodTaskBytes[1], 0, goodTaskBytes[1].length));
		assertEquals(4, md.getOpIdLength());
		assertArrayEquals(goodOps[1], md.getOpId());
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromBytes(validBytes[3], 0, validBytes[3].length);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(md.getTaskId(), TaskID.createFromBytes(goodTaskBytes[1], 0, goodTaskBytes[1].length));
		assertEquals(8, md.getOpIdLength());
		assertArrayEquals(goodOps[3], md.getOpId());
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromBytes(validBytes[4], 0, validBytes[4].length);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(md.getTaskId(), TaskID.createFromBytes(goodTaskBytes[2], 0, goodTaskBytes[2].length));
		assertEquals(4, md.getOpIdLength());
		assertArrayEquals(goodOps[1], md.getOpId());
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromBytes(validBytes[5], 0, validBytes[5].length);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(md.getTaskId(), TaskID.createFromBytes(goodTaskBytes[2], 0, goodTaskBytes[2].length));
		assertEquals(8, md.getOpIdLength());
		assertArrayEquals(goodOps[3], md.getOpId());
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromBytes(validBytes[6], 0, validBytes[6].length);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(md.getTaskId(), TaskID.createFromBytes(goodTaskBytes[3], 0, goodTaskBytes[3].length));
		assertEquals(4, md.getOpIdLength());
		assertArrayEquals(goodOps[1], md.getOpId());
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromBytes(validBytes[7], 0, validBytes[7].length);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(md.getTaskId(), TaskID.createFromBytes(goodTaskBytes[3], 0, goodTaskBytes[3].length));
		assertEquals(8, md.getOpIdLength());
		assertArrayEquals(goodOps[3], md.getOpId());
		assertNull(md.getOptions());
		
		// null case
		md = XTraceMetadata.createFromBytes(null, 0, 0);
		assertNotNull(md);
		assertFalse(md.isValid());
		assertNotNull(md.getTaskId());
		
		// various invalid byte arrays
		for (int i = 0; i < invalidBytes.length; i++) {
			md = XTraceMetadata.createFromBytes(invalidBytes[i], 0, invalidBytes[i].length);
			assertNotNull(md);
			assertFalse(md.isValid());
			assertNotNull(md.getTaskId());
			assertNull(md.getOptions());
		}
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#createFromString(java.lang.String)}.
	 */
	@Test
	public void testCreateFromString() {
		XTraceMetadata md = XTraceMetadata.createFromString(validStrings[0]);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(md.getTaskId(), TaskID.createFromBytes(goodTaskBytes[0], 0, goodTaskBytes[0].length));
		assertEquals(4, md.getOpIdLength());
		assertArrayEquals(goodOps[1], md.getOpId());
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromString(validStrings[1]);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(TaskID.createFromBytes(goodTaskBytes[0], 0, goodTaskBytes[0].length), md.getTaskId());
		assertEquals(8, md.getOpIdLength());
		assertArrayEquals(goodOps[3], md.getOpId());
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromString(validStrings[2]);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(md.getTaskId(), TaskID.createFromBytes(goodTaskBytes[1], 0, goodTaskBytes[1].length));
		assertEquals(4, md.getOpIdLength());
		assertArrayEquals(goodOps[1], md.getOpId());
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromString(validStrings[3]);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(md.getTaskId(), TaskID.createFromBytes(goodTaskBytes[1], 0, goodTaskBytes[1].length));
		assertEquals(8, md.getOpIdLength());
		assertArrayEquals(goodOps[3], md.getOpId());
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromString(validStrings[4]);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(md.getTaskId(), TaskID.createFromBytes(goodTaskBytes[2], 0, goodTaskBytes[2].length));
		assertEquals(4, md.getOpIdLength());
		assertArrayEquals(goodOps[1], md.getOpId());
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromString(validStrings[5]);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(md.getTaskId(), TaskID.createFromBytes(goodTaskBytes[2], 0, goodTaskBytes[2].length));
		assertEquals(8, md.getOpIdLength());
		assertArrayEquals(goodOps[3], md.getOpId());
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromString(validStrings[6]);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(md.getTaskId(), TaskID.createFromBytes(goodTaskBytes[3], 0, goodTaskBytes[3].length));
		assertEquals(4, md.getOpIdLength());
		assertArrayEquals(goodOps[1], md.getOpId());
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromString(validStrings[7]);
		assertNotNull(md);
		assertTrue(md.isValid());
		assertEquals(md.getTaskId(), TaskID.createFromBytes(goodTaskBytes[3], 0, goodTaskBytes[3].length));
		assertEquals(8, md.getOpIdLength());
		assertArrayEquals(goodOps[3], md.getOpId());
		assertNull(md.getOptions());
		
		// null case
		md = XTraceMetadata.createFromString(null);
		assertNotNull(md);
		assertFalse(md.isValid());
		assertNotNull(md.getTaskId());
		
		// various invalid byte arrays
		for (int i = 0; i < invalidStrings.length; i++) {
			md = XTraceMetadata.createFromString(invalidStrings[i]);
			assertNotNull(md);
			assertFalse(md.isValid());
			assertNotNull(md.getTaskId());
			assertNull(md.getOptions());
		}
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#pack()}.
	 */
	@Test
	public void testPack() {
		for (int i = 0; i < validBytes.length; i++) {
			XTraceMetadata md = XTraceMetadata.createFromBytes(validBytes[i], 0, validBytes[i].length);
			assertArrayEquals(validBytes[i], md.pack());
		}
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#isValid()}.
	 */
	@Test
	public void testIsValid() {
		assertFalse((new XTraceMetadata()).isValid());
		
		for (int i = 0; i < validBytes.length; i++) {
			XTraceMetadata md = XTraceMetadata.createFromBytes(validBytes[i], 0, validBytes[i].length);
			assertTrue(md.isValid());
		}
		
		for (int i = 0; i < invalidBytes.length; i++) {
			XTraceMetadata md = XTraceMetadata.createFromBytes(invalidBytes[i], 0, invalidBytes[i].length);
			assertFalse(md.isValid());
		}
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#sizeAsBytes()}.
	 */
	@Test
	public void testSizeAsBytes() {
		assertEquals(9, XTraceMetadata.createFromString(validStrings[0]).sizeAsBytes());
		assertEquals(13, XTraceMetadata.createFromString(validStrings[1]).sizeAsBytes());
		assertEquals(13, XTraceMetadata.createFromString(validStrings[2]).sizeAsBytes());
		assertEquals(17, XTraceMetadata.createFromString(validStrings[3]).sizeAsBytes());
		assertEquals(17, XTraceMetadata.createFromString(validStrings[4]).sizeAsBytes());
		assertEquals(21, XTraceMetadata.createFromString(validStrings[5]).sizeAsBytes());
		assertEquals(25, XTraceMetadata.createFromString(validStrings[6]).sizeAsBytes());
		assertEquals(29, XTraceMetadata.createFromString(validStrings[7]).sizeAsBytes());
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#getOptions()}
	 * and {@link edu.berkeley.xtrace.XTraceMetadata#addOption(edu.berkeley.xtrace.OptionField)}
	 */
	public void TODOtestOptions() {
		
		// Are these string comparison tests valid?  I don't think
		// the spec requires options to appear in the serialized
		// String in the same order they were inserted...
		
		XTraceMetadata md = new XTraceMetadata();
		assertNull(md.getOptions());
		
		md = XTraceMetadata.createFromString(validStrings[0]);
		assertNull(md.getOptions());
		assertEquals("10CAFEBABE01020304", md.toString());
		
		// Test composed insertions with null payloads
		OptionField o = new OptionField((byte) 0, null);
		md.addOption(o);
		assertNotNull(md.getOptions());
		assertEquals(1, md.getOptions().length);
		assertEquals(o, md.getOptions()[0]);
		assertEquals("14CAFEBABE01020304020002", md.toString());
		
		OptionField o2 = new OptionField((byte) 1, null);
		md.addOption(o2);
		assertNotNull(md.getOptions());
		assertEquals(2, md.getOptions().length);
		assertEquals(o, md.getOptions()[0]);
		assertEquals(o2, md.getOptions()[1]);
		assertEquals("14CAFEBABE010203040400020102", md.toString());
		
		OptionField o3 = new OptionField((byte) 2, null);
		md.addOption(o3);
		assertNotNull(md.getOptions());
		assertEquals(3, md.getOptions().length);
		assertEquals(o, md.getOptions()[0]);
		assertEquals(o2, md.getOptions()[1]);
		assertEquals(o3, md.getOptions()[2]);
		assertEquals("14CAFEBABE0102030406000201020202", md.toString());
		
		// Test composed insertions with non-null payloads
		md = XTraceMetadata.createFromString(validStrings[0]);
		assertNull(md.getOptions());
		assertEquals("10CAFEBABE01020304", md.toString());
		
		o = new OptionField((byte) 0, new byte[] {0x00, 0x01, 0x02, 0x03});
		md.addOption(o);
		assertNotNull(md.getOptions());
		assertEquals(1, md.getOptions().length);
		assertEquals(o, md.getOptions()[0]);
		assertEquals("14CAFEBABE0102030406000600010203", md.toString());
		
		o2 = new OptionField((byte) 1, new byte[] {0x05, 0x06});
		md.addOption(o2);
		assertNotNull(md.getOptions());
		assertEquals(2, md.getOptions().length);
		assertEquals(o, md.getOptions()[0]);
		assertEquals(o2, md.getOptions()[1]);
		assertEquals("14CAFEBABE010203040A00060001020301040506", md.toString());
		
		o3 = new OptionField((byte) 2, new byte[] {0x07});
		md.addOption(o3);
		assertNotNull(md.getOptions());
		assertEquals(3, md.getOptions().length);
		assertEquals(o, md.getOptions()[0]);
		assertEquals(o2, md.getOptions()[1]);
		assertEquals(o3, md.getOptions()[2]);
		assertEquals("14CAFEBABE010203040D00060001020301040506020307", md.toString());
		
		// Test composed insertions with null and non-null payloads
		md = XTraceMetadata.createFromString(validStrings[0]);
		assertNull(md.getOptions());
		
		o = new OptionField((byte) 0, new byte[] {0x00, 0x01, 0x02, 0x03});
		md.addOption(o);
		assertNotNull(md.getOptions());
		assertEquals(1, md.getOptions().length);
		assertEquals(o, md.getOptions()[0]);
		
		o2 = new OptionField((byte) 1, null);
		md.addOption(o2);
		assertNotNull(md.getOptions());
		assertEquals(2, md.getOptions().length);
		assertEquals(o, md.getOptions()[0]);
		assertEquals(o2, md.getOptions()[1]);
		
		o3 = new OptionField((byte) 2, new byte[] {0x07});
		md.addOption(o3);
		assertNotNull(md.getOptions());
		assertEquals(3, md.getOptions().length);
		assertEquals(o, md.getOptions()[0]);
		assertEquals(o2, md.getOptions()[1]);
		assertEquals(o3, md.getOptions()[2]);
	}

	public void testOptionSerialization() {
		for (int i = 0; i < goodOptions.length; i++) {
			OptionField o = OptionField.createFromString(goodOptions[i]);
			assertNotNull(o);
			assertEquals(goodOptions[i], o.toString());
		}
		
		OptionField o = new OptionField((byte) 0x01, null);
		assertNotNull(o);
		assertEquals(0x01, o.getType());
		assertNull(o.getPayload());
		
		o = new OptionField((byte) 0x02, new byte[] {0x00, 0x01, 0x02, 0x03});
		assertNotNull(o);
		assertEquals(0x02, o.getType());
		assertNotNull(o.getPayload());
		assertArrayEquals(new byte[] {0x00, 0x01, 0x02, 0x03}, o.getPayload());
	}
	
	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#getVersion()}.
	 */
	@Test
	public void testGetVersion() {
		XTraceMetadata md = XTraceMetadata.createFromString(validStrings[0]);
		assertEquals(md.getVersion(), 1);
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#getTaskId()}.
	 */
	@Test
	public void testGetTaskId() {
		for (int i = 0; i < goodTasks.length; i++) {
			XTraceMetadata md = new XTraceMetadata(goodTasks[i], 0);
			assertEquals(goodTasks[i], md.getTaskId());
		}
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#getOpIdLength()}.
	 */
	@Test
	public void testGetOpIdLength() {
		XTraceMetadata md = new XTraceMetadata(goodTasks[0], goodOps[0]);
		assertEquals(4, md.getOpIdLength());
		md = new XTraceMetadata(goodTasks[0], goodOps[1]);
		assertEquals(4, md.getOpIdLength());
		md = new XTraceMetadata(goodTasks[0], goodOps[2]);
		assertEquals(8, md.getOpIdLength());
		md = new XTraceMetadata(goodTasks[0], goodOps[3]);
		assertEquals(8, md.getOpIdLength());
		md = new XTraceMetadata();
		assertTrue(md.getOpIdLength() == 4 || md.getOpIdLength() == 8);
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#getOpId()}.
	 */
	@Test
	public void testGetOpId() {
		for (int i = 0; i < goodOps.length; i++) {
			XTraceMetadata md = new XTraceMetadata(goodTasks[0], goodOps[i]);
			assertNotNull(md.getOpId());
			assertArrayEquals(goodOps[i], md.getOpId());
		}
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#getOpIdString()}.
	 */
	@Test
	public void testGetOpIdString() {
		for (int i = 0; i < goodOps.length; i++) {
			XTraceMetadata md = new XTraceMetadata(goodTasks[0], goodOps[i]);
			assertNotNull(md.getOpId());
			try {
				assertEquals(IoUtil.bytesToString(goodOps[i]), md.getOpIdString());
			} catch (IOException e) {
				fail("Internal error");
			}
		}
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#setOpId(byte[])},
	 * {@link edu.berkeley.xtrace.XTraceMetadata#setOpId(int)}, and
	 * {@link edu.berkeley.xtrace.XTraceMetadata#setOpId(long)}
	 */
	@Test
	public void testSetOpId() {
		XTraceMetadata md = new XTraceMetadata(goodTasks[0], goodOps[0]);
		assertEquals(goodOps[0], md.getOpId());
		
		md.setOpId(goodOps[1]);
		assertArrayEquals(goodOps[1], md.getOpId());
		
		md.setOpId(goodOps[2]);
		assertArrayEquals(goodOps[2], md.getOpId());
		
		md.setOpId((int) 8);
		assertEquals(4, md.getOpIdLength());
		assertArrayEquals(new byte[] {0x00, 0x00, 0x00, 0x08}, md.getOpId());
		
		md.setOpId((long) 7);
		assertEquals(8, md.getOpIdLength());
		assertArrayEquals(new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07}, md.getOpId());
	}

	/**
	 * Test method for {@link edu.berkeley.xtrace.XTraceMetadata#toString()}.
	 */
	@Test
	public void testToString() {
		for (int i = 0; i < validStrings.length; i++) {
			XTraceMetadata md = XTraceMetadata.createFromString(validStrings[i]);
			assertEquals(validStrings[i], md.toString());
		}
		
		for (int i = 0; i < invalidStrings.length; i++) {
			XTraceMetadata md = XTraceMetadata.createFromString(invalidStrings[i]);
			assertEquals((new XTraceMetadata()).toString(), md.toString());
		}
	}
	
	@Test
	public void testLegacyMetadata() {
		// Let's see if the new XTraceMetadata class handles older metadata ok
		String[] old = new String[3];
		old[0] = "0000010203AABBCCDD";
		old[1] = "010001020304050607AABBCCDD";
		old[2] = "0000010203AABBCCDD0704070001020304";
		
		for (int i = 0; i < old.length; i++) {
			XTraceMetadata m = XTraceMetadata.createFromString(old[i]);
			assertNotNull(m);
			assertTrue(m.isValid());
		}
	}
}
