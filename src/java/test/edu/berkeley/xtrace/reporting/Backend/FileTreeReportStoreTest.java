package edu.berkeley.xtrace.reporting.Backend;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.berkeley.xtrace.Metadata;
import edu.berkeley.xtrace.TaskID;
import edu.berkeley.xtrace.XtraceException;
import edu.berkeley.xtrace.reporting.Report;

public class FileTreeReportStoreTest {
	private static final Logger LOG = Logger.getLogger(FileTreeReportStoreTest.class);
	private static final int NUM_STOCHASTIC_TASKS = 100;
	private static final int NUM_STOCHASTIC_REPORTS_PER_TASK = 10;
	
	private boolean canTest = true;
	private File testDirectory;
	private FileTreeReportStore fs;
	private Random r;

	@Before
	public void setUp() throws Exception {
		r = new Random();
		
		/* Create the temporary test directory */
		File tmpdir = new File("/tmp");
		if (!tmpdir.exists() || !tmpdir.canWrite()) {
			LOG.fatal("Unable to open /tmp directory.  Cannot execute these tests");
			canTest  = false;
			return;
		}
		testDirectory = new File(tmpdir, "xtrace-reportstore");
		testDirectory.mkdir();
		
		/* Create and initialize the file tree report store */
		System.setProperty("xtrace.backend.storedirectory", testDirectory.toString());
		fs = new FileTreeReportStore();
		fs.initialize();
	}
	
	@Test
	public void testSimpleInsertion() throws XtraceException {
		if (!canTest) return;
		
		long startTime = System.currentTimeMillis();
		
		/* Insert a single report into the file store */
		Metadata md = new Metadata(new TaskID(8), 0);
		Report report = randomReport(md);
		fs.receiveReport(report.toString());
		
		/* Sync() the report */
		fs.sync();
		
		/* Test the interface */
		assertEquals(1, fs.numReports());
		assertEquals(1, fs.numUniqueTasks());
		
		/* test getByTask and countByTaskID */
		assertEquals(1, fs.countByTaskId(md.getTaskId().toString()));
		Iterator<String> iter = fs.getReportsByTask(md.getTaskId().toString());
		assertTrue(iter.hasNext());
		String report2 = iter.next();
		assertNotNull(report2);
		assertEquals(report.toString(), report2);
		assertFalse(iter.hasNext());
		
		/* test getTasksSince */
		iter = fs.getTasksSince(startTime);
		assertTrue(iter.hasNext());
		assertEquals(md.getTaskId().toString(), iter.next());
		assertFalse(iter.hasNext());
	}
	
	@Test
	public void testMultipleInsertion() throws XtraceException {
		if (!canTest) return;
		
		final int TOTAL_REPORTS = NUM_STOCHASTIC_TASKS * NUM_STOCHASTIC_REPORTS_PER_TASK;
		
		long startTime = System.currentTimeMillis();
		
		/* Create a set of tasks */
		TaskID[] taskIds = new TaskID[NUM_STOCHASTIC_TASKS];
		String[] taskStrs = new String[NUM_STOCHASTIC_TASKS];
		for (int i = 0 ; i < taskIds.length; i++) {
			taskIds[i] = new TaskID(8);
			taskStrs[i] = taskIds[i].toString();
		}
		
		/* Create a set of reports */
		Metadata[] mds = new Metadata[TOTAL_REPORTS];
		Report[] reports = new Report[TOTAL_REPORTS];
		for (int i = 0; i < taskIds.length; i++) {
			for (int j = 0; j < NUM_STOCHASTIC_REPORTS_PER_TASK; j++) {
				int idx = i*NUM_STOCHASTIC_REPORTS_PER_TASK + j;
				mds[idx] = new Metadata(taskIds[i], r.nextInt());
				reports[idx] = randomReport(mds[idx]);
				fs.receiveReport(reports[idx].toString());
			}
		}
		
		/* Sync() the report */
		fs.sync();
		
		/* Test numReports() and numUniqueTasks() */
		assertEquals(TOTAL_REPORTS, fs.numReports());
		assertEquals(NUM_STOCHASTIC_TASKS, fs.numUniqueTasks());
		
		/* test countByTaskID() */
		for (int i = 0; i < taskIds.length; i++) {
			assertEquals(NUM_STOCHASTIC_REPORTS_PER_TASK, fs.countByTaskId(taskIds[i].toString()));
		}
		
		/* test getTasksSince() */
		List<String> tasklst = new ArrayList<String>(NUM_STOCHASTIC_TASKS);
		Iterator<String> taskiter = fs.getTasksSince(startTime);
		while (taskiter.hasNext()) {
			tasklst.add(taskiter.next());
		}
		String[] stringsSince = tasklst.toArray(new String[0]);
		Arrays.sort(taskStrs);
		Arrays.sort(stringsSince);
		assertTrue(Arrays.deepEquals(taskStrs, stringsSince));
		
		/* getReportsByTask() simple test: just check to
		 * make sure that the right number of reports are there
		 */
		for (int i = 0; i < NUM_STOCHASTIC_TASKS; i++) {
			Iterator<String> iter = fs.getReportsByTask(taskStrs[i]);
			assertTrue(iter.hasNext());
			int j = 0;
			while (iter.hasNext()) {
				iter.next();
				j++;
			}
			assertEquals(NUM_STOCHASTIC_REPORTS_PER_TASK, j);
		}
	}

