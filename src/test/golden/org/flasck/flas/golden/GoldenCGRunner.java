package org.flasck.flas.golden;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.flasck.flas.Main;
import org.flasck.flas.compiler.PhaseTo;
import org.flasck.flas.errors.ErrorResultException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.MethodSorters;
import org.junit.runners.model.InitializationError;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.cgharness.CGHClassLoaderImpl;
import org.zinutils.cgharness.CGHarnessRunnerHelper;
import org.zinutils.cgharness.TestMethodContentProvider;
import org.zinutils.utils.FileNameComparator;
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
	static String maxcnt = System.getProperty("org.flasck.golden.cnt");
	private static int MAXCNT = maxcnt == null ? Integer.MAX_VALUE : Integer.parseInt(maxcnt);
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
		super(figureTests(clz));
	}
	

	private static Class<?> figureTests(Class<?> clz) throws IOException, ErrorResultException {
		ByteCodeEnvironment bce = new ByteCodeEnvironment();
		CGHClassLoaderImpl cl = new CGHClassLoaderImpl();
		
		Pattern p = null;
		String match = System.getProperty("org.flasck.golden.pattern");
		if (match != null && match.length() > 0) {
			p = Pattern.compile(match);
		}

		DecimalFormat df = new DecimalFormat("000");
		int cnt = 1;
		Set<File> sf = new TreeSet<>(new FileNameComparator());
		for (File f : FileUtils.findFilesMatching(new File("src/golden"), "test.golden"))
			sf.add(f.getParentFile());
		for (File f : FileUtils.findFilesMatching(new File("src/golden"), "packages"))
			sf.add(f.getParentFile());
		sf = trackOrdering(sf);
		ByteCodeCreator bcc = CGHarnessRunnerHelper.emptyTestClass(bce, clz.getName());
		bcc.addRTVAnnotation("org.junit.FixMethodOrder").addEnumParam(MethodSorters.NAME_ASCENDING);
		for (File dir : sf) {
			if (p == null || p.matcher(dir.getPath()).find()) {
				addGoldenTest(bcc, "ut"+df.format(cnt++)+"_", dir);
			}
			if (cnt > MAXCNT)
				break;
		}
		return CGHarnessRunnerHelper.generate(cl, bcc);
	}

	private static Set<File> trackOrdering(Set<File> sf) throws IOException {
		File orig = new File("testorder");
		Set<File> ret;
		if (orig.exists()) {
			ret = new LinkedHashSet<File>();
			try (LineNumberReader lnr = new LineNumberReader(new FileReader(orig))) {
				String s;
				while ((s = lnr.readLine()) != null) {
					File t = new File(s);
					if (t.isDirectory())
						ret.add(t);
				}
			}
			ret.addAll(sf);
		} else
			ret = sf;
		File out = new File("testorder.new");
		try (PrintWriter pw = new PrintWriter(out)) {
			for (File f : ret)
				pw.println(f.getPath());
		}
		Files.move(out.toPath(), orig.toPath(), StandardCopyOption.REPLACE_EXISTING);
		return ret;
	}


	private static void addGoldenTest(ByteCodeCreator bcc, String prefix, final File f) {
		boolean ignoreTest = new File(f, "ignore").exists();
		String phase = new File(f, "phase").exists() ? FileUtils.readFile(new File(f, "phase")) : PhaseTo.COMPLETE.toString();
		boolean runjvm = !new File(f, "jsonly").exists();
		boolean runjs = !new File(f, "jvmonly").exists();

		StringBuilder name = makeNameForTest(f);
		name.insert(0, prefix);
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
		final File packages = new File(s, "packages");
		final File flimstore = new File(s, "flimstore");
		final File flimstoreTo = new File(s, "flimstore-tmp");
		final File flimfrom = new File(s, "flim-imports");
		FileUtils.cleanDirectory(actualErrors);
		FileUtils.cleanDirectory(tr);
		FileUtils.assertDirectory(actualErrors);
		FileUtils.assertDirectory(tr);
		FileUtils.deleteDirectoryTree(flimstoreTo);
		if (flimfrom.exists()) {
			FileUtils.assertDirectory(flimstoreTo);
			copyFlimstoresTo(flimstoreTo, flimfrom);
		}
		if (flimstore.exists()) {
			// TODO: we should create flimstoreTo & populate it with all the things not in "packages" (or test.golden) so that it is ready to go
			// If there are no such things, do not create it ...
//			FileUtils.assertDirectory(flimstoreTo);
		}
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
		if (flimstore.exists() || flimstoreTo.exists()) {
			args.add("--flim");
			args.add(flimstoreTo.getPath());
		}
		args.add("--testname");
		args.add(s.replace("/", "-").replace("src-golden-", ""));
		if (packages.exists()) {
			args.addAll(FileUtils.readFileAsLines(packages));
		} else
			args.add("test.golden");
		Main.standardCompiler(args.toArray(new String[args.size()]));
		if (checkExpectedErrors(te, expectedErrors, actualErrors)) {
			te.checkTestResults();
			te.checkFlimStore();
			te.checkTypes();
		}
	}

	private static void copyFlimstoresTo(File flimstoreTo, File flimfrom) throws FileNotFoundException, IOException {
		try (LineNumberReader lnr = new LineNumberReader(new FileReader(flimfrom))) {
			String s;
			while ((s = lnr.readLine()) != null) {
				File f = new File("src/golden", s);
				if (!f.isDirectory())
					throw new RuntimeException("No golden test " + s);
				File g = new File(f, "flimstore");
				if (!g.isDirectory())
					throw new RuntimeException("Test " + s + " does not provide a flimstore");
				for (File z : FileUtils.findFilesMatching(g, "*")) {
					FileUtils.copy(z, flimstoreTo);
				}
				// TODO: I think we also want to copy the jsout & jvmout bits ... 
			}
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
