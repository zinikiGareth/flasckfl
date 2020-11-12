package org.flasck.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.flasck.flas.Configuration;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.Repository;
import org.ziniki.ziwsh.intf.JsonSender;
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

public class JSRunner extends CommonTestRunner<JSTestState> {
	public class JSJavaBridge {
		public void error(String s) {
			errors.add(s);
		}
		
		public void log(String s) {
			System.out.println(s);
			logger.info(s);
		}
		
		public Object module(String s) {
			try {
				Class<?> clz = Class.forName(s);
				if (!modules.containsKey(clz)) {
					modules.put(clz, clz.getConstructor(JSJavaBridge.class, ClassLoader.class).newInstance(this, classloader));
				}
				return modules.get(clz);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
				throw WrappedException.wrap(e);
			}
		}

		public void transport(JsonSender toZiniki) {
			if (Platform.isFxApplicationThread()) {
				JSObject runner = (JSObject) page.executeScript("window.utrunner");
				runner.call("transport", toZiniki);
			} else
				throw new RuntimeException("Could not pass transport to JS: not in FX thread");
		}

		public void sendJson(String json) {
			System.out.println("sending " + json + " with " + counter);
			if (Platform.isFxApplicationThread()) {
				doSend(json);
			} else {
				uiThread(cdl -> {
					doSend(json);
					cdl.countDown();
				});
			}
		}

		private void doSend(String json) {
			JSObject runner = (JSObject) page.executeScript("window.utrunner");
			runner.call("deliver", json);
		}
		
		public void lock() {
			int cnt = counter.incrementAndGet();
			System.out.println("locked; counter = " + cnt);
		}
		
		public void unlock() {
			synchronized (counter) {
				if (counter.decrementAndGet() == 0)
					counter.notify();
				System.out.println("unlock counted down to " + counter.get());
			}
		}

		public AtomicInteger getTestCounter() {
			return counter;
		}
	}

	private final JSStorage jse;
	private final JSJavaBridge st = new JSJavaBridge();
	private final BrowserEngine browser;
	private final Map<Class<?>, Object> modules = new HashMap<>();
	private final ClassLoader classloader;
	private Page page;
	private File html;
	private boolean useCachebuster = false;
	private String jstestdir;
	private String specifiedTestName;
	private final AtomicInteger counter = new AtomicInteger(0);
	
	public JSRunner(Configuration config, Repository repository, JSStorage jse, Map<String, String> templates, ClassLoader cl) {
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
		this.classloader = cl;

		// TODO: I'm not sure how much more of this is actually per-package and how much is "global"
		buildHTML(templates);
		page = browser.navigate("file:" + html.getPath());
		boolean await = uiThread(cdl -> {
			JSObject win = (JSObject)page.executeScript("window");
			win.setMember("callJava", st);
			cdl.countDown();
		});
		if (!await)
			throw new RuntimeException("Whole test failed to initialize");
	}

	private boolean uiThread(Consumer<CountDownLatch> doit) {
		CountDownLatch cdl = new CountDownLatch(1);
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
		return await;
	}

	@Override
	public void runUnitTest(TestResultWriter pw, UnitTestCase utc) {
		String clz = utc.name.jsName();
		String desc = utc.description;
		
		SingleJSTest t1 = new SingleJSTest(page, errors, pw, null, clz, desc);
		t1.create();
		List<String> steps = t1.getSteps("dotest");
		for (String s : steps) {
			t1.step(s);
		}
		if (!steps.isEmpty() && desc != null && t1.ok())
			pw.pass("JS", desc);
//		runStage(pw, utc.description, null, utc.name.container().jsName(), utc.name.jsName(), true);
	}

	
	@Override
	protected JSTestState createSystemTest(TestResultWriter pw, SystemTest st) {
		String clz = st.name().jsName();
		String desc = null;
		pw.println("JS running system test " + st.name().uniqueName());
		SingleJSTest t1 = new SingleJSTest(page, errors, pw, null, clz, desc);
		t1.create();
//		Object ret = runStage(pw, st.name().uniqueName(), null, st.name().container().jsName(), st.name().jsName(), false);
		return new JSTestState(t1);
	}
	
	@Override
	protected void runSystemTestStage(TestResultWriter pw, JSTestState state, SystemTest st, SystemTestStage e) {
		String desc = e.desc;
		List<String> steps = state.test.getSteps(e.name.baseName());
		for (String s : steps) {
			state.test.step(s);
		}
		if (!steps.isEmpty() && desc != null && state.test.ok())
			pw.pass("JS", desc);
//		runStage(pw, e.desc, state, null, e.name.baseName(), true);
	}

