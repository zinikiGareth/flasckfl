package org.flasck.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.flasck.flas.Configuration;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.Repository;
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

public class JSRunner extends CommonTestRunner<JSObject> {
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

	private final JSStorage jse;
	private final JSJavaBridge st = new JSJavaBridge();
	private final BrowserEngine browser;
	private Page page;
	private File html;
	private boolean useCachebuster = false;
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
	public void runUnitTest(TestResultWriter pw, UnitTestCase utc) {
		runStage(pw, utc.description, null, utc.name.container().jsName(), utc.name.jsName(), true);
	}

	private Object runStage(TestResultWriter pw, String desc, JSObject obj, String ctr, String fn, boolean isTest) {
		CountDownLatch cdl = new CountDownLatch(1);
		List<Object> rets = new ArrayList<>();
		Platform.runLater(() -> {
			try {
				boolean ran = false;
				Object ret = null;
				if (obj != null) {
					ret = obj.call(fn);
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
				if (ret != null && !"undefined".equals(ret))
					rets.add(ret);
				if (isTest && desc != null && ran)
					pw.pass("JS", desc);
				cdl.countDown();
			} catch (Throwable t) {
				if (t instanceof Ui4jException)
					t = t.getCause();
				if (t instanceof JSException) {
					JSException ex = (JSException) t;
					String jsex = ex.getMessage();
					if (jsex.startsWith("Error: NSV\n")) {
						pw.fail("JS", desc);
						pw.println(jsex.substring(jsex.indexOf('\n')+1));
						cdl.countDown();
						return;
					} else if (jsex.startsWith("Error: EXP\n")) {
						pw.fail("JS", desc);
						pw.println(jsex.substring(jsex.indexOf('\n')+1));
						cdl.countDown();
						return;
					} else if (jsex.startsWith("Error: MATCH\n")) {
						pw.fail("JS", desc);
						pw.println(jsex.substring(jsex.indexOf('\n')+1));
						cdl.countDown();
						return;
					} else if (jsex.startsWith("Error: NEWDIV\n")) {
						pw.fail("JS", desc);
						pw.println("incorrect number of divs created");
						pw.println(jsex.substring(jsex.indexOf('\n')+1));
						cdl.countDown();
						return;
					}
				}
				pw.error("JS", desc, t);
				cdl.countDown();
			}
		});
		boolean await = false;
		try {
			await = cdl.await(100, TimeUnit.SECONDS);
		} catch (Exception ex) {
		}
		if (!await) {
			pw.println("JS TIMEOUT " + desc);
		}
		if (rets.isEmpty())
			return null;
		return rets.get(0);
	}
	
	@Override
	protected JSObject createSystemTest(TestResultWriter pw, SystemTest st) {
		pw.println("JS running system test " + st.name().uniqueName());
		Object ret = runStage(pw, st.name().uniqueName(), null, st.name().container().jsName(), st.name().jsName(), false);
		if (ret != null && ret instanceof JSObject)
			return (JSObject) ret;
		else
			return null;
	}
	
	@Override
	protected void runSystemTestStage(TestResultWriter pw, JSObject state, SystemTest st, SystemTestStage e) {
		runStage(pw, e.desc, state, null, e.name.baseName(), true);
	}
	
	@Override
	protected void cleanupSystemTest(TestResultWriter pw, JSObject state, SystemTest st) {
		pw.println("JS " + st.name().uniqueName() + " all stages passed");
	}

	private void buildHTML(Map<String, String> templates) {
		try {
			String testName;
			String testDir;
			if (specifiedTestName != null) {
				testName = config.specifiedTestName;
				testDir = jstestdir + "/html";
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
