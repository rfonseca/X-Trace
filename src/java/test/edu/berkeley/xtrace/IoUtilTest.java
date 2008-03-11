package edu.berkeley.xtrace;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IoUtilTest {

	private Random rnd;

	@Before
	public void setUp() throws Exception {
		rnd = new Random();
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

}
