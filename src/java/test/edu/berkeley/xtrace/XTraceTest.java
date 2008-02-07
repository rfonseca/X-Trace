package edu.berkeley.xtrace;

import static org.junit.Assert.*;

import org.junit.Test;

public class XTraceTest {

	@Test
	public void testContext() {
		// null context
		XTrace.setThreadContext(null);
		assertNull(XTrace.getThreadContext());
		
		// invalid context
		XTrace.setThreadContext(new XTraceMetadata());
		assertNull(XTrace.getThreadContext());
		
		// valid context 1
		TaskID task = new TaskID(4);
		XTrace.setThreadContext(new XTraceMetadata(task, 1234));
		assertNotNull(XTrace.getThreadContext());
		assertEquals(new XTraceMetadata(task, 1234), XTrace.getThreadContext());
		
		// valid context 2
		TaskID task2 = new TaskID(20);
		XTrace.setThreadContext(new XTraceMetadata(task2, (long) 98769876));
		assertNotNull(XTrace.getThreadContext());
		assertEquals(new XTraceMetadata(task2, (long) 98769876), XTrace.getThreadContext());
	}

	@Test
	public void testClearContext() {
		XTrace.setThreadContext(new XTraceMetadata(new TaskID(12), 1234));
		assertNotNull(XTrace.getThreadContext());
		XTrace.clearThreadContext();
		assertNull(XTrace.getThreadContext());
	}

	@Test
	public void testLogEvent() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public void testCreateEvent() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public void testIsContextValid() {
		XTrace.clearThreadContext();
		assertFalse(XTrace.isContextValid());
		XTrace.setThreadContext(new XTraceMetadata(new TaskID(4), 1234));
		assertTrue(XTrace.isContextValid());
	}

	@Test
	public void testStartProcess() {
		//fail("Not yet implemented"); // TODO
	}

}
