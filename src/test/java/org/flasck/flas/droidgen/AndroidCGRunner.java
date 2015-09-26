package org.flasck.flas.droidgen;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.flasck.flas.Compiler;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.ByteCodeFile;
import org.zinutils.cgharness.CGHClassLoaderImpl;
import org.zinutils.cgharness.CGHarnessRunner;
import org.zinutils.utils.FileUtils;

public class AndroidCGRunner extends CGHarnessRunner {
	public AndroidCGRunner(Class<?> klass, RunnerBuilder builder) throws InitializationError, FileNotFoundException {
		super(builder, figureClasses());
	}
	
	private static Class<?>[] figureClasses() throws FileNotFoundException {
		LogManager.getLogger("TypeChecker").setLevel(Level.WARN);
		Compiler compiler = new Compiler();
		compiler.writeDroidTo(new File("/tmp"));
		compiler.searchIn(new File("src/main/resources/flim"));
		compiler.compile(new File("src/test/resources/cards/test.ziniki"));

		CGHClassLoaderImpl zcl = new CGHClassLoaderImpl();
		List<Class<?>> ret = new ArrayList<Class<?>>();

		ByteCodeEnvironment bce = compiler.getBCE();
		compare(bce, zcl, ret, "test.ziniki.CounterObj");
//		compare(bce, zcl, ret, "test.ziniki.CounterCard");
		compare(bce, zcl, ret, "test.ziniki.CounterCard$B1");
		compare(bce, zcl, ret, "test.ziniki.CounterCard$_C0");
		compare(bce, zcl, ret, "test.ziniki.CounterCard$_C1");
		compare(bce, zcl, ret, "test.ziniki.CounterCard$CountUp");
		return ret.toArray(new Class<?>[ret.size()]);
	}

	private static void compare(ByteCodeEnvironment bce, CGHClassLoaderImpl zcl, List<Class<?>> ret, String clz) {
		try {
			byte[] bs = FileUtils.readAllStream(new FileInputStream("/Users/gareth/user/Personal/Projects/Android/HelloAndroid/qbout/classes/" + FileUtils.convertDottedToSlashPath(clz) + ".class"));
			ByteCodeFile bcf = new ByteCodeFile(new ByteArrayInputStream(bs));
			expected.put(clz, bs);
		} catch (FileNotFoundException ex) {
			// it doesn't exist so won't be found later ...
		}

		// Need to copy "everything" across from BCE to holder ... or else bind the two to begin with
		ByteCodeCreator bcc = bce.get(clz);
		if (bcc != null)
			holder.addEntry(bcc.getCreatedName(), bcc);

		// And build the test cases
		Class<?> testClass = testClass(zcl, bce, clz, false);
		ret.add(testClass);
	}

	@Override
	protected void cleanUp() {
		// compiler.destroy();
	}

	@Override
	protected String getName() {
		return "Android Generation Tests";
	}
}
