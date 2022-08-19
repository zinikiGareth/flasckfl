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
import org.flasck.flas.parsedForm.assembly.ApplicationAssembly;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Repository;
import org.flasck.jvm.ziniki.ContentObject;
import org.zinutils.exceptions.UtilException;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.sync.LockingCounter;
import org.zinutils.utils.FileUtils;

import io.webfolder.ui4j.api.browser.BrowserEngine;
import io.webfolder.ui4j.api.browser.BrowserFactory;
import io.webfolder.ui4j.api.browser.Page;
import javafx.application.Platform;
import netscape.javascript.JSObject;

/* To emulate this in a browser, load the html file, and then from the console:
 * 
 * Unit Tests:
 * 
 *   var runner = new UTRunner(console);
 *   var cxt = runner.newContext();
 *   var ut = new test.golden._ut_nested._ut2(runner, cxt); <-- correct name and test number
 *   ut._ut2_step_1(cxt); <-- for each step in ut._ut2_steps()
 *   ut._ut2_step_2(cxt);
 *   ut._ut2_step_3(cxt);
 *   ut._ut2_step_4(cxt);
 *   runner.assertSatisfied(); <-- to check conditions were all met
 *  
 * System Tests:
 * 
 *   var runner = new UTRunner(console, makeBridge(?,?)); <-- makeBridge is at end of runner.js
 *   var cxt = runner.newContext();
 *   var st = new test.golden._st_mymissing(runner, cxt); <-- correct test name
 *   st.configure_step_1(cxt); <-- first run the configure steps if any
 *   st.configure_step_2(cxt);
 *   runner.assertSatisfied(); <-- check everything went OK
 * 
 * Ziniki Tests:
 * 
 *   ** First, start a fake Ziniki running on a port (see ChromeTestRunner - use target "Ziniki for ChromeTestRunner")
 *   
 *   Then, the easy thing to do is to use the "manual" function, provided by the HTML file for the system test,
 *   which takes host and port and does everything:
 *   
 *     test.golden._st_mymissing.manual("localhost", 18080)
 *  
 *   -- If you insist on more control, you will need to unpack this and do a lot more work.
 *   
 *   To unroll this, you need to first create a bridge and a runner:
 *   
 *   var bridge = new WSBridge("localhost", 18080); <-- host and port must match fake Ziniki
 *   var runner = new UTRunner(bridge, bridge);     <-- WSBridge is both environment and bridge for reasons
 *   
 *   -- 1. You can run the whole system test in one go by calling runRemote with the stages that you want
 *   runner.runRemote(test.golden._st_mymissing, {
 *     configure:     configure: test.golden._st_mymissing.prototype.configure,
 *     stages: [
 *       test.golden._st_mymissing.prototype.stage0
 *     ]
 *   });
 *   
 *   -- to run things at a more granular level, you also need to instantiate the test and a context:
 *   var cxt = runner.newContext();
 *   var st = new test.golden._st_mymissing(runner, cxt);
 *
 *   -- 2. You can run all the steps of a single stage in one go
 *   bridge.executeSync(runner, st, cxt, st.configure(cxt));
 *   
 *   -- 3. You can run individual steps/groups of steps by name
 *   bridge.executeSync(runner, st, cxt, ['configure_step_1', 'st.configure_step_2', 'st.configure_step_3']);
 *   
 *   -- and from time to time, you will want to call assertSatisfied()
 *   runner.assertSatisfied();
 *   
 *   -- note that using runRemote or its child executeSync correctly handles race conditions
 */

public class JSRunner extends CommonTestRunner<JSTestState> {
	private final JSStorage jse;
	private final JSJavaBridge bridge = new FXJSJavaBridge(this);
	private final BrowserEngine browser;
	final Map<Class<?>, Object> modules = new HashMap<>();
	final ClassLoader classloader;
	Page page;
	private File html;
	private boolean useCachebuster = false;
	private String jstestdir;
	private String specifiedTestName;
	final LockingCounter counter = new LockingCounter();
	private boolean haveflascklib;
	
	public JSRunner(Configuration config, Repository repository, JSStorage jse, Map<String, String> templates, ClassLoader cl) {
		super(config, repository);
		if (config != null) {
			this.jstestdir = config.jsTestDir();
			this.specifiedTestName = config.specifiedTestName;
			haveflascklib = config.flascklibDir != null;
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
			win.setMember("callJava", bridge);
			cdl.countDown();
		});
		if (!await)
			throw new RuntimeException("Whole test failed to initialize");
	}

	boolean uiThread(Consumer<CountDownLatch> doit) {
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
		
		if (!haveflascklib) {
			pw.fail("JS", desc + ": cannot run tests without flascklib");
			return;
		}
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
				counter.waitForZero(15000);
			} catch (Throwable t) {
				logger.warn("Error waiting for test to end", t);
				pw.error("JS", desc, t);
				errors.add("JS ERROR " + (desc == null ? "configure":desc));
				break;
			}
		}
		t1.checkContextSatisfied(desc);
		if (desc != null && t1.ok())
			pw.pass("JS", desc);
		else if (!t1.ok() && errors.isEmpty())
			errors.add("JS ERROR " + (desc == null ? "configure" : desc));
	}

	@Override
	protected JSTestState createSystemTest(TestResultWriter pw, SystemTest st) {
		String clz = st.name().jsName();
		pw.systemTest("JS", st);
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
			pw.passedSystemTest("JS", st);
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
			pw.println("<script>");
			repository.traverse(new LeafAdapter() {
				@Override
				public void visitAssembly(ApplicationAssembly e) {
					pw.println(e.name().uniqueName() +  "._Application.prototype.securityModule = new STSecurityModule();");
				}
			});
			for (SystemTest st : jse.systemTests()) {
				pw.println(st.name().jsName()  + ".manual = function(host, port) {");
				pw.println("  var runner = new UTRunner(new WSBridge(host, port));");
				pw.println("  runner.runRemote(" + st.name().jsName() + ", {");
				String comma = "";
				if (st.configure != null) {
					pw.print("    configure: " + st.configure.name.jsPName());
					comma = ",\n";
				}
				if (st.stages != null) {
					pw.print(comma + "    stages: [");
					comma = "\n      ";
					String other = "";
					for (SystemTestStage e : st.stages) {
						pw.print(comma + e.name.jsPName());
						comma = ",      \n";
						other = "\n    ";
					}
					pw.println(other + "]");
					comma = ",\n";
				}
				if (st.cleanup != null) {
					pw.println(comma + "    cleanup: " + st.cleanup.name.jsPName());
					comma = ",\n";
				}
				pw.println("  });");
//				pw.println("  st.configure_step_1(cxt);");
				pw.println("}");
			}
			pw.println("</script>");
			pw.println("</body>");
			pw.println("</html>");
			pw.close();
//			System.out.println("Loading " + html);
//			FileUtils.cat(html);
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
