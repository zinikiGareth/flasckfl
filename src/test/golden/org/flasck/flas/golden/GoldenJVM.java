package org.flasck.flas.golden;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.TreeSet;

import org.flasck.flas.Main;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.errors.ErrorResultException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zinutils.cgharness.CGHarnessTestBase;
import org.zinutils.utils.FileUtils;

public class GoldenJVM {
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

	@BeforeClass
	public static void compileGolden() throws Exception {
		File dir = new File("src/test/resources/cards/test.runner");
		File out = new File("bin/jvmtest");
		File errs = new File(out, "errors");
		File droidTo = new File(out, "droid-to");
		File droid = new File(out, "droid-tmp");
		
		GoldenCGRunner.clean(errs);
		GoldenCGRunner.clean(droidTo);
		GoldenCGRunner.clean(droid);
		
		Main.setLogLevels();
		FLASCompiler compiler = new FLASCompiler();
		compiler.searchIn(new File(GoldenCGRunner.jvmdir, "services/flim"));
		compiler.unitTestPath(new File(GoldenCGRunner.jvmdir, "jvm/bin/classes"));
		compiler.unitjs(useJSRunner);
		compiler.unitjvm(useJVMRunner);
		try {
			compiler.writeJVMTo(droidTo);
			compiler.compile(dir);
		} catch (ErrorResultException ex) {
			GoldenCGRunner.handleErrors(errs, ex.errors, null);
		} catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}
		
		droidToClasses = new File(droidTo, "test/runner");
		assertTrue("JVM was not created", droidToClasses.isDirectory());
		handdir = new File("/Users/gareth/Ziniki/Over/flasjvm/jvm/bin/classes/test/runner");
		
		int width = 170;
		String wd = System.getProperty("org.zinutils.cg.width");
		if (wd != null)
			width = Integer.parseInt(wd);
		CGHarnessTestBase.configure(false, width, true);
	}
	
	@Test
	public void checkTheTotalNumberOfFiles() {
		final List<File> g1 = FileUtils.findFilesUnderMatching(droidToClasses, "*.class");
		TreeSet<File> genned = new TreeSet<File>(g1);
		System.out.println(genned);
		TreeSet<File> written = new TreeSet<File>(FileUtils.findFilesUnderMatching(handdir, "*.class"));
		System.out.println(written);
		genned.removeAll(written);
		written.removeAll(g1);
		assertEquals("there were " + genned.size() + " generated files that were not hand-written: " + genned, 0, genned.size());
		assertEquals("there were " + written.size() + " files written by hand that were not generated: " + written, 0, written.size());
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
	public void checkEcho() throws Exception {
		compare("TestCard$echoHello");
	}
	
	@Test
	public void checkEchoMessage() throws Exception {
		compare("TestCard$echoMessage");
	}
	
	@Test
	public void checkTracker() throws Exception {
		compare("Tracker");
	}
	
	@Test
	public void checkTrackerInitCnt() throws Exception {
		compare("Tracker$inits_cnt");
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
		byte[] hbs = FileUtils.readAllStream(new FileInputStream(new File(handdir, file + ".class")));
		byte[] gbs = FileUtils.readAllStream(new FileInputStream(new File(droidToClasses, file + ".class")));
		CGHarnessTestBase.compare(file, hbs, gbs);
	}
}
