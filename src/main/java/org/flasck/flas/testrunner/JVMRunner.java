package org.flasck.flas.testrunner;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.testrunner.JVMRunner.State;
import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.builtin.FLError;
import org.flasck.jvm.fl.AssertFailed;
import org.flasck.jvm.fl.JVMTestHelper;
import org.flasck.jvm.fl.NewDivException;
import org.flasck.jvm.fl.NotMatched;
import org.flasck.jvm.fl.TestHelper;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.reflection.Reflection;

public class JVMRunner extends CommonTestRunner<State>  {
	public class State {
		private final JVMTestHelper helper;
		private final Object inst;

		public State(JVMTestHelper helper, Object inst) {
			this.helper = helper;
			this.inst = inst;
		}
	}

	private final ClassLoader loader;
	private List<Throwable> runtimeErrors = new ArrayList<Throwable>();
	private final Map<String, String> templates;

	public JVMRunner(Configuration config, Repository repository, ClassLoader bcl, Map<String, String> templates) {
		super(config, repository);
		this.loader = bcl;
		this.templates = templates;
	}
	
	@Override
	public void runUnitTest(TestResultWriter pw, UnitTestCase utc) {
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
				pw.pass("JVM", utc.description);
			} catch (WrappedException ex) {
				Throwable e2 = WrappedException.unwrapThrowable(ex);
				if (e2 instanceof AssertFailed) {
					AssertFailed af = (AssertFailed) e2;
					pw.fail("JVM", utc.description);
					pw.println("  expected: " + af.expected);
					pw.println("  actual:   " + af.actual);
				} else if (e2 instanceof NotMatched) {
					pw.fail("JVM", utc.description);
					pw.println("  " + e2.getMessage());
				} else if (e2 instanceof NewDivException) {
					pw.fail("JVM", utc.description);
					pw.println("  " + e2.getMessage());
				} else {
					pw.error("JVM", utc.description, e2);
					pw.println("JVM ERROR " + utc.description);
				}
			} catch (Throwable t) {
				pw.error("JVM", utc.description, t);
			}
		} catch (ClassNotFoundException e) {
			pw.println("NOTFOUND " + utc.description);
			config.errors.message(((InputPosition)null), "cannot find test class " + utc.name.javaName());
		}
	}
	
	
	@Override
	protected State createSystemTest(TestResultWriter pw, SystemTest st) {
		pw.println("JVM running system test " + st.name().uniqueName());
		try {
			JVMTestHelper helper = new JVMTestHelper(loader, templates, runtimeErrors);
			FLEvalContext cxt = helper.create();
			Class<?> clz = Class.forName(st.name().javaName(), false, loader);
			Constructor<?> ctor = clz.getConstructor(TestHelper.class, FLEvalContext.class);
			Object inst = ctor.newInstance(helper, cxt);
			return new State(helper, inst);
		} catch (Throwable t) {
			pw.error("  JVM", "creating " + st.name().uniqueName(), t);
			return null;
		}
	}
	
	@Override
	protected void runSystemTestStage(TestResultWriter pw, State state, SystemTest st, SystemTestStage e) {
		pw.pass(" ", e.desc);
	}
	
	@Override
	protected void cleanupSystemTest(TestResultWriter pw, State state, SystemTest st) {
		pw.println("  " + st.name().uniqueName() + " all tests passed");
	}

}
