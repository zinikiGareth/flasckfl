package org.flasck.flas.golden;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.flasck.flas.Main;
import org.flasck.flas.compiler.FLASCompiler;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.cgharness.CGHarnessTestBase;
import org.zinutils.utils.FileUtils;

@Ignore
public class GoldenJVM {
	static Logger logger = LoggerFactory.getLogger("HSIE");
	static String checkEverythingS = System.getProperty("org.flasck.golden.check");
	static boolean checkEverything = checkEverythingS == null || !checkEverythingS.equalsIgnoreCase("false");
	static String stripNumbersS = System.getProperty("org.flasck.golden.strip"); 
	static boolean stripNumbers = stripNumbersS != null && stripNumbersS.equalsIgnoreCase("true");
	static String useRunner = System.getProperty("org.flasck.golden.runner");
	// Note that not specifying defaults to "JS"; but "neither" or "none" (or almost anything else, in fact) does not run either
	static boolean useJSRunner = useRunner == null || useRunner.equals("js") || useRunner.equals("both");
	static boolean useJVMRunner = useRunner != null && (useRunner.equals("jvm") || useRunner.equals("both"));
	static String buildDroidOpt = System.getProperty("org.flasck.golden.buildDroid");
	private static File droidToClasses;
	private static File handdir;
	private static File goldenDir;

	@BeforeClass
	public static void compileGolden() throws Exception {
		File dir = new File("src/test/resources/cards/test.runner");
		goldenDir = new File("src/golden-jvm");
		File out = new File("bin/jvmtest");
		File errs = new File(out, "errors");
		File droidTo = new File(out, "droid-to");
		File droid = new File(out, "droid-tmp");

		File utpath;
		{
			assertNotNull("There was no jvm dir", GoldenCGRunner.jvmdir);
			File jvmbin = new File(GoldenCGRunner.jvmdir, "jvm/bin");
			if (jvmbin.exists()) {
				utpath = new File(jvmbin, "classes");
				FileUtils.cleanDirectory(goldenDir);
				FileUtils.assertDirectory(goldenDir);
				handdir = new File(jvmbin, "classes/test/runner");
				for (File f : FileUtils.findFilesMatching(handdir, "*.class")) {
					String golden = CGHarnessTestBase.inspect(FileUtils.readAllStream(new FileInputStream(f)));
					final File bci = new File(goldenDir, FileUtils.ensureExtension(f.getName(), ".bci"));
					FileUtils.writeFile(bci, golden);
				}
			} else {
				logger.warn("No jvm/bin dir found, using repository golden files");
				utpath = new File(new File(GoldenCGRunner.jvmdir, "jvm/qbout"), "classes");
			}
		}
		
		TestEnvironment.clean(errs);
		TestEnvironment.clean(droidTo);
		TestEnvironment.clean(droid);
//		TestEnvironment te = new TestEnvironment(GoldenCGRunner.jvmdir, out.getPath(), true, useJSRunner, useJVMRunner, false, checkEverything, stripNumbers);
		
		Main.setLogLevels();
		FLASCompiler compiler = new FLASCompiler(null);
//		compiler.searchIn(new File(GoldenCGRunner.jvmdir, "services/flim"));
//		compiler.unitTestPath(utpath);
//		compiler.unitjs(useJSRunner);
//		compiler.unitjvm(useJVMRunner);
		compiler.errorWriter(new PrintWriter(System.out));
//		try {
//			compiler.writeJVMTo(droidTo);
//			compiler.compile(dir);
//		} catch (ErrorResultException ex) {
//			GoldenCGRunner.handleErrors(te, errs, ex.errors, null);
//		} catch (Throwable t) {
//			t.printStackTrace();
//			throw t;
//		}
		
		droidToClasses = new File(droidTo, "test/runner");
		assertTrue("JVM was not created", droidToClasses.isDirectory());
		
		int width = 170;
		String wd = System.getProperty("org.zinutils.cg.width");
		if (wd != null)
			width = Integer.parseInt(wd);
		CGHarnessTestBase.configure(false, width, true);
	}