	@Test
	public void testLastUpdatedByTaskID() {
		if (!canTest) return;
		
		/* Test the null case */
		fs.sync();
		assertEquals(0L, fs.lastUpdatedByTaskId("00000000"));
		
		/* Test if an insertion updates the time */
		long startTime = System.currentTimeMillis();
		
		Metadata md = new Metadata(new TaskID(8), 0);
		Report report = randomReport(md);
		fs.receiveReport(report.toString());
		
		/* Sync() the report */
		fs.sync();
		
		long afterFirstInsertion = fs.lastUpdatedByTaskId(md.getTaskId().toString());
		assertTrue(afterFirstInsertion > startTime);
		
		md = new Metadata(new TaskID(8), 0);
		report = randomReport(md);
		fs.receiveReport(report.toString());
		
		/* Sync() the report */
		fs.sync();
		
		long afterSecondInsertion = fs.lastUpdatedByTaskId(md.getTaskId().toString());
		assertTrue(afterFirstInsertion < afterSecondInsertion);
	}
	
	@Test
	public void getLatestTasks() {
		if (!canTest) return;
		
		/* Create a set of reports */
		Report[] reports = new Report[10];
		for (int i = 0; i < reports.length; i++) {
			reports[i] = randomReport(new Metadata(new TaskID(8), r.nextInt()));
			fs.receiveReport(reports[i].toString());
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) { }
		}
		
		fs.sync();
		
		/* Test '1' case */
		Iterator<TaskID> iter = fs.getLatestTasks(1);
		assertTrue(iter.hasNext());
		assertEquals(reports[9].getMetadata().getTaskId(), iter.next());
		assertFalse(iter.hasNext());
		
		/* Test '2' case */
		iter = fs.getLatestTasks(2);
		assertTrue(iter.hasNext());
		assertEquals(reports[9].getMetadata().getTaskId(), iter.next());
		assertTrue(iter.hasNext());
		assertEquals(reports[8].getMetadata().getTaskId(), iter.next());
		assertFalse(iter.hasNext());
	}
	
	@After
	public void tearDown() throws Exception {
		fs.shutdown();
		deleteDir(testDirectory);
	}
	
	// From the Java Almanac: http://www.exampledepot.com/egs/java.io/DeleteDir.html
	// Deletes all files and subdirectories under dir.
    // Returns true if all deletions were successful.
    // If a deletion fails, the method stops attempting to delete and returns false.
    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
    
        // The directory is now empty so delete it
        return dir.delete();
    }
    
    private Report randomReport(Metadata md) {
		Report report = new Report();
		report.put("X-Trace", md.toString());
		
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
