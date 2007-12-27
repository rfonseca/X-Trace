package edu.berkeley.xtrace;

import static org.junit.Assert.*;

import org.junit.Test;

public class EventContextTest {

	@Test
	public void testContext() {
		// null context
		EventContext.setContext(null);
		assertNull(EventContext.getContext());
		
		// invalid context
		EventContext.setContext(new Metadata());
		assertNull(EventContext.getContext());
		
		// valid context 1
		TaskID task = new TaskID(4);
		EventContext.setContext(new Metadata(task, 1234));
		assertNotNull(EventContext.getContext());
		assertEquals(new Metadata(task, 1234), EventContext.getContext());
		
		// valid context 2
		TaskID task2 = new TaskID(20);
		EventContext.setContext(new Metadata(task2, (long) 98769876));
		assertNotNull(EventContext.getContext());
		assertEquals(new Metadata(task2, (long) 98769876), EventContext.getContext());
	}

	@Test
	public void testClearContext() {
		EventContext.setContext(new Metadata(new TaskID(12), 1234));
		assertNotNull(EventContext.getContext());
		EventContext.clearContext();
		assertNull(EventContext.getContext());
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
		EventContext.clearContext();
		assertFalse(EventContext.isValid());
		EventContext.setContext(new Metadata(new TaskID(4), 1234));
		assertTrue(EventContext.isValid());
	}

	@Test
	public void testStartProcess() {
		//fail("Not yet implemented"); // TODO
	}

}