	@Test
	public void checkTheTotalNumberOfFiles() {
		final TreeSet<File> genned = FileUtils.findFilesUnderMatching(droidToClasses, "*.class").stream().map(f -> FileUtils.ensureExtension(f, ".bci")).collect(Collectors.toCollection(TreeSet::new));
		System.out.println(genned);
		final List<File> w1 = FileUtils.findFilesUnderMatching(goldenDir, "*.bci");
		TreeSet<File> written = new TreeSet<>(w1);
		System.out.println(written);
		written.removeAll(genned);
		genned.removeAll(w1);
		assertEquals("there were " + genned.size() + " generated files that were not hand-written: " + genned, 0, genned.size());
		assertEquals("there were " + written.size() + " files written by hand that were not generated: " + written, 0, written.size());
	}
	
	@Test
	public void checkDataStoreUp() throws Exception {
		compare("DataStore$Up");
	}

	@Test
	public void checkDataStoreDown() throws Exception {
		compare("DataStore$Down");
	}

	@Test
	public void checkEchoUp() throws Exception {
		compare("Echo$Up");
	}

	@Test
	public void checkEchoDown() throws Exception {
		compare("Echo$Down");
	}

	@Test
	public void checkEchoImpl() throws Exception {
		compare("Echo$Impl");
	}

	@Test
	public void checkMyHandlerUp() throws Exception {
		compare("MyHandler$Up");
	}

	@Test
	public void checkMyHandlerDown() throws Exception {
		compare("MyHandler$Down");
	}

	@Test
	public void checkMyHandlerImpl() throws Exception {
		compare("MyHandler$Impl");
	}

	@Test
	public void checkTestCard() throws Exception {
		compare("TestCard");
	}

	@Test
	public void checkTrackerInit() throws Exception {
		compare("TestCard$inits_tracker");
	}

	@Test
	public void checkAccount() throws Exception {
		compare("Account");
	}

	@Test
	public void checkLocalHandler() throws Exception {
		compare("TestCard$LocalHandler");
	}

	@Test
	public void checkH1() throws Exception {
		compare("TestCard$handlers_1");
	}

	@Test
	public void checkH3() throws Exception {
		compare("TestCard$handlers_3");
	}

	@Test
	public void checkB1() throws Exception {
		compare("TestCard$B1");
	}
	
	@Test
	public void checkB2() throws Exception {
		compare("TestCard$B2");
	}

	@Test
	public void checkB3() throws Exception {
		compare("TestCard$B3");
	}
	
	@Test
	public void checkC1() throws Exception {
		compare("TestCard$_C1");
	}
	
	@Test
	public void checkEcho() throws Exception {
		compare("TestCard$echoHello");
	}
	
	@Test
	public void checkEchoMessage() throws Exception {
		compare("TestCard$echoMessage");
	}
	
	@Test
	public void checkDoMy() throws Exception {
		compare("TestCard$domy");
	}
	
	@Test
	public void checkTracker() throws Exception {
		compare("Tracker");
	}
	
	@Test
	public void checkTrackerSimpleCtor() throws Exception {
		compare("Tracker$_ctor_simple");
	}
	
	@Test
	public void checkPkg() throws Exception {
		compare("PACKAGEFUNCTIONS");
	}
	
	@Test
	public void checkId() throws Exception {
		compare("PACKAGEFUNCTIONS$id");
	}
	
	@Test
	public void checkStyleIf() throws Exception {
		compare("PACKAGEFUNCTIONS$styleIf");
	}
	
	@Test
	public void checkX() throws Exception {
		compare("PACKAGEFUNCTIONS$x");
	}
	
	private void compare(final String file) throws FileNotFoundException, Exception {
		String golden = FileUtils.readFile(new File(goldenDir, file + ".bci"));
		byte[] gbs = FileUtils.readAllStream(new FileInputStream(new File(droidToClasses, file + ".class")));
		CGHarnessTestBase.compareToGolden(file, golden, gbs);
	}
}
