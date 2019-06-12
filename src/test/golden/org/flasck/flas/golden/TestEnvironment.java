package org.flasck.flas.golden;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.debug.PFDumper;
import org.flasck.flas.errors.ErrorResult;
import org.zinutils.system.RunProcess;
import org.zinutils.utils.Crypto;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.Indenter;
import org.zinutils.utils.StringUtil;

public class TestEnvironment {

	private File importFrom;
	private File zip;
	private File pform;
	private File pformGolden;
	private File rwform;
	private File jsto;
	private File dependTo;
	private File hsie;
	private File flim;
	private File tc2;
	private File droidTo;
	private File droid;
	private File testReportTo;
	private File errors;
	private boolean isLegacy;
	private boolean useJSRunner;
	private boolean useJVMRunner;
	private File jvmbin;
	private File depend;
	private File rootdir;
	private File testReports;
	private boolean checkNothing;
	private boolean checkEverything;
	private boolean stripNumbers;
	private File goldhs;
	private File rwGolden;
	private File goldjs;
	private File goldtc;
	private File goldflim;
	private File c3p0;

	public TestEnvironment(File jvmdir, String root, boolean isLegacy, boolean useJSRunner, boolean useJVMRunner, boolean checkNothing, boolean checkEverything, boolean stripNumbers) throws FileNotFoundException, IOException {
		this.rootdir = new File(root, "test.golden");
		this.isLegacy = isLegacy;
		this.useJSRunner = useJSRunner;
		this.useJVMRunner = useJVMRunner;
		this.checkNothing = checkNothing;
		this.checkEverything = checkEverything;
		this.stripNumbers = stripNumbers;
		importFrom = new File(root, "import");
		zip = zipWebZip(root);
		pform = new File(root, "parser-tmp");
		pformGolden = new File(root, "pform");
		rwGolden = new File(root, "rw");
		rwform = new File(root, "rw-tmp");
		jsto = new File(root, "jsout-tmp");
		goldjs = new File(root, "jsout");
		dependTo = new File(root, "depend-tmp");
		goldhs = new File(root, "hsie");
		hsie = new File(root, "hsie-tmp");
		goldflim = new File(root, "flim");
		flim = new File(root, "flim-tmp");
		goldtc = new File(root, "tc");
		tc2 = new File(root, "tc-tmp");
		c3p0 = new File(root, "droid");
		droidTo = new File(root, "droid-to");
		droid = new File(root, "droid-tmp");
		testReports = new File(root, "testReports");
		testReportTo = new File(root, "testReports-tmp");
		errors = new File(root, "errors-tmp");
		depend = new File(root, "depend");

		jvmbin = new File(jvmdir, "jvm/bin");
		if (!jvmbin.exists())
			jvmbin = new File(jvmdir, "jvm/qbout");
		if (!jvmbin.exists())
			throw new RuntimeException("No jvm bin directory could be found");

	}

	public FLASCompiler configureCompiler() {
		FLASCompiler compiler = new FLASCompiler(null);
		compiler.unitTestPath(new File(jvmbin, "classes"));
		compiler.unitjs(useJSRunner);
		compiler.unitjvm(useJVMRunner);
		if (zip != null) {
			compiler.webZipDir(zip.getParentFile());
			compiler.useWebZip(zip.getName());
		}
		compiler.searchIn(new File("../FLASJvm/services/flim"));
		if (importFrom.isDirectory())
			compiler.searchIn(importFrom);
		compiler.trackTC(tc2);
		compiler.writeRWTo(rwform);
		compiler.writeJSTo(jsto);
		compiler.writeHSIETo(hsie);
		compiler.writeFlimTo(flim);
		compiler.writeJVMTo(droidTo);
		if (haveTests()) {
			clean(testReportTo);
			compiler.writeTestReportsTo(testReportTo);
		}
		if (depend.isDirectory()) {
			clean(dependTo);
			compiler.writeDependsTo(dependTo);
		}
		compiler.unitjvm(true);
		compiler.scanWebZips();

		return compiler;
	}

