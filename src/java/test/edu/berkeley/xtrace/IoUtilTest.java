package edu.berkeley.xtrace;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IoUtilTest {

	private Random rnd;
	TaskID[] tasks;

	@Before
	public void setUp() throws Exception {
		rnd = new Random();
		tasks = new TaskID[4];
		tasks[0] = new TaskID(4);
		tasks[1] = new TaskID(8);
		tasks[2] = new TaskID(12);
		tasks[3] = new TaskID(20);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLong() {
		final int NUM_TESTS = 100000;
		
		/* Stochastic tests */
		for (int i = 0; i < NUM_TESTS; i++) {
			long l = rnd.nextLong();
			assertEquals(l, IoUtil.hexStringToLong(IoUtil.longToString(l)));
		}
		
		/* Boundary cases */
		assertEquals(Long.MAX_VALUE, IoUtil.hexStringToLong(IoUtil.longToString(Long.MAX_VALUE)));
		assertEquals(Long.MIN_VALUE, IoUtil.hexStringToLong(IoUtil.longToString(Long.MIN_VALUE)));
		assertEquals(0L, IoUtil.hexStringToLong(IoUtil.longToString(0L)));
	}
	
	@Test
	public void testFastOpIdExtraction() {
		
		/* 4 byte taskid; 4 byte opid */
		XTraceMetadata md = new XTraceMetadata(new TaskID(4), (int) 0);
		assertEquals("00000000", IoUtil.fastOpIdExtraction(md.toString()));
		/* 4 byte taskid; 8 byte opid */
		md = new XTraceMetadata(new TaskID(4), (long) 0);
		assertEquals("0000000000000000", IoUtil.fastOpIdExtraction(md.toString()));
		/* 12 byte taskid; 4 byte opid */
		md = new XTraceMetadata(new TaskID(12), (int) 0);
		assertEquals("00000000", IoUtil.fastOpIdExtraction(md.toString()));
		/* 12 byte taskid; 8 byte opid */
		md = new XTraceMetadata(new TaskID(12), (long) 0);
		assertEquals("0000000000000000", IoUtil.fastOpIdExtraction(md.toString()));
		
		/* 10,000 stochastic tests */
		final int NUM_TESTS = 10000;
		
		for (int i = 0; i < NUM_TESTS; i++) {
			
			if (rnd.nextBoolean()) {
				// 4-byte opid
				md = new XTraceMetadata(tasks[rnd.nextInt(4)], rnd.nextInt());
			} else {
				// 8-byte opid
				md = new XTraceMetadata(tasks[rnd.nextInt(4)], rnd.nextLong());
			}
			
			assertEquals(md.getOpIdString(), IoUtil.fastOpIdExtraction(md.toString()));
		}
	}
	
	@Test
	public void opidExtractionPerformance() {
		final int NUM_TESTS = 100000;
		String[] md = new String[NUM_TESTS];
		
		for (int i = 0; i < NUM_TESTS; i++) {	
			if (rnd.nextBoolean()) {
				// 4-byte opid
				md[i] = new XTraceMetadata(tasks[rnd.nextInt(4)], rnd.nextInt()).toString();
			} else {
				// 8-byte opid
				md[i] = new XTraceMetadata(tasks[rnd.nextInt(4)], rnd.nextLong()).toString();
			}
		}
		
		long startStandard = System.currentTimeMillis();
		for (int i = 0; i < md.length; i++) {
			XTraceMetadata mdobj = XTraceMetadata.createFromString(md[i]);
			String opidstr = mdobj.getOpIdString();
		}
		long endStandard = System.currentTimeMillis();
		
		long startFast = System.currentTimeMillis();
		for (int i = 0; i < md.length; i++) {
			String opidstr = IoUtil.fastOpIdExtraction(md[i]);
		}
		long endFast = System.currentTimeMillis();
		
		System.out.println("Standard completed in " + (endStandard - startStandard) + " ms");
		System.out.println("Fast completed in " + (endFast - startFast) + " ms");
	}

}
