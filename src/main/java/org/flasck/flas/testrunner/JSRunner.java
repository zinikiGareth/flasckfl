package org.flasck.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.flasck.flas.Configuration;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.repository.Repository;
import org.flasck.jvm.FLEvalContext;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.utils.FileUtils;

import io.webfolder.ui4j.api.browser.BrowserEngine;
import io.webfolder.ui4j.api.browser.BrowserFactory;
import io.webfolder.ui4j.api.browser.Page;
import io.webfolder.ui4j.api.util.Ui4jException;
import javafx.application.Platform;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

public class JSRunner extends CommonTestRunner {
	AtomicInteger pendingAsyncs = new AtomicInteger(0);

	public class JSJavaBridge {
		public void error(String s) {
			errors.add(s);
		}
		
		public void log(String s) {
			logger.info(s);
		}

		public void callAsync(final JSObject fn) {
			pendingAsyncs.incrementAndGet();
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					try {
						fn.eval("this.f()");
						if (pendingAsyncs.decrementAndGet() == 0) {
							synchronized(pendingAsyncs) {
								pendingAsyncs.notifyAll();
							}
						}
					} catch (Throwable t) {
						t.printStackTrace();
						errors.add(t.getMessage());
					}
				}
			});
		}
	}

	/*
	// Because of a bug in JDK8 (https://bugs.openjdk.java.net/browse/JDK-8088751), it's not possible
	// to have the MockService directly implement both process methods and share that with JS/Webkit
	// Thus we have a wrapper here to deal with the impedance mismatch
	public class MockServiceWrapper {
		public String _myAddr; // JSRunner depends on this; JVMRunner probably should need it as well :-)
		private MockService wraps;

		public MockServiceWrapper(String name) {
			wraps = new MockService(name, errors, invocations, expectations);
		}
		
		public void process(final JSObject msg) {
			wraps.process(msg);
		}
	}
	*/
	
	private final JSStorage jse;
	private final JSJavaBridge st = new JSJavaBridge();
	private final BrowserEngine browser;
	private Page page;
	private File html;
	private boolean useCachebuster = true;
	private String jstestdir;
	private String specifiedTestName;
	
	public JSRunner(Configuration config, Repository repository, JSStorage jse, Map<String, String> templates) {
		super(config, repository);
		if (config != null) {
			this.jstestdir = config.jsTestDir();
			this.specifiedTestName = config.specifiedTestName;
		} else {
			this.jstestdir = System.getProperty("user.dir");
			this.specifiedTestName = null;
		}
		this.jse = jse;
		this.browser = BrowserFactory.getWebKit();

		// TODO: I'm not sure how much more of this is actually per-package and how much is "global"
		buildHTML(templates);
		page = browser.navigate("file:" + html.getPath());
		CountDownLatch cdl = new CountDownLatch(1);
		Platform.runLater(() -> {
			JSObject win = (JSObject)page.executeScript("window");
			win.setMember("callJava", st);
			cdl.countDown();
		});
		boolean await = false;
		try {
			await = cdl.await(1, TimeUnit.SECONDS);
		} catch (Throwable t) {
		}
		if (!await) {
			throw new RuntimeException("Whole test failed to initialize");
		}
	}

	@Override
	public void preparePackage(PrintWriter pw, UnitTestPackage e) {
	}
	
	// currently untested due to browser issues
	@Override
	public void runit(PrintWriter pw, UnitTestCase utc) {
		CountDownLatch cdl = new CountDownLatch(1);
		Platform.runLater(() -> {
			try {
				Object isdf = page.executeScript("typeof(" + utc.name.container().jsName() + ")");
				if (!"undefined".equals(isdf))
					isdf = page.executeScript("typeof(" + utc.name.jsName() + ")");
				if ("function".equals(isdf)) {
					page.executeScript(utc.name.jsName() + "(new window.UTRunner(window.JavaLogger))");
					pw.println("JS PASS " + utc.description);
				}
				cdl.countDown();
			} catch (Throwable t) {
				if (t instanceof Ui4jException)
					t = t.getCause();
				if (t instanceof JSException) {
					JSException ex = (JSException) t;
					String jsex = ex.getMessage();
					if (jsex.startsWith("Error: NSV\n")) {
						pw.println("JS FAIL " + utc.description);
						pw.println(jsex.substring(jsex.indexOf('\n')+1));
						cdl.countDown();
						return;
					} else if (jsex.startsWith("Error: EXP\n")) {
						pw.println("JS FAIL " + utc.description);
						pw.println(jsex.substring(jsex.indexOf('\n')+1));
						cdl.countDown();
						return;
					}
				}
				pw.println("JS ERROR " + utc.description);
				t.printStackTrace(pw);
				cdl.countDown();
			}
		});
		boolean await = false;
		try {
			await = cdl.await(1, TimeUnit.SECONDS);
		} catch (Exception ex) {
		}
		if (!await) {
			pw.println("JS TIMEOUT " + utc.description);
		}
	}

	@Override
	public String name() {
		return "js";
	}
	
	private void buildHTML(Map<String, String> templates) {
		try {
			String testName;
			String testDir;
			if (specifiedTestName != null) {
				testName = config.specifiedTestName;
				testDir = jstestdir + "/html/" + specifiedTestName;
			} else {
				testName = "test";
				testDir = jstestdir + "/html";
			}
			String testDirJS = testDir + "/js";
			FileUtils.assertDirectory(new File(testDirJS));
			html = new File(testDir, testName + ".html");
			PrintWriter pw = new PrintWriter(html);
			pw.println("<!DOCTYPE html>");
			pw.println("<html>");
			pw.println("<head>");
			for (Entry<String, String> e : templates.entrySet())
				renderTemplate(pw, e.getKey(), e.getValue());

			// probably wants to be config :-)
			copyResourceIntoScript(pw, "javalogger.js", testDirJS);
			copyResourceIntoScript(pw, "ziwsh.js", testDirJS);
			copyResourceIntoScript(pw, "flas-runtime.js", testDirJS);
			copyResourceIntoScript(pw, "flas-unittest.js", testDirJS);
			for (File f : jse.files())
				includeFileAsScript(pw, f, testDirJS);
			pw.println("</head>");
			pw.println("<body>");
			pw.println("</body>");
			pw.println("</html>");
			pw.close();
//			System.out.println("Loading " + html);
//			FileUtils.cat(html);
		} catch (IOException ex) {
			throw WrappedException.wrap(ex);
		}
	}

	private void copyResourceIntoScript(PrintWriter pw, String resource, String testDir) {
		File to = new File(testDir, resource);
		InputStream is = this.getClass().getResourceAsStream("/flasck/" + resource);
		if (is == null) {
			errors.add("Could not copy resource " + resource);
			return;
		}
		FileUtils.copyStreamToFile(is, to);
		pw.println("<script src='file:" + to.getPath() + "' type='text/javascript'></script>");
	}

	private void renderTemplate(PrintWriter pw, String name, String template) {
		pw.println("<template id='" + name.replace(".html", "") + "'>");
		pw.println(template);
		pw.println("</template>");
	}

	protected void includeFileAsScript(PrintWriter pw, File f, String testDir) {
		File to = new File(testDir, f.getName());
		if (!f.isAbsolute())
			f = new File(new File(System.getProperty("user.dir")), f.getPath());
		FileUtils.copy(f, to);
		String path = to.getPath();
		if (useCachebuster)
			path += "?cachebuster=" + System.currentTimeMillis();
		pw.println("<script src='file:" + path + "' type='text/javascript'></script>");
	}

	@Override
	public void invoke(FLEvalContext cx, Object sendExpr) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public void send(FLEvalContext cxt, Object to, String contract, String meth, Object... args) {
		throw new NotImplementedException();
	}

