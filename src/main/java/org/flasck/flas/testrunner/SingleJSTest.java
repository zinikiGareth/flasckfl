package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.webfolder.ui4j.api.browser.Page;
import io.webfolder.ui4j.api.util.Ui4jException;
import javafx.application.Platform;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

public class SingleJSTest {
	private final Page page;
	private final List<String> errors;
	private final TestResultWriter pw;
	private final JSTestState state;
	private final String clz;
	private final String desc;
	private boolean error;
	private JSObject cxt;
	private JSObject testObj;

	public SingleJSTest(Page page, List<String> errors, TestResultWriter pw, JSTestState state, String clz, String desc) {
		this.page = page;
		this.errors = errors;
		this.pw = pw;
		this.state = state;
		this.clz = clz;
		this.desc = desc;
	}

	public void create() {
		uiThread(cdl -> {
			cxt = (JSObject) page.executeScript("window.runner = new window.UTRunner(window.JavaLogger); window.testcxt = window.runner.newContext();");
			testObj = (JSObject) page.executeScript("new " + clz + "(window.runner, window.testcxt)");
			cdl.countDown();
		});
	}

	public List<String> getSteps() {
		if (error)
			return new ArrayList<>();
		List<String> steps = new ArrayList<>();
		uiThread(cdl -> {
			JSObject arr = (JSObject) testObj.call("dotest", cxt);
			int len = (Integer)arr.getMember("length");
			for (int i=0;i<len;i++)
				steps.add((String) arr.getMember(Integer.toString(i)));
			cdl.countDown();
		});
		return steps;
	}

	public void step(String s) {
		if (error)
			return;
		System.out.println("running js step " + s);
		List<Throwable> excs = new ArrayList<>();
		uiThread(cdl -> {
			try {
				testObj.call(s, cxt);
				cdl.countDown();
			} catch (Throwable t) {
				excs.add(t);
			}
		});
		if (!excs.isEmpty()) {
			Throwable t = excs.get(0);
			if (state != null)
				state.failed++;
			if (t instanceof Ui4jException)
				t = t.getCause();
			if (t instanceof JSException) {
				JSException ex = (JSException) t;
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
				}
			}
			pw.error("JS", desc, t);
			errors.add("JS ERROR " + desc);
		}
	}
	
	private void uiThread(Consumer<CountDownLatch> doit) {
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

	public boolean ok() {
		return !error;
	}
}
