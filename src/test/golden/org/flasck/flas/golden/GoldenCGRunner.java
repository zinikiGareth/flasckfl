package org.flasck.flas.golden;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.flasck.flas.Main;
import org.flasck.flas.compiler.PhaseTo;
import org.flasck.flas.errors.ErrorResultException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.cgharness.CGHClassLoaderImpl;
import org.zinutils.cgharness.CGHarnessRunnerHelper;
import org.zinutils.cgharness.TestMethodContentProvider;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.StringUtil;

public class GoldenCGRunner extends BlockJUnit4ClassRunner {
	static String checkOption = System.getProperty("org.flasck.golden.check");
	static boolean checkEverything = checkOption == null || !checkOption.equalsIgnoreCase("false");
	static boolean checkNothing = checkOption != null && checkOption.equalsIgnoreCase("nothing");
	static String tdaOption = System.getProperty("org.flasck.golden.tda");
	static String useRunner = System.getProperty("org.flasck.golden.runner");
	// Note that not specifying defaults to "JS"; but "neither" or "none" (or almost anything else, in fact) does not run either
	static boolean useJSRunner = useRunner == null || useRunner.equals("js") || useRunner.equals("both");
	static boolean useJVMRunner = useRunner == null || useRunner.equals("jvm") || useRunner.equals("both");
	static String buildDroidOpt = System.getProperty("org.flasck.golden.buildDroid");
//	private static boolean buildDroid = buildDroidOpt != null && buildDroidOpt.equals("true");
	
	public static final File jvmdir;
	static {
		Main.setLogLevels();
		File jd = new File("/Users/gareth/Ziniki/Over/FLASJvm");
		if (!jd.exists()) {
			jd = new File("../FLASJvm");
			if (!jd.exists()) {
				System.err.println("There is no directory for the FLASJvm code");
				jd = null;
			}
		}
		jvmdir = jd;
	}
	
	public GoldenCGRunner(Class<?> clz) throws InitializationError, IOException, ErrorResultException {
		super(figureClasses(clz)[0]);
	}
	

	private static Class<?>[] figureClasses(Class<?> clz) throws IOException, ErrorResultException {
		ByteCodeEnvironment bce = new ByteCodeEnvironment();
		CGHClassLoaderImpl cl = new CGHClassLoaderImpl();
		
		Pattern p = null;
		String match = System.getProperty("org.flasck.golden.pattern");
		if (match != null && match.length() > 0) {
			p = Pattern.compile(match);
		}

		List<File> fls = FileUtils.findFilesMatching(new File("src/golden"), "test.golden");
		ByteCodeCreator bcc = CGHarnessRunnerHelper.emptyTestClass(bce, clz.getName());
		for (File f : fls) {
			File dir = f.getParentFile();
			if (p == null || p.matcher(dir.getPath()).find()) {
				addGoldenTest(bcc, dir);
			}
		}
		return new Class<?>[] { CGHarnessRunnerHelper.generate(cl, bcc) };
	}

	private static void addGoldenTest(ByteCodeCreator bcc, final File f) {
		boolean ignoreTest = new File(f, "ignore").exists();
		String phase = new File(f, "phase").exists() ? FileUtils.readFile(new File(f, "phase")) : PhaseTo.COMPLETE.toString();
		boolean runjvm = !new File(f, "jsonly").exists();
		boolean runjs = !new File(f, "jvmonly").exists();

		StringBuilder name = makeNameForTest(f);
		name.insert(0, "test");
		addTests(bcc, f, name.toString(), ignoreTest, runjvm, runjs, phase);
	}

	private static StringBuilder makeNameForTest(final File f) {
		File f1 = FileUtils.makeRelativeTo(f, new File("src/golden"));
		StringBuilder name = new StringBuilder();
		while (f1 != null) {
			name.insert(0, StringUtil.capitalize(f1.getName()));
			f1 = f1.getParentFile();
		}
		return name;
	}

	private static void addTests(ByteCodeCreator bcc, final File f, String name, boolean ignoreTest, boolean runJvm, boolean runJs, String phase) {
		CGHarnessRunnerHelper.addMethod(bcc, name, ignoreTest, new TestMethodContentProvider() {
			@Override
			public void defineMethod(NewMethodDefiner done) {
				done.callStatic(GoldenCGRunner.class.getName(), "void", "runGolden", done.stringConst(f.getPath()), done.boolConst(runJvm), done.boolConst(runJs), done.stringConst(phase)).flush();
			}
		});
	}
	
	public static void runGolden(String s, boolean runJvm, boolean runJs, String phase) throws Exception {
		System.out.println("GoldenTest[" + s + "]:");
		TestEnvironment te = new TestEnvironment(GoldenCGRunner.jvmdir, s, useJSRunner && runJs, useJVMRunner && runJvm, checkNothing, checkEverything);
		te.cleanUp();
		
		final File actualErrors = new File(s, "errors-tmp");
		final File expectedErrors = new File(s, "errors");
		final File tr = new File(s, "testReports-tmp");
		FileUtils.cleanDirectory(actualErrors);
		FileUtils.cleanDirectory(tr);
		FileUtils.assertDirectory(actualErrors);
		FileUtils.assertDirectory(tr);
		List<String> args = new ArrayList<String>();
		args.addAll(Arrays.asList("--root", s, "--jvmout", "jvmout", "--jsout", "jsout", "--testReports", "testReports-tmp", "--errors", "errors-tmp/errors", "--types", "tc-tmp/types"));
		for (File wf : new File(s).listFiles()) {
			// TODO: this restricts us to directories, which are easier to work with, but we could add another case for ZIP files if we wanted ...
			// We could also add a case that zipped up the directory to /tmp and did that ...
			if (wf.isDirectory() && wf.getName().startsWith("web")) {
				args.add("--web");
				args.add(wf.getName());
			}
		}
		if (!useJVMRunner || !runJvm)
			args.add("--no-unit-jvm");
		if (!useJSRunner || !runJs)
			args.add("--no-unit-js");
		args.add("--testname");
		args.add(s.replace("/", "-").replace("src-golden-", ""));
		args.add("test.golden");
		Main.standardCompiler(args.toArray(new String[args.size()]));
		if (checkExpectedErrors(te, expectedErrors, actualErrors)) {
			te.checkTestResults();
			te.checkTypes();
		}
	}

	private static boolean checkExpectedErrors(TestEnvironment te, File expectedErrors, File actualErrors) {
		final File aef = new File(actualErrors, "errors");
		if (expectedErrors.isDirectory()) {
			te.assertGolden(expectedErrors, actualErrors);
			return false;
		} else if (aef.length() > 0) {
			FileUtils.cat(aef);
			fail("unexpected compilation errors");
			return false; // won't actually happen
		} else
			return true;
	}
}
