package edu.berkeley.xtrace;

import static org.junit.Assert.*;

import org.junit.Test;

public class XTraceTest {

	@Test
	public void testContext() {
		// null context
		XTraceContext.setThreadContext(null);
		assertNull(XTraceContext.getThreadContext());
		
		// invalid context
		XTraceContext.setThreadContext(new XTraceMetadata());
		assertNull(XTraceContext.getThreadContext());
		
		// valid context 1
		TaskID task = new TaskID(4);
		XTraceContext.setThreadContext(new XTraceMetadata(task, 1234));
		assertNotNull(XTraceContext.getThreadContext());
		assertEquals(new XTraceMetadata(task, 1234), XTraceContext.getThreadContext());
		
		// valid context 2
		TaskID task2 = new TaskID(20);
		XTraceContext.setThreadContext(new XTraceMetadata(task2, (long) 98769876));
		assertNotNull(XTraceContext.getThreadContext());
		assertEquals(new XTraceMetadata(task2, (long) 98769876), XTraceContext.getThreadContext());
	}

	@Test
	public void testClearContext() {
		XTraceContext.setThreadContext(new XTraceMetadata(new TaskID(12), 1234));
		assertNotNull(XTraceContext.getThreadContext());
		XTraceContext.clearThreadContext();
		assertNull(XTraceContext.getThreadContext());
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
		XTraceContext.clearThreadContext();
		assertFalse(XTraceContext.isValid());
		XTraceContext.setThreadContext(new XTraceMetadata(new TaskID(4), 1234));
		assertTrue(XTraceContext.isValid());
	}

	@Test
	public void testStartProcess() {
		//fail("Not yet implemented"); // TODO
	}

}
