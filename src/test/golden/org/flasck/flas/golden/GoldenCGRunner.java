package org.flasck.flas.golden;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

import org.flasck.flas.Main;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.stories.StoryRet;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.cgharness.CGHClassLoaderImpl;
import org.zinutils.cgharness.CGHarnessRunner;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.StringUtil;

public class GoldenCGRunner extends CGHarnessRunner {
	static String checkOption = System.getProperty("org.flasck.golden.check");
	static boolean checkEverything = checkOption == null || !checkOption.equalsIgnoreCase("false");
	static boolean checkNothing = checkOption != null && checkOption.equalsIgnoreCase("nothing");
	static String tdaOption = System.getProperty("org.flasck.golden.tda");
	static boolean useTDA = tdaOption != null && ("true".equalsIgnoreCase(tdaOption) || "both".equalsIgnoreCase(tdaOption));
	static boolean useOLD = tdaOption == null || "both".equalsIgnoreCase(tdaOption) || "false".equalsIgnoreCase(tdaOption);
	static String stripNumbersS = System.getProperty("org.flasck.golden.strip"); 
	static boolean stripNumbers = stripNumbersS != null && stripNumbersS.equalsIgnoreCase("true");
	static String useRunner = System.getProperty("org.flasck.golden.runner");
	// Note that not specifying defaults to "JS"; but "neither" or "none" (or almost anything else, in fact) does not run either
	static boolean useJSRunner = useRunner == null || useRunner.equals("js") || useRunner.equals("both");
	static boolean useJVMRunner = useRunner != null && (useRunner.equals("jvm") || useRunner.equals("both"));
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

	public GoldenCGRunner(Class<?> klass, RunnerBuilder builder) throws InitializationError, IOException, ErrorResultException {
		super(builder, figureClasses());
	}
	
	private static Class<?>[] figureClasses() throws IOException, ErrorResultException {
		ByteCodeEnvironment bce = new ByteCodeEnvironment();
		CGHClassLoaderImpl cl = new CGHClassLoaderImpl();
		
		Pattern p = null;
		String match = System.getProperty("org.flasck.golden.pattern");
		if (match != null && match.length() > 0) {
			p = Pattern.compile(match);
		}

		ByteCodeCreator bcc = emptyTestClass(bce, "goldenTests");
		for (File f : FileUtils.findFilesMatching(new File("src/golden"), "*.fl")) {
			File dir = f.getParentFile().getParentFile();
			if (p == null || p.matcher(dir.getPath()).find())
				addGoldenTest(bcc, dir);
		}
		return new Class<?>[] { generate(cl, bcc) };
	}

	private static void addGoldenTest(ByteCodeCreator bcc, final File f) {
		boolean ignoreTest = new File(f, "ignore").exists();
		boolean legacyTest = new File(f, "legacy").exists();
		boolean approvedForTDA = new File(f, "tda").exists();

		File f1 = FileUtils.makeRelativeTo(f, new File("src/golden"));
		StringBuilder name = new StringBuilder();
		while (f1 != null) {
			name.insert(0, StringUtil.capitalize(f1.getName()));
			f1 = f1.getParentFile();
		}
		name.insert(0, "test");
		if (useOLD)
			addTests(bcc, f, name.toString(), ignoreTest, legacyTest, false);
		if (useTDA && approvedForTDA) {
			name.append("_tda");
			addTests(bcc, f, name.toString(), ignoreTest, legacyTest, true);
		}
	}

	private static void addTests(ByteCodeCreator bcc, final File f, String name, boolean ignoreTest, boolean legacyTest, boolean tdaTest) {
		addMethod(bcc, name, ignoreTest, new TestMethodContentProvider() {
			@Override
			public void defineMethod(NewMethodDefiner done) {
				done.callStatic(GoldenCGRunner.class.getName(), "void", "runGolden", done.stringConst(f.getPath()), done.boolConst(legacyTest), done.boolConst(tdaTest)).flush();
			}
		});
	}
	
	public static void runGolden(String s, boolean isLegacy, boolean runAsTDA) throws Exception {
		System.out.println("Run golden test for " + s);
		TestEnvironment te = new TestEnvironment(GoldenCGRunner.jvmdir, s, isLegacy, useJSRunner, useJVMRunner, checkNothing, checkEverything, stripNumbers);
		
		te.cleanUp();
		
		FLASCompiler compiler = te.configureCompiler();
		File dir = new File(s, "test.golden");
		if (!runAsTDA) {
			ErrorResult er = new ErrorResult();
			for (File input : FileUtils.findFilesMatching(dir, "*.fl")) {
				StoryRet sr = compiler.parse("test.golden", FileUtils.readFile(input));
				te.dump(input, sr, er);
			}
			if (er.hasErrors()) {
				handleErrors(te, s, er);
				return;
			}
		}

		if (runAsTDA) {
			final File actualErrors = new File(s, "errors-tmp");
			final File expectedErrors = new File(s, "errors");
			FileUtils.assertDirectory(actualErrors);
			compiler.errorWriter(new PrintWriter(new File(s, "errors-tmp/errors")));
			compiler.parse(dir);
			checkExpectedErrors(te, expectedErrors, actualErrors);
//			throw new UtilException("Didn't think about UTs did you?");
		} else {
			try {
				compiler.compile(dir);
				File errors = new File(s, "errors");
				if (errors.isDirectory())
					fail("expected errors, but none occurred");
			} catch (ErrorResultException ex) {
				handleErrors(te, s, ex.errors);
				return;
			}
		}
		
		te.checkTestResults();
		te.checkGeneration();

		/*
		// This is to build an actual APK, not code geenrate
		if (buildDroid)
			compiler.getBuilder().build();
			*/
	}

	@Deprecated
	protected static void handleErrors(TestEnvironment te, String s, ErrorReporter er) throws FileNotFoundException, IOException {
		// either way, write the errors to a suitable directory
		File etmp = new File(s, "errors-tmp"); // may or may not be needed
		File errors = new File(s, "errors");
		handleErrors(te, etmp, er, errors);
	}

	@Deprecated
	protected static void handleErrors(TestEnvironment te, File etmp, ErrorReporter er, File errors) throws FileNotFoundException, IOException {
		ErrorResult eres = (ErrorResult) er;
		// either way, write the errors to a suitable directory
		FileUtils.assertDirectory(etmp);
		PrintWriter pw = new PrintWriter(new File(etmp, "errors"));
		eres.showTo(pw, 0);
		pw.close();

		if (errors != null && errors.isDirectory()) {
			// we expected this, so check the errors are correct ...
			te.assertGolden(errors, etmp);
		} else {
			// we didn't expect the error, so by definition is an error
			eres.showTo(new PrintWriter(System.out), 0);
			fail("unexpected compilation errors");
		}
	}

	private static void checkExpectedErrors(TestEnvironment te, File expectedErrors, File actualErrors) {
		final File aef = new File(actualErrors, "errors");
		if (expectedErrors.isDirectory())
			te.assertGolden(expectedErrors, actualErrors);
		else if (aef.length() > 0) {
			FileUtils.cat(aef);
			fail("unexpected compilation errors");
		}
	}

	@Override
	protected void cleanUp() {
		// compiler.destroy();
	}

	@Override
	protected String getName() {
		return "FLAS Golden Tests";
	}
}
