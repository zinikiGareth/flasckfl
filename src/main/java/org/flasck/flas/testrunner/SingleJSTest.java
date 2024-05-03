package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.codehaus.jettison.json.JSONException;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.jvm.fl.FlasTestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.sync.LockingCounter;

public class SingleJSTest {
	static final Logger logger = LoggerFactory.getLogger("SingleJSTest");
	static String patienceChild = System.getProperty("org.flasck.patience.child");
	boolean wantTimeout = patienceChild == null || !patienceChild.equals("true");
	private BrowserJSJavaBridge bridge;
	private final LockingCounter counter;
	private final List<String> errors;
	private final TestResultWriter pw;
	private final NameOfThing utn;
	final JSTestState state;
	private boolean error = false;

	public SingleJSTest(BrowserJSJavaBridge bridge, LockingCounter counter, List<String> errors, TestResultWriter pw, NameOfThing name) {
		this.bridge = bridge;
		this.counter = counter;
		this.errors = errors;
		this.pw = pw;
		this.utn = name;
		this.state = new JSTestState(this);
	}

	public void create(CountDownLatch cdl) throws JSONException, InterruptedException {
		if (utn instanceof UnitTestName)
			bridge.prepareUnitTest(utn.container(), utn.baseName());
		else
			bridge.prepareSystemTest(utn);
		if (wantTimeout) {
			boolean isReady = cdl.await(25, TimeUnit.SECONDS);
			if (!isReady)
				throw new CantHappenException("the test steps were not made available");
		} else {
			cdl.await();
		}
	}

	public void prepareStage(CountDownLatch cdl, SystemTestStage e) throws JSONException, InterruptedException {
		logger.info("prepareStage: cdl = " + cdl.getCount());
		bridge.prepareStage(e.name.baseName());
		boolean isReady = cdl.await(25, TimeUnit.SECONDS);
		logger.info("prepareStage: cdl = " + cdl.getCount() + " isr = " + isReady);
		if (!isReady)
			throw new CantHappenException("the test steps were not made available");
	}

	public void step(String desc, String s) {
		if (error)
			return;
		List<Throwable> excs = new ArrayList<>();
		logger.info("calling " + desc + " step " + s);
		try {
			counter.start();
			runStep(s);
			counter.waitForZero(15000);
			logger.info("step " + s + " has finished");
		} catch (JSCaughtException t) {
			excs.add(t);
			logger.warn("step " + s + " failed with " + t.getMessage());
		} catch (Throwable t) {
			logger.warn("Error waiting for test to end", t);
			pw.error("JS", desc, t);
			errors.add("JS ERROR " + (desc == null ? "configure":desc));
		}
		handleExceptions(desc, excs);
	}

	public void runStep(String step) throws InterruptedException, JSONException, TimeoutException {
		logger.info("running step " + step);
		bridge.runStep(step);
	}

	public void checkContextSatisfied(String desc) {
		List<Throwable> excs = new ArrayList<>();
		try {
			counter.start();
			bridge.checkContextSatisfied();
			counter.waitForZero(15000);
		} catch (JSCaughtException t) {
			excs.add(t);
		} catch (Throwable t) {
			logger.warn("Error waiting for test to end", t);
			pw.error("JS", desc, t);
			errors.add("JS ERROR " + (desc == null ? "configure":desc));
		}
		handleExceptions(desc, excs);
	}

	private void handleExceptions(String desc, List<Throwable> excs) {
		if (!excs.isEmpty()) {
			Throwable t = excs.get(0);
			if (state != null)
				state.failed++;
			if (t instanceof FlasTestException) {
				JVMRunner.handleError("JS", errors, pw, null, desc, t);
				return;
			} else if (t instanceof TimeoutException) {
				pw.fail("JS", "step timed out");
			} else {
				String jsex = t.getMessage();
				if (jsex == null) {
					pw.error("JS", "unknown exception", t);
				} else if (jsex.startsWith("Error: NSV\n")) {
					pw.fail("JS", desc);
					errors.add("JS FAIL " + desc);
					pw.println(jsex.substring(jsex.indexOf('\n')+1));
					return;
				} else if (jsex.startsWith("Error: EXP\n")) {
					pw.fail("JS", desc);
					errors.add("JS FAIL " + desc);
					pw.println(jsex.substring(jsex.indexOf('\n')+1));
					return;
				} else if (jsex.startsWith("Error: UNUSED\n")) {
					pw.fail("JS", desc);
					errors.add("JS FAIL " + desc);
					pw.println("  Expectation not called: " + jsex.substring(jsex.indexOf('\n')+3));
					return;
				} else if (jsex.startsWith("Error: EXPCAN\n")) {
					pw.fail("JS", desc);
					errors.add("JS FAIL " + desc);
					pw.println(jsex.substring(jsex.indexOf('\n')+1));
					return;
				} else if (jsex.startsWith("Error: UECAN\n")) {
					pw.fail("JS", desc);
					errors.add("JS FAIL " + desc);
					pw.println(jsex.substring(jsex.indexOf('\n')+1));
					return;
				} else if (jsex.startsWith("Error: MATCH\n")) {
					pw.fail("JS", desc);
					errors.add("JS FAIL " + desc);
					pw.println(jsex.substring(jsex.indexOf('\n')+1));
					return;
				} else if (jsex.startsWith("Error: NEWDIV\n")) {
					pw.fail("JS", desc);
					errors.add("JS FAIL " + desc);
					pw.println("incorrect number of divs created");
					pw.println(jsex.substring(jsex.indexOf('\n')+1));
					return;
				} else if (jsex.startsWith("Error: NOHDLR\n")) {
					pw.fail("JS", desc);
					errors.add("JS FAIL " + desc);
					pw.println(jsex.substring(jsex.indexOf('\n')+1));
					return;
				}
			}
			pw.error("JS", desc, t);
			errors.add("JS ERROR " + (desc == null ? "configure":desc));
		}
	}
	
	public boolean ok() {
		return !error && state.failed == 0;
	}
}
