package edu.berkeley.xtrace.reporting.Backend;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.berkeley.xtrace.reporting.Report;

public class FileTreeIteratorTest {
	private static final Logger LOG = Logger.getLogger(FileTreeIteratorTest.class);

	private final static int NUM_STOCHASTIC = 1000;
	private Report[] reports;
	private File reportFile;
	private Random r;

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		r = new Random();
		
		reports = new Report[NUM_STOCHASTIC];
		for (int i = 0; i < reports.length; i++) {
			reports[i] = randomReport();
		}
		
		
		reportFile = File.createTempFile("xtrace", null);
		LOG.info("Creating temporary file: " + reportFile);
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(reportFile)));
		
		for (int i = 0; i < reports.length; i++) {
			out.println(reports[i].toString());
		}
		out.flush();
		out.close();
	}

	@After
	public void tearDown() throws Exception {
		LOG.info("Deleting temporary file");
		reportFile.delete();
	}

	@Test
	public void testFileTreeIterator() {
		FileTreeReportStore.FileTreeIterator iter =
			new FileTreeReportStore.FileTreeIterator(reportFile);
		
		for (int i = 0; i < reports.length; i++) {
			assertTrue(iter.hasNext());
			assertEquals(reports[i].toString(), iter.next());
		}
		
		assertFalse(iter.hasNext());
	}
	
	private Report randomReport() {
		Report report = new Report();
		
		final int numKeys = r.nextInt(15);
		for (int i = 0; i < numKeys; i++) {
			report.put("Key"+i, randomString(10 + r.nextInt(20)));
		}
		return report;
	}
	
	private String randomString(int length) {
		char[] ar = new char[length];
		
		for (int i = 0; i < length; i++) {
			ar[i] = (char)((int)'a' + r.nextInt(25));
		}

		return new String(ar);
	}
}
