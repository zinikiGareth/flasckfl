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
import org.junit.Ignore;
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
		}
		
		droidToClasses = new File(droidTo, "test/runner");
		assertTrue("JVM was not created", droidToClasses.isDirectory());
		handdir = new File("/Users/gareth/Ziniki/Over/flasjvm/jvm/bin/classes/test/runner");
		
		CGHarnessTestBase.configure(false, 170, true);
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
	@Ignore
	public void checkAccount() throws Exception {
		compare("Account");
	}

	private void compare(final String file) throws FileNotFoundException, Exception {
		byte[] hbs = FileUtils.readAllStream(new FileInputStream(new File(handdir, file + ".class")));
		byte[] gbs = FileUtils.readAllStream(new FileInputStream(new File(droidToClasses, file + ".class")));
		CGHarnessTestBase.compare(file, hbs, gbs);
	}
}
