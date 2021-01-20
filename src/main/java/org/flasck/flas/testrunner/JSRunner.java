package org.flasck.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.flasck.flas.Configuration;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.Repository;
import org.flasck.jvm.ziniki.ContentObject;
import org.ziniki.ziwsh.intf.JsonSender;
import org.zinutils.exceptions.UtilException;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.reflection.Reflection;
import org.zinutils.sync.LockingCounter;
import org.zinutils.utils.FileUtils;

import io.webfolder.ui4j.api.browser.BrowserEngine;
import io.webfolder.ui4j.api.browser.BrowserFactory;
import io.webfolder.ui4j.api.browser.Page;
import javafx.application.Platform;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

public class JSRunner extends CommonTestRunner<JSTestState> {
	public class JSJavaBridge {
		public void error(String s) {
			errors.add(s);
		}
		
		public void log(String s) {
			logger.info(s);
		}
		
		public Object module(String s) {
			try {
				Class<?> clz = Class.forName(s);
				if (!modules.containsKey(clz)) {
					modules.put(clz, Reflection.callStatic(clz, "createJS", this, classloader, config.root));
				}
				return modules.get(clz);
			} catch (IllegalArgumentException | ClassNotFoundException e) {
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
			try {
				runner.call("deliver", json);
			} catch (JSException ex) {
				logger.error("JSException " + ex);
			}
		}
		
		public void lock() {
			counter.lock("lock");
		}
		
		public void unlock() {
			counter.release("unlock");
		}

		public LockingCounter getTestCounter() {
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
	private final LockingCounter counter = new LockingCounter();
	
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
		
		SingleJSTest t1 = new SingleJSTest(page, errors, pw, clz, desc);
		t1.create(desc);
		String name = "dotest";
		runSteps(pw, desc, t1, name);
	}

	private void runSteps(TestResultWriter pw, String desc, SingleJSTest t1, String name) {
		List<String> steps = t1.getSteps(desc, name);
		if (desc != null)
			pw.begin("JS", desc);
		for (String s : steps) {
			if (t1.state != null && t1.state.failed > 0)
				break;
			if (desc != null)
				pw.begin("JS", desc + ": " + s);
			counter.start();
			t1.step(desc, s);
			counter.end(s);
			try {
				counter.waitForZero(5000);
			} catch (Throwable t) {
				pw.error("JS", desc, t);
				errors.add("JS ERROR " + (desc == null ? "configure":desc));
				break;
			}
		}
		if (!steps.isEmpty() && desc != null && t1.ok())
			pw.pass("JS", desc);
	}

	@Override
	protected JSTestState createSystemTest(TestResultWriter pw, SystemTest st) {
		String clz = st.name().jsName();
		pw.println("JS running system test " + st.name().uniqueName());
		SingleJSTest t1 = new SingleJSTest(page, errors, pw, clz, null);
		t1.create(null);
		return t1.state;
	}
	
	@Override
	protected void runSystemTestStage(TestResultWriter pw, JSTestState state, SystemTest st, SystemTestStage e) {
		runSteps(pw, e.desc, state.test, e.name.baseName());
		if (!state.test.ok())
			state.failed++;
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

			for (ContentObject incl : jse.jsIncludes(config, testDirJS)) {
				includeAsScript(pw, incl);
			}
			pw.println("</head>");
			pw.println("<body>");
			pw.println("</body>");
			pw.println("</html>");
			pw.close();
			System.out.println("Loading " + html);
			FileUtils.cat(html);
		} catch (IOException ex) {
			throw WrappedException.wrap(ex);
		}
	}

	private void renderTemplate(PrintWriter pw, String name, String template) {
		pw.println("<template id='" + name.replace(".html", "") + "'>");
		pw.println(template);
		pw.println("</template>");
	}

	private void includeAsScript(PrintWriter pw, ContentObject co) {
		String path = co.url();
		if (useCachebuster)
			path += "?cachebuster=" + System.currentTimeMillis();
		pw.println("<script src='" + path + "' type='text/javascript'></script>");
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
