package org.flasck.flas.testrunner;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.Repository;
import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.builtin.FLError;
import org.flasck.jvm.fl.AssertFailed;
import org.flasck.jvm.fl.JVMTestHelper;
import org.flasck.jvm.fl.NotMatched;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.reflection.Reflection;

public class JVMRunner extends CommonTestRunner  {
	private final ClassLoader loader;
	private List<Throwable> runtimeErrors = new ArrayList<Throwable>();
	private final Map<String, String> templates;

	public JVMRunner(Configuration config, Repository repository, ClassLoader bcl, Map<String, String> templates) {
		super(config, repository);
		this.loader = bcl;
		this.templates = templates;
	}
	
	@Override
	public void runit(PrintWriter pw, UnitTestCase utc) {
		try {
			JVMTestHelper helper = new JVMTestHelper(loader, templates, runtimeErrors);
			FLEvalContext cxt = helper.create();
			Class<?> tc = Class.forName(utc.name.javaName(), false, loader);
			try {
				Object result = Reflection.callStatic(tc, "dotest", helper, cxt);
				if (result instanceof FLError)
					throw (Throwable)result;
				if (cxt.getError() != null)
					throw cxt.getError();
				pw.println("JVM PASS " + utc.description);
			} catch (WrappedException ex) {
				Throwable e2 = WrappedException.unwrapThrowable(ex);
				if (e2 instanceof AssertFailed) {
					AssertFailed af = (AssertFailed) e2;
					pw.println("JVM FAIL " + utc.description);
					pw.println("  expected: " + af.expected);
					pw.println("  actual:   " + af.actual);
				} else if (e2 instanceof NotMatched) {
					pw.println("JVM FAIL " + utc.description);
					pw.println("  " + e2.getMessage());
				} else {
					pw.println("JVM ERROR " + utc.description);
					e2.printStackTrace(pw);
				}
			} catch (Throwable t) {
				pw.println("ERROR " + utc.description);
				t.printStackTrace(pw);
			}
		} catch (ClassNotFoundException e) {
			pw.println("NOTFOUND " + utc.description);
			config.errors.message(((InputPosition)null), "cannot find test class " + utc.name.javaName());
		}
		pw.flush();
	}
}