	public boolean haveTests() {
		return !FileUtils.findFilesMatching(rootdir, "*.ut").isEmpty();
	}

	private File zipWebZip(String s) throws FileNotFoundException, IOException {
		File webzip = new File(s, "webzip");
		if (webzip.exists()) {
			File zip = new File(webzip, "webzip.zip");
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
			for (File f : webzip.listFiles(f -> f.getPath().endsWith(".html"))) {
				zos.putNextEntry(new ZipEntry(f.getName()));
				FileUtils.copyFileToStream(f, zos);
				zos.closeEntry();
			}
			zos.close();
			return zip;
		} else
			return null;
	}

	public void cleanUp() {
		FileUtils.deleteDirectoryTree(errors);
		clean(pform);
		clean(rwform);
		clean(jsto);
		clean(hsie);
		clean(flim);
		clean(droidTo);
		clean(droid);
		clean(tc2);
	}
	
	public static void clean(File dir) {
		FileUtils.cleanDirectory(dir);
		FileUtils.assertDirectory(dir);
	}

	public void dump(File input, Object sr, ErrorResult er) throws FileNotFoundException {
		PFDumper dumper = new PFDumper();
		Indenter pw = new Indenter(new File(pform, input.getName().replace(".fl", ".pf")));
		pw.close();
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

	public void checkGeneration() throws IOException {
		if (isLegacy)
			checkLegacyGeneration();
	}
	
	public void checkLegacyGeneration() throws IOException {
		if (stripNumbers) {
			stripPform(pform);
			stripPform(pformGolden);
		}
		assertGolden(pformGolden, pform);
		
		File droidToClasses = new File(droidTo, "test/golden");
		if (droidToClasses.isDirectory()) {
			FileOutputStream fos = new FileOutputStream(new File(droid, "droid.clz"));
			List<File> files = FileUtils.findFilesMatching(droidToClasses, "*.java");
			files.sort(FileUtils.pathNameComparator);
			for (File f : files) {
				fos.write((f.getPath()+"\n").getBytes());
				FileUtils.copyFileToStream(f, fos);
			}
			fos.close();
		}

		// Now assert that we matched things ...
		if (depend.isDirectory()) {
			assertGolden(depend, dependTo);
		}
		assertGolden(rwGolden, rwform);
		if (stripNumbers) {
			stripHSIE(goldhs);
			stripHSIE(hsie);
		}
		assertGolden(goldjs, jsto);
		assertGolden(goldhs, hsie);
		assertGolden(goldtc, tc2);
		assertGolden(goldflim, flim);

		assertGolden(c3p0, droid);
	}


	// I want to see the locations, but I don't (always) want to be bound to them.  Remove them if desired (i.e. call this method if desired)
	public void stripPform(File pform) throws IOException {
		for (File pf : pform.listFiles()) {
			File tmp = File.createTempFile("temp", ".pf");
			tmp.deleteOnExit();
			PrintWriter to = new PrintWriter(tmp);
			LineNumberReader lnr = new LineNumberReader(new FileReader(pf));
			try {
				String s;
				while ((s = lnr.readLine()) != null) {
					int idx = s.indexOf(" @{");
					if (idx != -1)
						s = s.substring(0, idx);
					to.println(s);
				}
			} finally {
				to.close();
				lnr.close();
			}
			FileUtils.copy(tmp, pf);
			tmp.delete();
		}
	}

	public void stripHSIE(File pform) throws IOException {
		for (File pf : pform.listFiles()) {
			File tmp = File.createTempFile("temp", ".hs");
			tmp.deleteOnExit();
			PrintWriter to = new PrintWriter(tmp);
			LineNumberReader lnr = new LineNumberReader(new FileReader(pf));
			try {
				String s;
				while ((s = lnr.readLine()) != null) {
					int idx = s.indexOf(" #");
					if (idx != -1)
						s = StringUtil.trimRight(s.substring(0, idx));
					to.println(s);
				}
			} finally {
				to.close();
				lnr.close();
			}
			FileUtils.copy(tmp, pf);
			tmp.delete();
		}
	}
}
