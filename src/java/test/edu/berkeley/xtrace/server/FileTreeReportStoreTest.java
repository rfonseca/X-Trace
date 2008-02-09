package edu.berkeley.xtrace.server;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.berkeley.xtrace.XTraceMetadata;
import edu.berkeley.xtrace.TaskID;
import edu.berkeley.xtrace.XTraceException;
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
		BasicConfigurator.configure(new NullAppender());
		
		r = new Random();
		
		/* Create the temporary test directory */
		File tmpdir = new File("/tmp");
		if (!tmpdir.exists() || !tmpdir.canWrite()) {
			LOG.fatal("Unable to open /tmp directory.  Cannot execute these tests");
			canTest  = false;
			return;
		}
		
		testDirectory = new File(tmpdir, "xtrace-reportstore");
		deleteDir(testDirectory);
		testDirectory.mkdir();
		
		/* Create and initialize the file tree report store */
		System.setProperty("xtrace.server.storedirectory", testDirectory.toString());
		fs = new FileTreeReportStore();
		fs.initialize();
	}
	
	@Test
	public void testSimpleInsertion() throws XTraceException {
		if (!canTest) return;
		
		long startTime = System.currentTimeMillis();
		
		/* Insert a single report into the file store */
		XTraceMetadata md = new XTraceMetadata(new TaskID(8), 0);
		Report report = randomReport(md);
		fs.receiveReport(report.toString());
		
		/* Sync() the report */
		fs.sync();
		
		/* Test the interface */
		assertEquals(1, fs.numReports());
		assertEquals(1, fs.numTasks());
		
		/* test getByTask and countByTaskID */
		assertEquals(1, fs.countByTaskId(md.getTaskId()));
		Iterator<Report> iter = fs.getReportsByTask(md.getTaskId());
		assertTrue(iter.hasNext());
		Report report2 = iter.next();
		assertNotNull(report2);
		assertEquals(report, report2);
		assertFalse(iter.hasNext());
		
		/* test getTasksSince */
		Iterator<TaskRecord> taskiter = fs.getTasksSince(startTime).iterator();
		assertTrue(taskiter.hasNext());
		assertEquals(md.getTaskId(), taskiter.next().getTaskId());
		assertFalse(taskiter.hasNext());
	}
	
	@Test
	public void testMultipleInsertion() throws XTraceException {
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
		XTraceMetadata[] mds = new XTraceMetadata[TOTAL_REPORTS];
		Report[] reports = new Report[TOTAL_REPORTS];
		for (int i = 0; i < taskIds.length; i++) {
			for (int j = 0; j < NUM_STOCHASTIC_REPORTS_PER_TASK; j++) {
				int idx = i*NUM_STOCHASTIC_REPORTS_PER_TASK + j;
				mds[idx] = new XTraceMetadata(taskIds[i], r.nextInt());
				reports[idx] = randomReport(mds[idx]);
				fs.receiveReport(reports[idx].toString());
			}
		}
		
		/* Sync() the report */
		fs.sync();
		
		/* Test numReports() and numUniqueTasks() */
		assertEquals(TOTAL_REPORTS, fs.numReports());
		assertEquals(NUM_STOCHASTIC_TASKS, fs.numTasks());
		
		/* test countByTaskID() */
		for (int i = 0; i < taskIds.length; i++) {
			assertEquals(NUM_STOCHASTIC_REPORTS_PER_TASK, fs.countByTaskId(taskIds[i]));
		}
		
		/* test getTasksSince() */
		List<String> tasklst = new ArrayList<String>(NUM_STOCHASTIC_TASKS);
		Iterator<TaskRecord> taskiter = fs.getTasksSince(startTime).iterator();
		while (taskiter.hasNext()) {
			tasklst.add(taskiter.next().getTaskId().toString());
		}
		String[] stringsSince = tasklst.toArray(new String[0]);
		Arrays.sort(taskStrs);
		Arrays.sort(stringsSince);
		assertTrue(Arrays.deepEquals(taskStrs, stringsSince));
		
		/* getReportsByTask() simple test: just check to
		 * make sure that the right number of reports are there
		 */
		for (int i = 0; i < NUM_STOCHASTIC_TASKS; i++) {
			Iterator<Report> iter = fs.getReportsByTask(taskIds[i]);
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
	public void testDuplicateTags() throws XTraceException {
		if (!canTest) return;
		
		/* Insert a single report into the file store */
		TaskID taskId = new TaskID(8);
		Report report = randomReport(new XTraceMetadata(taskId, 0));
		report.put("Tag", "tag1");
		report.put("Tag", "tag1");
		fs.receiveReport(report.toString());
		fs.sync();
		
		/* Test that a single copy of the tag is seen */
		assertEquals(1, fs.getTasksByTag("tag1").size());
		List<String> tags = fs.getTasksByTag("tag1").get(0).getTags();
		assertEquals(1, tags.size());
		assertEquals("tag1", tags.get(0));
		assertEquals(0, fs.getTasksByTag("tag1,tag1").size());
		// Substring of "tag1" should not return any tasks
		assertEquals(0, fs.getTasksByTag("ta").size());
		
		/* Insert another report in the same task */
		report = randomReport(new XTraceMetadata(taskId, 1));
		report.put("Tag", "tag1");
		report.put("Tag", "tag2");
		fs.receiveReport(report.toString());
		fs.sync();
		
		/* Test that a single task is returned for both tags */
		assertEquals(1, fs.getTasksByTag("tag1").size());
		assertEquals(1, fs.getTasksByTag("tag2").size());
		tags = fs.getTasksByTag("tag1").get(0).getTags();
		assertEquals(2, tags.size());
		assertEquals(0, fs.getTasksByTag("tag1,tag1").size());
		// Substring of "tag1" should not return any tasks
		assertEquals(0, fs.getTasksByTag("ta").size());
	}
	
	@Test
	public void testTitleOperations() throws XTraceException {
		if (!canTest) return;
		
		/* Insert a single report into the file store */
		TaskID taskId = new TaskID(8);
		Report report = randomReport(new XTraceMetadata(taskId, 0));
		fs.receiveReport(report.toString());
		fs.sync();
		
		/* Test that the title is the taskID (since no title field was given) */
		assertEquals(taskId.toString(), fs.getLatestTasks(1).get(0).getTitle());
		assertEquals(1, fs.getTasksByTitle(taskId.toString()).size());
		
		/* Insert another report in the same task, with no title */
		report = randomReport(new XTraceMetadata(taskId, 1));
		fs.receiveReport(report.toString());
		fs.sync();
		
		/* Test that the title is the taskID (since no title field was given) */
		assertEquals(taskId.toString(), fs.getLatestTasks(1).get(0).getTitle());
		assertEquals(1, fs.getTasksByTitle(taskId.toString()).size());
		
		/* Insert another report in the same task, with a title */
		report = randomReport(new XTraceMetadata(taskId, 2));
		report.put("Title", "title1");
		fs.receiveReport(report.toString());
		fs.sync();
		
		/* Test that the title is title1 */
		assertEquals("title1", fs.getLatestTasks(1).get(0).getTitle());
		assertEquals(0, fs.getTasksByTitle(taskId.toString()).size());
		assertEquals(1, fs.getTasksByTitle("title1").size());
		
		/* Insert another report in the same task, with another title */
		report = randomReport(new XTraceMetadata(taskId, 3));
		report.put("Title", "title2");
		fs.receiveReport(report.toString());
		fs.sync();
		
		/* Test that the title is title1 */
		assertEquals("title2", fs.getLatestTasks(1).get(0).getTitle());
		assertEquals(0, fs.getTasksByTitle(taskId.toString()).size());
		assertEquals(0, fs.getTasksByTitle("title1").size());
		assertEquals(1, fs.getTasksByTitle("title2").size());
		assertEquals(1, fs.getTasksByTitleSubstring("title2").size());
		assertEquals(1, fs.getTasksByTitleSubstring("itle").size());
		assertEquals(1, fs.getTasksByTitleSubstring("t").size());
		assertEquals(0, fs.getTasksByTitleSubstring("title1").size());
	}
	
	@Test
	public void testLastUpdatedByTaskID() {
		if (!canTest) return;
		
		/* Test the null case */
		fs.sync();
		assertEquals(0L, fs.lastUpdatedByTaskId(TaskID.createFromString("00000000")));
		
		/* Test if an insertion updates the time */
		long startTime = System.currentTimeMillis();
		
		XTraceMetadata md = new XTraceMetadata(new TaskID(8), 0);
		Report report = randomReport(md);
		fs.receiveReport(report.toString());
		
		/* Sync() the report */
		fs.sync();
		
		long afterFirstInsertion = fs.lastUpdatedByTaskId(md.getTaskId());
		assertTrue(afterFirstInsertion > startTime);
		
		md = new XTraceMetadata(new TaskID(8), 0);
		report = randomReport(md);
		fs.receiveReport(report.toString());
		
		/* Sync() the report */
		fs.sync();
		
		long afterSecondInsertion = fs.lastUpdatedByTaskId(md.getTaskId());
		assertTrue(afterFirstInsertion < afterSecondInsertion);
	}
	
	@Test
	public void getLatestTasks() {
		if (!canTest) return;
		
		/* Create a set of reports */
		Report[] reports = new Report[10];
		for (int i = 0; i < reports.length; i++) {
			reports[i] = randomReport(new XTraceMetadata(new TaskID(8), r.nextInt()));
			fs.receiveReport(reports[i].toString());
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) { }
		}
		
		fs.sync();
		
		/* Test '1' case */
		List<TaskRecord> tasks = fs.getLatestTasks(1);
		assertEquals(1, tasks.size());
		assertEquals(reports[9].getMetadata().getTaskId(), tasks.get(0).getTaskId());
		
		/* Test '2' case */
		tasks = fs.getLatestTasks(2);
		assertEquals(2, tasks.size());
		assertEquals(reports[9].getMetadata().getTaskId(), tasks.get(0).getTaskId());
		assertEquals(reports[8].getMetadata().getTaskId(), tasks.get(1).getTaskId());
	}
	
	@Test
	public void getTasksByTag() {
		if (!canTest) return;
		
		/* Create a set of reports */
		Report[] reports = new Report[10];
		for (int i = 0; i < reports.length; i++) {
			reports[i] = randomReport(new XTraceMetadata(new TaskID(8), r.nextInt()));
		}
		reports[2].put("Tag", "tag1");
		reports[4].put("Tag", "tag2");
		reports[5].put("Tag", "tag2");
		reports[6].put("Tag", "tag3");
		reports[6].put("Tag", "tag4");
		
		/* Send them to the report store */
		for (int i = 0; i < reports.length; i++) {
			fs.receiveReport(reports[i].toString());
		}
		fs.sync();
		
		/* Test null case */
		List<TaskRecord> nulllst = fs.getTasksByTag("foobar");
		assertNotNull(nulllst);
		assertEquals(0, nulllst.size());
		
		/* tag in exactly 1 report */
		List<TaskRecord> lst = fs.getTasksByTag("tag1");
		assertNotNull(lst);
		assertEquals(1, lst.size());
		assertEquals(reports[2].getMetadata().getTaskId(), lst.get(0).getTaskId());
		
		/* tag in two different reports */
		TaskID report4 = reports[4].getMetadata().getTaskId();
		TaskID report5 = reports[5].getMetadata().getTaskId();
		lst = fs.getTasksByTag("tag2");
		assertNotNull(lst);
		assertEquals(2, lst.size());
		assertTrue(  (report4.equals(lst.get(0).getTaskId()) 
				            && report5.equals(lst.get(1).getTaskId()))  ||
				         (report5.equals(lst.get(0).getTaskId()) 
				        		&& report4.equals(lst.get(1).getTaskId()))   );
		
		/* two tags in the same report */
		TaskID report6 = reports[6].getMetadata().getTaskId();
		lst = fs.getTasksByTag("tag3");
		assertNotNull(lst);
		assertEquals(1, lst.size());
		assertEquals(report6, lst.get(0).getTaskId());
		lst = fs.getTasksByTag("tag4");
		assertNotNull(lst);
		assertEquals(1, lst.size());
		assertEquals(report6, lst.get(0).getTaskId());
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
    
    private Report randomReport(XTraceMetadata md) {
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
