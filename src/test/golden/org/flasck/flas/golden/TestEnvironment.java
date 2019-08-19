package org.flasck.flas.golden;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.zinutils.system.RunProcess;
import org.zinutils.utils.Crypto;
import org.zinutils.utils.FileUtils;

public class TestEnvironment {

	private File jsto;
	private File tc2;
	private File droidTo;
	public final File testReportTo;
	private File errors;
	private boolean useJSRunner;
	private boolean useJVMRunner;
	private File jvmbin;
	private File rootdir;
	private File testReports;
	private boolean checkNothing;
	private boolean checkEverything;
	private File goldtc;

	public TestEnvironment(File jvmdir, String root, boolean useJSRunner, boolean useJVMRunner, boolean checkNothing, boolean checkEverything) throws FileNotFoundException, IOException {
		this.rootdir = new File(root, "test.golden");
		this.useJSRunner = useJSRunner;
		this.useJVMRunner = useJVMRunner;
		this.checkNothing = checkNothing;
		this.checkEverything = checkEverything;
		jsto = new File(root, "jsout-tmp");
		goldtc = new File(root, "tc");
		tc2 = new File(root, "tc-tmp");
		droidTo = new File(root, "droid-to");
		testReports = new File(root, "testReports");
		testReportTo = new File(root, "testReports-tmp");
		errors = new File(root, "errors-tmp");

		jvmbin = new File(jvmdir, "jvm/bin");
		if (!jvmbin.exists())
			jvmbin = new File(jvmdir, "jvm/qbout");
		if (!jvmbin.exists())
			throw new RuntimeException("No jvm bin directory could be found");

	}

	public boolean haveTests() {
		return !FileUtils.findFilesMatching(rootdir, "*.ut").isEmpty();
	}

	public void cleanUp() {
		FileUtils.deleteDirectoryTree(errors);
		clean(jsto);
		clean(droidTo);
		clean(tc2);
	}
	
	public static void clean(File dir) {
		FileUtils.cleanDirectory(dir);
		FileUtils.assertDirectory(dir);
	}

	public void checkTestResults() {
		if (haveTests() && (useJSRunner || useJVMRunner)) {
			FileUtils.assertDirectory(testReports);
			assertGolden(testReports, testReportTo);
		}
	}
	
	public void assertGolden(File golden, File genned) {
		if (checkNothing)
			return;
		if (!golden.isDirectory()) {
			if (!checkEverything)
				return;
			fail("There is no golden directory " + golden);
		}
		if (!genned.isDirectory()) {
			if (!checkEverything)
				return;
			fail("There is no generated directory " + genned);
		}
		List<File> missing = new ArrayList<>();
		for (File f : genned.listFiles()) {
			if (!new File(golden, f.getName()).exists()) {
				System.out.println("--- missing " + f);
				FileUtils.cat(f);
				System.out.println("---");
				missing.add(f);
			}
		}
		if (!missing.isEmpty())
			fail("There is no golden file for the generated " + missing);
		for (File f : golden.listFiles()) {
			File gen = new File(genned, f.getName());
			assertTrue("There is no generated file for the golden " + f, gen.exists());
			String goldhash = Crypto.hashTrim(f);
			String genhash = Crypto.hashTrim(gen);
			if (!goldhash.equals(genhash)) {
				RunProcess proc = new RunProcess("diff");
				proc.arg("-C5");
				proc.arg(f.getPath());
				proc.arg(gen.getPath());
				proc.redirectStdout(System.out);
				proc.redirectStderr(System.err);
				proc.execute();
				proc.getExitCode();
			}
			assertEquals("Files " + f + " and " + gen + " differed", goldhash, genhash);
		}
	}

	public void checkTypes() throws IOException {
		assertGolden(goldtc, tc2);
	}
}