//	@Override
//	public void match(HTMLMatcher matcher, String selector) throws NotMatched {
//		logger.info("Matching " + selector);
////		System.out.println(page.getDocument().getBody().getOuterHTML());
////		matcher.match(selector, page.getDocument().queryAll(selector).stream().map(e -> new UI4JWrapperElement(page, e)).collect(Collectors.toList()));
//		assertNoErrors();
//	}

	@Override
	public void match(FLEvalContext cx, Object target, String selector, boolean contains, String matches) {
	}

	protected JSObject getVar(String var) {
		return (JSObject)page.executeScript(var);
	}

	protected void execute(String instr) {
		JSObject err = (JSObject)page.executeScript("_tmp_error = null; try { " + instr + " } catch (err) { _tmp_error = err; }; _tmp_error;");
		if (err != null)
			throw new UtilException("Error processing javascript: " + err);
	}

	@Override
	public void event(FLEvalContext cx, Object card, Object event) throws Exception {
//		logger.info("Sending event " + methodName + " to " +  cardVar);
//		CardHandle card = cards.get(cardVar);
//		AtomicBoolean choke = new AtomicBoolean(false);
//		Platform.runLater(() -> {
//			// Or else just call "dispatchEvent()" on wrapper
//			// But where do I find wrapper?
//			// cf JVMRunner, where we call the hanlde and get it to do the dirty work
//			// is that possible in this world?
//			Object events = card.card.call(methodName);
//			card.wrapper.call("evalAndProcess", events);
//			choke.set(true);
//		});
//		waitForChoke(choke);
//		processEvents();
//		assertNoErrors();
//		assertAllInvocationsCalled();
	}

//	@Override
//	public void click(String selector) {
//		List<Element> elts = page.getDocument().queryAll(selector);
//		if (elts.isEmpty())
//			throw new UtilException("No elements matched " + selector);
//		else if (elts.size() > 1)
//			throw new UtilException("Multiple elements matched " + selector);
//		Element e = elts.get(0);
//		if (!e.hasAttribute("onclick")) {
//			String html;
//			try {
//				html = e.getOuterHTML();
//			} catch (Throwable t) {
//				html = "";
//			}
//			throw new UtilException("There is no 'onclick' attribute on " + selector + html);
//		}
//		e.click();
//		processEvents();
//		assertAllInvocationsCalled();
//	}

	/* There was an exception here telling me to do this some other way,
	 * but I'm not sure what the problem was - although the code didn't compile.
	 * I fixed the compilation problems, but kept the same logic.
	 * 
	 * If that's good enough, sobeit. Otherwise, fix this when it breaks or at
	 * least add a really good comment.
	 */
//	private void processEvents() {
//		Date waitUntil = new Date(new Date().getTime() + 5000);
//		while (pendingAsyncs.get() != 0)
//			try {
//				if (new Date().after(waitUntil))
//					throw new RuntimeException("After 5s still have " + pendingAsyncs.get() +" pending asyncs");
//				synchronized (pendingAsyncs) {
//					pendingAsyncs.wait(1000);
//				}
//			} catch (InterruptedException ex) {
//				throw new UtilException("timed out waiting for pending async");
//			}
//	}
//
//	private void waitForChoke(AtomicBoolean choke) {
//		for (int i=0;!choke.get() && i<100;i++) {
//			try { Thread.sleep(10); } catch (InterruptedException ex) {}
//		}
//		if (!choke.get())
//			fail("did not manage to run platform code");
//	}
}
