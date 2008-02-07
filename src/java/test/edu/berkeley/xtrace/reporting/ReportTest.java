package edu.berkeley.xtrace.reporting;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import edu.berkeley.xtrace.XTraceMetadata;
import edu.berkeley.xtrace.TaskID;

public class ReportTest {

	@Test
	public void testReport() {
		Report r = new Report();
		assertNotNull(r.toString());
		assertEquals("X-Trace Report ver 1.0\n", r.toString());
	}
	
	@Test
	public void testReport2() {
		Report r = new Report();
		r.put("Key1", "Value1");
		r.put("Key2", "Value2");

		List<String> kset = r.get("Key1");
		assertEquals(kset.size(), 1);
		assertTrue(kset.contains("Value1"));

		kset = r.get("Key2");
		assertEquals(kset.size(), 1);
		assertTrue(kset.contains("Value2"));

		kset = r.get("Key3");
		assertNull(kset);

		r.put("Key4", "Value4.1");
		r.put("Key4", "Value4.2");
		r.put("Key4", "Value4.3");

		kset = r.get("Key4");
		assertNotNull(kset);
		assertEquals(kset.size(), 3);
		assertTrue(kset.contains("Value4.1"));
		assertTrue(kset.contains("Value4.2"));
		assertTrue(kset.contains("Value4.3"));
	}

	@Test
	public void testRemove() {
		Report r = new Report();
		r.put("Key1", "Value1");
		r.put("Key2", "Value2");

		List<String> kset = r.get("Key1");
		assertEquals(kset.size(), 1);
		assertTrue(kset.contains("Value1"));
		
		r.remove("Key1");
		kset = r.get("Key1");
		assertNull(kset);

		r.remove("Key2");
		kset = r.get("Key2");
		assertNull(kset);

		r.put("Key4", "Value4.1");
		r.put("Key4", "Value4.2");
		r.put("Key4", "Value4.3");

		kset = r.get("Key4");
		assertNotNull(kset);
		assertEquals(kset.size(), 3);
		assertTrue(kset.contains("Value4.1"));
		assertTrue(kset.contains("Value4.2"));
		assertTrue(kset.contains("Value4.3"));
		
		r.remove("Key4");
		kset = r.get("Key4");
		assertNull(kset);
	}

	@Test
	public void testGetMetadata() {
		TaskID task = new TaskID(8);
		XTraceMetadata md = new XTraceMetadata(task, 0);
		
		Report r = new Report();
		r.put("X-Trace", md.toString());
		r.put("Key1", "Value1");
		r.put("Key2", "Value2");
		
		assertEquals(md, r.getMetadata());
		
		r = new Report();
		r.put("Key1", "Value1");
		r.put("X-Trace", md.toString());
		r.put("Key2", "Value2");
		
		assertEquals(md, r.getMetadata());
	}

	@Test
	public void testToString() {
		Report r = new Report();
		XTraceMetadata xtr = new XTraceMetadata();
		r.put("X-Trace", xtr.toString());
		r.put("Key1", "Value1");
		r.put("Key2", "Value2");
		r.put("Key4", "Value4.1");
		r.put("Key4", "Value4.2");
		r.put("Key4", "Value4.3");

		String s = r.toString();

		assertNotNull(s);
		assertTrue(s.startsWith("X-Trace Report ver 1.0"));
		assertTrue(s.contains("Key1: Value1"));
		assertTrue(s.contains("Key2: Value2"));
		assertTrue(s.contains("Key4: Value4.1"));
		assertTrue(s.contains("Key4: Value4.2"));
		assertTrue(s.contains("Key4: Value4.3"));
		assertTrue(s.contains("X-Trace: 100000000000000000"));
	}

	@Test
	public void testCreateFromString() {
		Report r = new Report();
		r.put("Key1", "Value1");
		r.put("Key2", "Value2");
		r.put("Key4", "Value4.1");
		r.put("Key4", "Value4.2");
		r.put("Key4", "Value4.3");
		r.put("X-Trace", "100000000000000000");
		
		Report r2 = Report.createFromString(r.toString());
		String s = r2.toString();

		assertNotNull(s);
		assertTrue(s.startsWith("X-Trace Report ver 1.0"));
		assertTrue(s.contains("Key1: Value1"));
		assertTrue(s.contains("Key2: Value2"));
		assertTrue(s.contains("Key4: Value4.1"));
		assertTrue(s.contains("Key4: Value4.2"));
		assertTrue(s.contains("Key4: Value4.3"));
		assertTrue(s.contains("X-Trace: 100000000000000000"));
		
        r = Report.createFromString("X-Trace Report ver 1.0\n" +
        		                    "X-Trace: 0000000000\n" +
        		                    "Key1: Value1\n" +
        		                    "Key2: Value2\n" +
        		                    "Key4: Value4.1\n" +
        		                    "Key4: Value4.2\n" +
        		                    "Key4: Value4.3");
		
		List<String> kset = r.get("Key1");
        assertEquals(kset.size(), 1);
        assertTrue(kset.contains("Value1"));
        
        kset = r.get("Key2");
        assertEquals(kset.size(), 1);
        assertTrue(kset.contains("Value2"));
        
        kset = r.get("Key3");
        assertNull(kset);
        
        kset = r.get("Key4");
        assertEquals(kset.size(), 3);
        assertTrue(kset.contains("Value4.1"));
        assertTrue(kset.contains("Value4.2"));
        assertTrue(kset.contains("Value4.3"));
	}

}
