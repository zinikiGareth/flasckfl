package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.flasck.jvm.fl.FlasTestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.webfolder.ui4j.api.browser.Page;
import io.webfolder.ui4j.api.util.Ui4jException;
import javafx.application.Platform;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

public class SingleJSTest {
    static final Logger logger = LoggerFactory.getLogger("SingleJSTest");
	private final Page page;
	private final List<String> errors;
	private final TestResultWriter pw;
	final JSTestState state;
	private final String clz;
	private boolean error;
	private JSObject cxt;
	private JSObject testObj;

	public SingleJSTest(Page page, List<String> errors, TestResultWriter pw, String clz, String desc) {
		this.page = page;
		this.errors = errors;
		this.pw = pw;
		this.state = new JSTestState(this);
		this.clz = clz;
	}

	public void create(String desc) {
		uiThread(desc, cdl -> {
			cxt = (JSObject) page.executeScript("window.runner = new window.UTRunner(makeBridge(window.callJava, window.JavaLogger)); window.testcxt = window.runner.newContext();");
			testObj = (JSObject) page.executeScript("new " + clz + "(window.runner, window.testcxt)");
			page.executeScript("window.runner.clear();");
			cdl.countDown();
		});
	}

	public List<String> getSteps(String desc, String name) {
		if (error)
			return new ArrayList<>();
		List<String> steps = new ArrayList<>();
		uiThread(desc, cdl -> {
			logger.debug("calling " + name + ".getSteps(" + cxt + ")");
			Object ua = testObj.call(name, cxt);
			if (ua instanceof JSObject) {
				JSObject arr = (JSObject) ua;
				int len = (Integer)arr.getMember("length");
				for (int i=0;i<len;i++)
					steps.add((String) arr.getMember(Integer.toString(i)));
			}
			cdl.countDown();
		});
		return steps;
	}

	public void step(String desc, String s) {
		if (error)
			return;
		List<Throwable> excs = new ArrayList<>();
		uiThread(desc, cdl -> {
			logger.debug("calling " + desc + " step " + s + "(" + cxt + ")");
			try {
				testObj.call(s, cxt);
			} catch (Throwable t) {
				excs.add(t);
			}
			cdl.countDown();
		});
		handleExceptions(desc, excs);
	}

	public void checkContextSatisfied(String desc) {
		List<Throwable> excs = new ArrayList<>();
		uiThread(desc, cdl -> {
			try {
				page.executeScript("window.runner.checkAtEnd();");
				cxt.call("assertSatisfied");
			} catch (Throwable t) {
				excs.add(t);
			}
			cdl.countDown();
		});
		handleExceptions(desc, excs);
	}

	private void uiThread(String desc, Consumer<CountDownLatch> doit) {
		CountDownLatch cdl = new CountDownLatch(1);
		if (Platform.isFxApplicationThread()) {
			try {
				doit.accept(cdl);
			} catch (Throwable t) {
				t.printStackTrace(System.out);
			}
		} else
			Platform.runLater(() -> {
				try {
					doit.accept(cdl);
				} catch (Throwable t) {
					t.printStackTrace(System.out);
				}
			});
		boolean await = false;
		try {
			await = cdl.await(1, TimeUnit.SECONDS);
		} catch (Throwable t) {
		}
		if (!await) {
			error = true;
			pw.println("JS TIMEOUT " + desc);
		}
	}

	private void handleExceptions(String desc, List<Throwable> excs) {
		if (!excs.isEmpty()) {
			Throwable t = excs.get(0);
			if (state != null)
				state.failed++;
			if (t instanceof Ui4jException)
				t = t.getCause();
			if (t instanceof JSException) {
				JSException ex = (JSException) t;
				if (ex.getCause() instanceof FlasTestException) {
					JVMRunner.handleError("JS", errors, pw, null, desc, ex.getCause());
					return;
				} else {
					String jsex = ex.getMessage();
					if (jsex.startsWith("Error: NSV\n")) {
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
			}
			pw.error("JS", desc, t);
			errors.add("JS ERROR " + (desc == null ? "configure":desc));
		}
	}
	
	public boolean ok() {
		return !error && state.failed == 0;
	}
}
