package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleJSTest {
	/*
    public static final class BindModule {
		private CountDownLatch cdl;

		public BindModule(CountDownLatch cdl) {
			this.cdl = cdl;
		}

		public void bind(String name, JSObject obj) {
			System.out.println(name + ": " + obj);
			if (cdl != null)
				cdl.countDown();
		}
		
		public void fail(String name, String msg) {
			System.out.println(name + ": failed - " + msg);
			if (cdl != null)
				cdl.countDown();
		}
	}
	*/

	static final Logger logger = LoggerFactory.getLogger("SingleJSTest");
//	private final Page page;
//	private final List<String> errors;
//	private final TestResultWriter pw;
	final JSTestState state;
//	private final String clz;
//	private boolean error = true;
//	private JSObject cxt;
//	private JSObject testObj;

	public SingleJSTest(/* Page page, List<String> errors, TestResultWriter pw, String clz, String desc */) {
//		this.page = page;
//		this.errors = errors;
//		this.pw = pw;
		this.state = new JSTestState(this);
//		this.clz = clz;
	}

	public void create(String desc) {
		/*
		uiThread(desc, 2, cdl -> {
			JSObject w = (JSObject) page.executeScript("window");
			w.setMember("callMe", new BindModule(cdl));
//			page.executeScript("window.callMe.accept('hello');");
			page.executeScript("import('./js/test.golden._ut_different.js').then(function(mod) { window.callMe.bind('ut', mod.ut); }, function(reason) { window.callMe.fail('ut', 'rejected: ' + reason); });");
//			page.executeScript("import('./js/flasjs.js').then(function(mod) { window.callMe.bind('flas', mod); }, function(reason) { window.callMe.fail('flas', 'rejected: ' + reason); });");
			page.executeScript("import('./js/flastest.js').then(function(mod) { window.callMe.bind('test', mod.tests); }, function(reason) { window.callMe.fail('test', 'rejected: ' + reason); });");
//			cxt = (JSObject) page.executeScript("window.runner = new window.UTRunner(makeBridge(window.callJava, window.JavaLogger)); window.testcxt = window.runner.newContext();");
//			testObj = (JSObject) page.executeScript("new " + clz + "(window.runner, window.testcxt)");
//			page.executeScript("window.runner.clear();");
//			cdl.countDown();
		});
//		uiThread(desc, cdl -> {
//			Object cc = page.executeScript("window.bar");
//			System.out.println(cc);
//		});
 */
	}

	public List<String> getSteps(String desc, String name) {
//		if (error)
			return new ArrayList<>();
		/*
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
		*/
	}

	/*
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
		uiThread(desc, 1, doit);
	}
	
	private void uiThread(String desc, int cnt, Consumer<CountDownLatch> doit) {
		CountDownLatch cdl = new CountDownLatch(cnt);
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
	 */
	
	public boolean ok() {
		return true; // !error && state.failed == 0;
	}
}
