package org.flasck.flas.testing.golden;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.Configuration;
import org.zinutils.system.RunProcess;
import org.zinutils.utils.Crypto;
import org.zinutils.utils.FileUtils;

public class TestEnvironment {

	private File jsout;
	private File tc2;
	private File jvmout;
	public final File testReportTo;
	private File errors;
	private boolean useJSRunner;
	private boolean useJVMRunner;
	private File rootdir;
	private File testReports;
	private boolean checkNothing;
	private boolean checkEverything;
	private File goldtc;
	private File goldfd;
	private File tmpfd;

	public TestEnvironment(File jvmdir, String root, boolean useJSRunner, boolean useJVMRunner, boolean checkNothing, boolean checkEverything) throws FileNotFoundException, IOException {
		this.rootdir = new File(root, "test.golden");
		this.useJSRunner = useJSRunner;
		this.useJVMRunner = useJVMRunner;
		this.checkNothing = checkNothing;
		this.checkEverything = checkEverything;
		jsout = new File(root, "jsout");
		jvmout = new File(root, "jvmout");
		goldfd = new File(root, "flimstore");
		tmpfd = new File(root, "flimstore-tmp");
		goldtc = new File(root, "tc");
		tc2 = new File(root, "tc-tmp");
		testReports = new File(root, "testReports");
		testReportTo = new File(root, "testReports-tmp");
		errors = new File(root, "errors-tmp");
	}

	public boolean haveTests() {
		return rootdir.isDirectory() && 
			(!FileUtils.findFilesMatching(rootdir, "*.ut").isEmpty() ||
			 !FileUtils.findFilesMatching(rootdir, "*.st").isEmpty() ||
			 !FileUtils.findFilesMatching(rootdir, "*.pt").isEmpty());
	}

	public void cleanUp() {
		FileUtils.deleteDirectoryTree(errors);
		clean(jsout);
		clean(jvmout);
		clean(tc2);
	}
	
	public static void clean(File dir) {
		FileUtils.cleanDirectory(dir);
		FileUtils.assertDirectory(dir);
	}

	public void checkTestResults(boolean errorsExpected) {
		if (haveTests() && (useJSRunner || useJVMRunner)) {
			FileUtils.assertDirectory(testReports);
			for (String f : testReportTo.list()) {
				FileUtils.assertFile(new File(testReports, f));
			}
			assertGolden(errorsExpected, testReports, testReportTo, false, false);
		}
	}
	
	public void assertGolden(boolean errorsExpected, File golden, File genned, boolean allowMissingGolden, boolean allowWSDiffs) {
		if (checkNothing)
			return;
		if (!golden.isDirectory()) {
			if (!checkEverything || errorsExpected)
				return;
			fail("There is no golden directory " + golden);
		}
		if (!genned.isDirectory()) {
			if (!checkEverything || errorsExpected)
				return;
			fail("There is no generated directory " + genned);
		}
		List<File> missing = new ArrayList<>();
		for (File f : genned.listFiles()) {
			if (!new File(golden, f.getName()).exists() && !allowMissingGolden) {
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
			if (!gen.exists() && errorsExpected)
				continue;
			assertTrue("There is no generated file for the golden " + f, gen.exists());
			String goldhash = Crypto.hashTrim(f);
			String genhash = Crypto.hashTrim(gen);
			if (!goldhash.equals(genhash)) {
				RunProcess proc = new RunProcess("diff");
				proc.arg("-C5");
				if (allowWSDiffs)
					proc.arg("-b");
				proc.arg(f.getPath());
				proc.arg(gen.getPath());
				proc.redirectStdout(System.out);
				proc.redirectStderr(System.err);
				proc.execute();
				int code = proc.getExitCode();
				if (code == 0)
					continue;
			}
			assertEquals("Files " + f + " and " + gen + " differed", goldhash, genhash);
		}
	}

	public void checkFlimStore(boolean errorsExpected) throws IOException {
		if (!goldfd.exists())
			return;
		if (!tmpfd.exists())
			fail("flim store was not created");
		assertGolden(errorsExpected, goldfd, tmpfd, true, false);
	}

	public void checkTypes(boolean expectingErrors) throws IOException {
		if (!new Configuration(null, new String[0]).doTypeCheck)
			return;
		File goldtf = new File(goldtc, "types");
		File gentf = new File(tc2, "types");
		if (expectingErrors && (!goldtf.exists() || !gentf.exists()))
			return;
		FileUtils.assertDirectory(goldtc);
		FileUtils.assertFile(goldtf);
		assertGolden(expectingErrors, goldtc, tc2, false, false);
	}

	public void checkReconstructions(File sources, File reconstructions) {
		assertGolden(false, sources, reconstructions, true, true);
	}
}
