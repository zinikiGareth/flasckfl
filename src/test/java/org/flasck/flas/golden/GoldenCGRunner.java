package org.flasck.flas.golden;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.Compiler;
import org.flasck.flas.errors.ErrorResultException;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.cgharness.CGHClassLoaderImpl;
import org.zinutils.cgharness.CGHarnessRunner;
import org.zinutils.utils.Crypto;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.StringUtil;

public class GoldenCGRunner extends CGHarnessRunner {
	public GoldenCGRunner(Class<?> klass, RunnerBuilder builder) throws InitializationError, IOException, ErrorResultException {
		super(builder, figureClasses());
	}
	
	private static Class<?>[] figureClasses() throws IOException, ErrorResultException {
		ByteCodeEnvironment bce = new ByteCodeEnvironment();
		CGHClassLoaderImpl cl = new CGHClassLoaderImpl();
		
		List<Class<?>> ret = new ArrayList<Class<?>>();
		for (File f : new File("src/golden").listFiles()) {
			if (f.isDirectory())
				ret.add(goldenTest(bce, cl, f));
		}
		return ret.toArray(new Class<?>[ret.size()]);
	}

	private static Class<?> goldenTest(ByteCodeEnvironment bce, CGHClassLoaderImpl cl, final File f) {
		ByteCodeCreator bcc = emptyTestClass(bce, "test" + StringUtil.capitalize(f.getName()));
		addMethod(bcc, "testSomething", new TestMethodContentProvider() {
			@Override
			public void defineMethod(NewMethodDefiner done) {
				done.callStatic(GoldenCGRunner.class.getName(), "void", "runGolden", done.stringConst(f.getPath())).flush();
			}
		});
		return generate(cl, bcc);
	}
	
	public static void runGolden(String s) throws Exception {
		System.out.println("Run golden test for " + s);
		try {
			Compiler.setLogLevels();
			Compiler compiler = new Compiler();
			// read these kinds of things from "new File(s, ".settings")"
	//		compiler.writeDroidTo(new File("null"));
	//		compiler.searchIn(new File("src/main/resources/flim"));
			
			File jsto = new File(s, "jsout-tmp");
			File flim = new File(s, "flim-tmp");
			clean(jsto);
			clean(flim);
			compiler.writeJSTo(jsto);
			compiler.writeFlimTo(flim);
			compiler.compile(new File(s, "test.golden"));
			
			// Now assert that we matched things ...
			assertGolden(new File(s, "jsout"), jsto);
		} catch (ErrorResultException ex) {
			ex.errors.showTo(new PrintWriter(System.out), 0);
			throw ex;
		}
	}

	private static void assertGolden(File golden, File jsto) {
		if (!golden.isDirectory())
			fail("There is no golden directory " + golden);
		if (!jsto.isDirectory())
			fail("There is no generated directory " + jsto);
		for (File f : jsto.listFiles())
			assertTrue("There is no golden file for the generated " + f, new File(golden, f.getName()).exists());
		for (File f : golden.listFiles()) {
			File gen = new File(jsto, f.getName());
			assertTrue("There is no generated file for the golden " + f, gen.exists());
			String goldhash = Crypto.hash(f);
			String genhash = Crypto.hash(gen);
			if (!goldhash.equals(genhash)) {
				// TODO: should do a visual line by line diff here ...
				System.out.println("Need visual diff");
			}
			assertEquals("Files " + f + " and " + gen + " differed", goldhash, genhash);
		}
	}

	private static void clean(File dir) {
		FileUtils.cleanDirectory(dir);
		FileUtils.assertDirectory(dir);
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
