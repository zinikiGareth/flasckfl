package org.flasck.flas.testrunner;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.testrunner.CommonTestRunner.CommonState;
import org.flasck.flas.testrunner.JVMRunner.State;
import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.container.CardContext;
import org.flasck.jvm.container.ExpectationException;
import org.flasck.jvm.container.UnexpectedCancelException;
import org.flasck.jvm.container.UnusedExpectationException;
import org.flasck.jvm.fl.AssertFailed;
import org.flasck.jvm.fl.ClientContext;
import org.flasck.jvm.fl.FlasTestException;
import org.flasck.jvm.fl.JVMTestHelper;
import org.flasck.jvm.fl.NewDivException;
import org.flasck.jvm.fl.NotMatched;
import org.flasck.jvm.fl.TestHelper;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.reflection.Reflection;
import org.zinutils.sync.LockingCounter;

public class JVMRunner extends CommonTestRunner<State>  {
	public interface FunctionThrows<T1, T2> {
		T2 apply(T1 in) throws Throwable;
	}

	public class State extends CommonState {
		private final JVMTestHelper helper;
		private final Class<?> clz;
		private final Object inst;
		private final ClientContext cxt;

		public State(JVMTestHelper helper, Class<?> clz, Object inst, ClientContext cxt) {
			this.helper = helper;
			this.clz = clz;
			this.inst = inst;
			this.cxt = cxt;
		}
	}

	private final ClassLoader loader;
	private List<Throwable> runtimeErrors = new ArrayList<Throwable>();
	private final Map<String, String> templates;
	private final LockingCounter counter = new LockingCounter();

	public JVMRunner(Configuration config, Repository repository, ClassLoader bcl, Map<String, String> templates) {
		super(config, repository);
		this.loader = bcl;
		this.templates = templates;
	}
	
	@Override
	public void runUnitTest(TestResultWriter pw, UnitTestCase utc) {
		String desc = utc.description;
		try {
			JVMTestHelper helper = new JVMTestHelper(loader, config.root, templates, runtimeErrors, counter);
			ClientContext cxt = (ClientContext) helper.create();
			helper.clearBody(cxt);
			Object test = Class.forName(utc.name.javaName(), false, loader).getConstructor(TestHelper.class, FLEvalContext.class).newInstance(helper, cxt);
			@SuppressWarnings("unchecked")
			List<String> steps = (List<String>)Reflection.call(test, "dotest", cxt);
			doSteps(pw, helper, null, test, steps, cxt, utc.description);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			pw.println("NOTFOUND " + desc);
			config.errors.message(((InputPosition)null), "cannot find test class " + utc.name.javaName());
		}
	}
	
	@Override
	protected State createSystemTest(TestResultWriter pw, SystemTest st) {
		pw.systemTest("JVM", st);
		try {
			JVMTestHelper helper = new JVMTestHelper(loader, config.root, templates, runtimeErrors, counter);
			ClientContext cxt = (ClientContext) helper.create();
			helper.clearBody(cxt);
			Class<?> clz = Class.forName(st.name().javaName(), false, loader);
			Constructor<?> ctor = clz.getConstructor(TestHelper.class, FLEvalContext.class);
			Object inst = ctor.newInstance(helper, cxt);
			return new State(helper, clz, inst, cxt);
		} catch (Throwable t) {
			pw.error("  JVM", "creating " + st.name().uniqueName(), t);
			return null;
		}
	}
	
	@Override
	protected void runSystemTestStage(TestResultWriter pw, State state, SystemTest st, SystemTestStage e) {
		if (state.failed > 0)
			return;
		try {
			Method method = state.clz.getMethod(e.name.baseName(), FLEvalContext.class);
			@SuppressWarnings("unchecked")
			List<String> steps = (List<String>) method.invoke(state.inst, state.cxt);
			doSteps(pw, state.helper, state, state.inst, steps, state.cxt, e.desc);
		} catch (Throwable t) {
			pw.error("JVM", e.desc, t);
			state.failed++;
		}
	}
	
	@Override
	protected void cleanupSystemTest(TestResultWriter pw, State state, SystemTest st) {
		if (state.failed == 0) {
			pw.passedSystemTest("JVM", st);
		} else {
			pw.println("JVM " + st.name().uniqueName() + " " + state.failed + " stages failed");
		}
	}

	private void doSteps(TestResultWriter pw, JVMTestHelper helper, State state, Object test, List<String> steps, FLEvalContext cxt, String desc) {
		try {
			if (desc != null)
				pw.begin("JVM", desc);
			helper.reset();
			for (String s : steps) {
				if (state != null && state.failed > 0)
					return;
				if (desc != null)
					pw.begin("JVM", desc + ": " + s);
				counter.start();
				Reflection.call(test, s, cxt);
				counter.end(s);
				while (true) {
					counter.waitForZero(5000);
					if (!cxt.getDispatcher().isDone())
						cxt.getDispatcher().waitForQueueDone();
					if (counter.isZero()) {
						logger.debug("counter is still zero; step done");
						break;
					}
				}
			}
			((CardContext)cxt).assertSatisfied();
			if (cxt.getError() != null)
				throw cxt.getError();
			helper.testComplete();
			if (desc != null)
				pw.pass("JVM", desc);
		} catch (Throwable t) {
			handleError("JVM", errors, pw, state, desc, t);
		}
	}

	public static void handleError(String code, List<String> errors, TestResultWriter pw, CommonState state, String desc, Throwable ex) {
		if (desc == null)
			desc = "configure";
		if (state != null)
			state.failed++;
		Throwable e2;
		if (ex instanceof WrappedException || ex instanceof InvocationTargetException || ex instanceof FlasTestException) {
			e2 = WrappedException.unwrapThrowable(ex);
		} else {
			e2 = ex;
		}
		if (e2 instanceof AssertFailed) {
			AssertFailed af = (AssertFailed) e2;
			pw.fail(code, desc);
			errors.add(code + " FAIL " + desc);
			pw.println("  expected: " + valueOf(af.expected));
			pw.println("  actual:   " + valueOf(af.actual));
		} else if (e2 instanceof NotMatched) {
			pw.fail(code, desc);
			errors.add(code + " FAIL " + desc);
			pw.println("  " + e2.getMessage());
		} else if (e2 instanceof NewDivException) {
			pw.fail(code, desc);
			errors.add(code + " FAIL " + desc);
			pw.println("  " + e2.getMessage());
		} else if (e2 instanceof ExpectationException) {
			pw.fail(code, desc);
			errors.add(code + " FAIL " + desc);
			pw.println("  " + e2.getMessage());
		} else if (e2 instanceof UnusedExpectationException) {
			pw.fail(code, desc);
			errors.add(code + " FAIL " + desc);
			pw.println("  " + e2.getMessage());
		} else if (e2 instanceof UnexpectedCancelException) {
			pw.fail(code, desc);
			errors.add(code + " FAIL " + desc);
			pw.println("  " + e2.getMessage());
		} else {
			pw.error(code, desc, e2);
			errors.add(code + " ERROR " + desc);
			pw.println(code + " ERROR " + desc);
		}
	}

	private static Object valueOf(Object val) {
		if (val instanceof Double) {
			double d = (double) val;
			long k = (long) d;
			if (k == d && !Double.isInfinite(d))
				return k;
		}
		return val;
	}
}
