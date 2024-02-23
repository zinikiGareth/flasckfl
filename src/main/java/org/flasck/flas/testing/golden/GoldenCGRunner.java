package org.flasck.flas.testing.golden;

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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.flasck.flas.Main;
import org.flasck.flas.compiler.PhaseTo;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.testing.golden.grammar.GrammarChecker;
import org.flasck.flas.testing.golden.grammar.GrammarTree;
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
	// Note that not specifying defaults to "both"; but "neither" or "none" (or anything but 'js', 'jvm' or 'both', in fact) does not run either
	static boolean useJSRunner = useRunner == null || useRunner.equals("js") || useRunner.equals("both");
	static boolean useJVMRunner = useRunner == null || useRunner.equals("jvm") || useRunner.equals("both");
	static String buildDroidOpt = System.getProperty("org.flasck.golden.buildDroid");
	static String maxcnt = System.getProperty("org.flasck.golden.cnt");
	static String flascklibOption = System.getProperty("org.flasck.golden.flascklib");
	static String flascklib = flascklibOption != null ? flascklibOption : "src/main/resources/flasck";
	private static int MAXCNT = maxcnt == null ? Integer.MAX_VALUE : Integer.parseInt(maxcnt);
	protected static Interceptor interceptor = null;
	private static String wantOrderedOption = System.getProperty("wantOrdered");
	private static boolean wantOrdered = wantOrderedOption == null || wantOrderedOption.equals("ordered");
	private static String checkGrammarOption = System.getProperty("checkGrammar");
	private static boolean checkGrammar = true; // checkGrammarOption != null && checkGrammarOption.equals("check");
	
	public static final File jvmdir;
	static {
		Main.setLogLevels();
		File jd = new File("../FLASJvm");
		if (!jd.exists()) {
			jd = new File("../../FLASJvm");
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
			if (f.isDirectory())
				sf.add(f.getParentFile());
		for (File f : FileUtils.findFilesMatching(new File("src/golden"), "packages"))
			sf.add(f.getParentFile());
		sf = trackOrdering(sf);
		ByteCodeCreator bcc = CGHarnessRunnerHelper.emptyTestClass(bce, clz.getName());
		if (wantOrdered)
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
		
		if (interceptor != null)
			interceptor.before(s);
		
		final File actualErrors = new File(s, "errors-tmp");
		final File expectedErrors = new File(s, "errors");
		final File tr = new File(s, "testReports-tmp");
		final File packages = new File(s, "packages");
		final File flimstore = new File(s, "flimstore");
		final File flimstoreTo = new File(s, "flimstore-tmp");
		final File flimfrom = new File(s, "flim-imports");
		final File incldirs = new File(s, "incl");
		final File modules = new File(s, "modules");
		final File parseTokens = new File(s, "parsetokens");
		final File reconstruct = new File(s, "reconstruct");
		FileUtils.cleanDirectory(actualErrors);
		FileUtils.cleanDirectory(tr);
		FileUtils.cleanDirectory(parseTokens);
		FileUtils.cleanDirectory(reconstruct);
		FileUtils.assertDirectory(actualErrors);
		FileUtils.assertDirectory(tr);
		FileUtils.assertDirectory(parseTokens);
		FileUtils.assertDirectory(reconstruct);
		FileUtils.deleteDirectoryTree(flimstoreTo);
		if (flimfrom.exists()) {
			FileUtils.assertDirectory(flimstoreTo);
			copyFlimstoresTo(flimstoreTo, flimfrom);
		}
		List<String> packageList = new ArrayList<>();
		if (packages.exists()) {
			packageList = FileUtils.readFileAsLines(packages);
		}
		if (flimstore.exists()) {
			// Only create flimstore-to and populate it if we have a flimstore with packages that need importing
			TreeSet<String> create = new TreeSet<>();
			for (File f : FileUtils.findFilesMatching(flimstore, "*")) {
				create.add(f.getName());
			}
			create.removeAll(packageList);
			if (!create.isEmpty()) {
				// If we do need to import something, copy everything to check we don't load in what we shouldn't
				FileUtils.assertDirectory(flimstoreTo);
				for (File f : FileUtils.findFilesMatching(flimstore, "*")) {
					FileUtils.copy(f, flimstoreTo);
				}
			}
		}
		List<String> args = new ArrayList<String>();
		args.addAll(Arrays.asList("--flascklib", flascklib, "--root", s, "--jvmout", "jvmout", "--jsout", "jsout", "--testReports", "testReports-tmp", "--errors", "errors-tmp/errors", "--types", "tc-tmp/types"));
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
		if (flimfrom.exists()) {
			for (String ff : FileUtils.readFileAsLines(flimfrom)) {
				if (ff.startsWith("."))
					continue;
				args.add("--incl");
				args.add("src/golden/" + ff + "/jsout");
				args.add("--incl");
				args.add("src/golden/" + ff + "/jvmout");
			}
		}
		if (incldirs.exists()) {
			for (String fi : FileUtils.readFileAsLines(incldirs)) {
				args.add("--incl");
				args.add(fi);
			}
		}
		if (modules.exists()) {
			for (String fi : FileUtils.readFileAsLines(modules)) {
				File f = new File(fi);
				args.add("--moduledir");
				args.add(f.getParent());
				args.add("--module");
				args.add(f.getName());
			}
		}
		if (interceptor != null)
			interceptor.addIncludes(args);
//		args.add("--dumprepo");
//		args.add("repo.txt");

		args.add("--testname");
		args.add(s.replace("/", "-").replace("src-golden-", ""));
		if (packages.exists()) {
			args.addAll(packageList);
		} else
			args.add("test.golden");
//		try {
		Main.standardCompiler(parseTokens, args.toArray(new String[args.size()]));
//		} finally {
//		FileUtils.cat(new File(s, "repo.txt"));
//		}
		checkExpectedErrors(te, expectedErrors, actualErrors);
		GrammarChecker r = new GrammarChecker(parseTokens, reconstruct);
		// TODO: allow it to merge in other grammars such as Ziniki
		Map<String, GrammarTree> fileOrchards = r.checkParseTokenLogic(expectedErrors.isDirectory());
		if (checkGrammar) {
			r.checkGrammar(fileOrchards);
		}
		AssertionError tmp = null;
//		if (!expectedErrors.isDirectory()) {
			try {
				te.checkTestResults(expectedErrors.isDirectory());
				te.checkFlimStore(expectedErrors.isDirectory());
				te.checkTypes(expectedErrors.isDirectory());
			} catch (AssertionError ex) {
				tmp = ex;
			}
			if (checkGrammar && !expectedErrors.isDirectory()) {
				File golden = new File(s, "test.golden");
				if (golden.exists())
					te.checkReconstructions(golden, reconstruct);
				for (String p : packageList) {
					te.checkReconstructions(new File(s, p), reconstruct);
				}
			}
//		}
		if (tmp != null)
			throw tmp;
	}

	private static void copyFlimstoresTo(File flimstoreTo, File flimfrom) throws FileNotFoundException, IOException {
		try (LineNumberReader lnr = new LineNumberReader(new FileReader(flimfrom))) {
			String s;
			while ((s = lnr.readLine()) != null) {
				File f;
				if (s.startsWith("."))
					f = new File(s);
				else
					f = new File("src/golden", s);
				if (!f.isDirectory())
					throw new RuntimeException("No FLIM directory " + s + " in golden test");
				File g = new File(f, "flimstore");
				if (!g.isDirectory())
					g = f;
				boolean any = false;
				for (File z : FileUtils.findFilesMatching(g, "*.flim")) {
					FileUtils.copy(z, flimstoreTo);
					any = true;
				}
				if (!any)
					throw new RuntimeException("No .flim files found in " + g);
				for (File z : FileUtils.findFilesMatching(g, "*.js")) {
					FileUtils.copy(z, flimstoreTo);
				}
				for (File z : FileUtils.findFilesMatching(g, "*.jar")) {
					FileUtils.copy(z, flimstoreTo);
				}
			}
		}
	}


	private static boolean checkExpectedErrors(TestEnvironment te, File expectedErrors, File actualErrors) {
		final File aef = new File(actualErrors, "errors");
		if (expectedErrors.isDirectory()) {
			// fairly obviously, we are expecting errors, but we say we aren't so the checks go through
			te.assertGolden(false, expectedErrors, actualErrors, false, false);
			return false;
		} else if (aef.length() > 0) {
			FileUtils.cat(aef);
			fail("unexpected compilation errors");
			return false; // won't actually happen
		} else
			return true;
	}
}