	private Object runStage(TestResultWriter pw, String desc, JSTestState state, String ctr, String fn, boolean isTest) {
		List<Object> rets = new ArrayList<>();
		counter.set(1);
		System.out.println("starting count at " + counter);
		boolean await = uiThread(cdl -> {
			try {
				boolean ran = false;
				Object ret = null;
				if (state != null && state.test != null) {
//					ret = state.test.call(fn);
					ran = true;
				} else {
					Object isdf = page.executeScript("typeof(" + ctr + ")");
					if (!"undefined".equals(isdf))
						isdf = page.executeScript("typeof(" + fn + ")");
					if ("function".equals(isdf)) {
						ret = page.executeScript((!isTest?"new ":"") + fn + "(new window.UTRunner(window.JavaLogger))");
						ran = true;
					}
				}
				int cnt = counter.decrementAndGet();
				System.out.println("method complete: counted down to " + cnt);
				if (ret != null && !"undefined".equals(ret))
					rets.add(ret);
				if (isTest && desc != null && ran)
					pw.pass("JS", desc);
				cdl.countDown();
			} catch (Throwable t) {
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
						int cnt = counter.decrementAndGet();
						System.out.println("method failed: counted down to " + cnt);
						cdl.countDown();
						return;
					} else if (jsex.startsWith("Error: EXP\n")) {
						pw.fail("JS", desc);
						errors.add("JS FAIL " + desc);
						pw.println(jsex.substring(jsex.indexOf('\n')+1));
						int cnt = counter.decrementAndGet();
						System.out.println("method failed: counted down to " + cnt);
						cdl.countDown();
						return;
					} else if (jsex.startsWith("Error: MATCH\n")) {
						pw.fail("JS", desc);
						errors.add("JS FAIL " + desc);
						pw.println(jsex.substring(jsex.indexOf('\n')+1));
						int cnt = counter.decrementAndGet();
						System.out.println("method failed: counted down to " + cnt);
						cdl.countDown();
						return;
					} else if (jsex.startsWith("Error: NEWDIV\n")) {
						pw.fail("JS", desc);
						errors.add("JS FAIL " + desc);
						pw.println("incorrect number of divs created");
						pw.println(jsex.substring(jsex.indexOf('\n')+1));
						int cnt = counter.decrementAndGet();
						System.out.println("method failed: counted down to " + cnt);
						cdl.countDown();
						return;
					}
				}
				pw.error("JS", desc, t);
				errors.add("JS ERROR " + desc);
				int cnt = counter.decrementAndGet();
				System.out.println("method error: counted down to " + cnt);
				cdl.countDown();
			}
		});
		synchronized (counter) {
			try {
				System.out.println("after method complete, have counter at " + counter.get());
				while (counter.get() != 0)
					counter.wait(5000);
			} catch (Throwable t) {
				pw.error("JS", desc, t);
				errors.add("JS ERROR " + desc);
			}
		}
		if (!await) {
			pw.println("JS TIMEOUT " + desc);
		}
		if (rets.isEmpty())
			return null;
		return rets.get(0);
	}
	
	@Override
	protected void cleanupSystemTest(TestResultWriter pw, JSTestState state, SystemTest st) {
		if (state.failed == 0)
			pw.println("JS " + st.name().uniqueName() + " all stages passed");
		else
			pw.println("JS " + st.name().uniqueName() + " " + state.failed + " stages failed");
	}

	private void buildHTML(Map<String, String> templates) {
		try {
			String testName;
			String testDir;
			if (specifiedTestName != null) {
				testName = config.specifiedTestName;
			} else {
				testName = "test";
			}
			List<File> css = null;
			File webdir = new File(jstestdir + "/web");
			if (webdir.exists()) 
				css = FileUtils.findFilesMatching(webdir, "*.css");
			testDir = jstestdir + "/html";
			String testDirJS = testDir + "/js";
			File testDirCSS = new File(testDir + "/css");
			FileUtils.assertDirectory(new File(testDirJS));
			html = new File(testDir, testName + ".html");
			PrintWriter pw = new PrintWriter(html);
			pw.println("<!DOCTYPE html>");
			pw.println("<html>");
			pw.println("<head>");
			if (css != null && !css.isEmpty()) {
				FileUtils.assertDirectory(testDirCSS);
				for (File c : css) {
					FileUtils.copy(c, testDirCSS);
					pw.println("  <link rel='stylesheet' type='text/css' href='" + c.getPath() + "'>");
				}
			}
			for (Entry<String, String> e : templates.entrySet())
				renderTemplate(pw, e.getKey(), e.getValue());

			// probably wants to be config :-)
			List<String> flascklib = Arrays.asList("javalogger.js", "ziwsh.js", "flas-runtime.js", "flas-container.js", "flas-unittest.js");
			for (String s : flascklib)
				copyResourceIntoScript(pw, s, testDirJS);
			for (String s : jse.packages()) {
				File f = jse.fileFor(s);
				if (f != null)
					includeFileAsScript(pw, f, testDirJS);
				else {
					for (File q : config.includeFrom) {
						for (File i : FileUtils.findFilesMatching(q, s + ".js")) {
							if (!flascklib.contains(i.getName()))
								includeFileAsScript(pw, i, testDirJS);
						}
					}
				}
			}
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

	protected JSObject getVar(String var) {
		return (JSObject)page.executeScript(var);
	}

	protected void execute(String instr) {
		JSObject err = (JSObject)page.executeScript("_tmp_error = null; try { " + instr + " } catch (err) { _tmp_error = err; }; _tmp_error;");
		if (err != null)
			throw new UtilException("Error processing javascript: " + err);
	}
}
