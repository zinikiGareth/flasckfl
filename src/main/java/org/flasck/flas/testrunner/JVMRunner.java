package org.flasck.flas.testrunner;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
	public interface FunctionThrows<T1, T2> {
		T2 apply(T1 in) throws Throwable;
	}

	public class State {
		private final JVMTestHelper helper;
		private final Class<?> clz;
		private final Object inst;
		private int failed = 0;

		public State(JVMTestHelper helper, Class<?> clz, Object inst) {
			this.helper = helper;
			this.clz = clz;
			this.inst = inst;
		}
	}

	private final ClassLoader loader;
	private List<Throwable> runtimeErrors = new ArrayList<Throwable>();
	private final Map<String, String> templates;
	private final AtomicInteger counter = new AtomicInteger(0);

	public JVMRunner(Configuration config, Repository repository, ClassLoader bcl, Map<String, String> templates) {
		super(config, repository);
		this.loader = bcl;
		this.templates = templates;
	}
	
	@Override
	public void runUnitTest(TestResultWriter pw, UnitTestCase utc) {
		String desc = utc.description;
		try {
			JVMTestHelper helper = new JVMTestHelper(loader, templates, runtimeErrors, counter);
			Class<?> tc = Class.forName(utc.name.javaName(), false, loader);
			runStepsFunction(pw, desc, null, helper, cxt -> Reflection.callStatic(tc, "dotest", helper, cxt));
		} catch (ClassNotFoundException e) {
			pw.println("NOTFOUND " + desc);
			config.errors.message(((InputPosition)null), "cannot find test class " + utc.name.javaName());
		}
	}
	
	@Override
	protected State createSystemTest(TestResultWriter pw, SystemTest st) {
		pw.println("JVM running system test " + st.name().uniqueName());
		try {
			JVMTestHelper helper = new JVMTestHelper(loader, templates, runtimeErrors, counter);
			Class<?> clz = Class.forName(st.name().javaName(), false, loader);
			Constructor<?> ctor = clz.getConstructor(TestHelper.class);
			Object inst = ctor.newInstance(helper);
			return new State(helper, clz, inst);
		} catch (Throwable t) {
			pw.error("  JVM", "creating " + st.name().uniqueName(), t);
			return null;
		}
	}
	
	@Override
	protected void runSystemTestStage(TestResultWriter pw, State state, SystemTest st, SystemTestStage e) {
		try {
			Method method = state.clz.getMethod(e.name.baseName());
			runStepsFunction(pw, e.desc, state, state.helper, cxt -> method.invoke(state.inst));
		} catch (Throwable t) {
			pw.error("JVM", e.desc, t);
			state.failed++;
		}
	}
	
	@Override
	protected void cleanupSystemTest(TestResultWriter pw, State state, SystemTest st) {
		if (state.failed == 0) {
			pw.println("JVM " + st.name().uniqueName() + " all stages passed");
		} else {
			pw.println("JVM " + st.name().uniqueName() + " " + state.failed + " stages failed");
		}
	}

	private void runStepsFunction(TestResultWriter pw, String desc, State state, JVMTestHelper helper, FunctionThrows<FLEvalContext, Object> doit) {
		try {
			FLEvalContext cxt = helper.create();
			Object result = doit.apply(cxt);
			synchronized (counter) {
				System.out.println("after method complete, have counter at " + counter.get());
				while (counter.get() != 0 || !cxt.getDispatcher().isDone()) {
					if (counter.get() != 0)
						counter.wait(5000);
					if (!cxt.getDispatcher().isDone())
						cxt.getDispatcher().waitForQueueDone();
				}
			}
			if (result instanceof FLError)
				throw (Throwable)result;
			if (cxt.getError() != null)
				throw cxt.getError();
			if (desc != null)
				pw.pass("JVM", desc);
		} catch (WrappedException | InvocationTargetException ex) {
			ex.printStackTrace(System.out);
			if (state != null)
				state.failed++;
			Throwable e2 = WrappedException.unwrapThrowable(ex);
			if (e2 instanceof AssertFailed) {
				AssertFailed af = (AssertFailed) e2;
				pw.fail("JVM", desc);
				errors.add("JVM FAIL " + desc);
				pw.println("  expected: " + valueOf(af.expected));
				pw.println("  actual:   " + valueOf(af.actual));
			} else if (e2 instanceof NotMatched) {
				pw.fail("JVM", desc);
				errors.add("JVM FAIL " + desc);
				pw.println("  " + e2.getMessage());
			} else if (e2 instanceof NewDivException) {
				pw.fail("JVM", desc);
				errors.add("JVM FAIL " + desc);
				pw.println("  " + e2.getMessage());
			} else {
				pw.error("JVM", desc, e2);
				errors.add("JVM ERROR " + desc);
				pw.println("JVM ERROR " + desc);
			}
		} catch (Throwable t) {
			if (state != null)
				state.failed++;
			pw.error("JVM", desc, t);
		}
	}

	private Object valueOf(Object val) {
		if (val instanceof Double) {
			double d = (double) val;
			long k = (long) d;
			if (k == d && !Double.isInfinite(d))
				return k;
		}
		return val;
	}
}
