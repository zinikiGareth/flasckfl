package org.flasck.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.codehaus.jettison.json.JSONException;
import org.flasck.flas.Configuration;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.assembly.ApplicationAssembly;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Repository;
import org.flasck.jvm.ziniki.ContentObject;
import org.flasck.jvm.ziniki.FileContentObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Navigation;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.ziniki.server.NewConnectionHandler;
import org.ziniki.server.di.DehydratedHandler;
import org.ziniki.server.di.Instantiator;
import org.ziniki.server.di.MakeAHandler;
import org.ziniki.server.grizzly.GrizzlyTDAServer;
import org.ziniki.server.grizzly.GrizzlyTDAWebSocketHandler;
import org.ziniki.server.path.PathTree;
import org.ziniki.server.path.SimplePathTree;
import org.ziniki.server.tda.Transport;
import org.ziniki.server.tda.WSReceiver;
import org.ziniki.servlet.tda.RequestProcessor;
import org.ziniki.servlet.tda.TDAConfiguration;
import org.ziniki.ziwsh.intf.WSProcessor;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.InvalidUsageException;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.sync.LockingCounter;
import org.zinutils.utils.FileUtils;

/* To emulate this in a browser, load the html file, and then from the console:
 * 
 * Unit Tests:
 * 
    var runner = new UTRunner(console);
    var cxt = runner.newContext();
    var ut = new test.golden._ut_nested._ut2(runner, cxt); <-- correct name and test number
    ut._ut2_step_1(cxt); <-- for each step in ut._ut2_steps()
    ut._ut2_step_2(cxt);
    ut._ut2_step_3(cxt);
    ut._ut2_step_4(cxt);
    runner.assertSatisfied(); <-- to check conditions were all met
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
	static String showOption = System.getProperty("org.ziniki.chrome.show");
	static boolean headless = showOption == null || !showOption.equalsIgnoreCase("true");
	static String chromeRoot = System.getProperty("org.ziniki.chrome.root");
	static String chromeDriver = System.getProperty("org.ziniki.chrome.driver");
	static String chromeBinary = System.getProperty("org.ziniki.chrome.binary");
	static String headlessBinary = System.getProperty("org.ziniki.headless.binary");
	private final JSStorage jse;
	private BrowserJSJavaBridge bridge = null;
	private WebDriver wd = null;
	private List<String> testsToRun;
	private Navigation nav;
	private List<String> testSteps;
	private CountDownLatch cdl;
	final Map<Class<?>, Object> modules = new HashMap<>();
	private File flasckPath;
	private File basePath;
	private String htmlUri;
	final ClassLoader classloader;
	private boolean useCachebuster = false;
	private String jstestdir;
	private String specifiedTestName;
	final LockingCounter counter = new LockingCounter();
	private boolean haveflascklib;
	private GrizzlyTDAServer server;
	
	public JSRunner(Configuration config, Repository repository, JSStorage jse, Map<String, String> templates, ClassLoader cl) throws Exception {
		super(config, repository);
		
		if (chromeRoot == null || !new File(chromeRoot).exists())
			throw new InvalidUsageException("must specify org.ziniki.chrome.root as a directory with driver and binary");
		if (chromeDriver == null || !new File(chromeRoot, chromeDriver).exists())
			throw new InvalidUsageException("must specify org.ziniki.chrome.driver as a directory under chrome_root with driver");
		if (!headless && (chromeBinary == null || !new File(chromeRoot, chromeBinary).exists()))
			throw new InvalidUsageException("must specify org.ziniki.chrome.binary as a directory under chrome_root with chrome binary");
		if (headless && (headlessBinary == null || !new File(chromeRoot, headlessBinary).exists()))
			throw new InvalidUsageException("must specify org.ziniki.headless.binary as a directory under chrome_root with headless binary");
		
		System.setProperty("webdriver.chrome.driver", chromeRoot + "/" + chromeDriver);
		
		if (config != null) {
			this.jstestdir = config.jsTestDir();
			this.specifiedTestName = config.specifiedTestName;
			haveflascklib = config.flascklibDir != null;
		} else {
			this.jstestdir = System.getProperty("user.dir");
			this.specifiedTestName = null;
		}
		this.jse = jse;
		this.classloader = cl;

		flasckPath = new File(new File(System.getProperty("user.dir")), "src/main/resources");
		buildHTML(templates);
		startServer();
		startDriver();
		visitUri(htmlUri);
		while (runAvailableTests()) {
			while (runNextStep()) {
			}
		}
		if (!headless) {
			System.out.println("done ... waiting to allow browser to be examined");
			Thread.sleep(50000);
		}
		shutdown();
		
		/*
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
		 */
	}

	public void testsToRun(List<String> names) {
		this.testsToRun = names;
		cdl.countDown();
	}

	public void stepsForTest(List<String> steps) {
		this.testSteps = steps;
		cdl.countDown();
	}
	
	private boolean runAvailableTests() throws InterruptedException, JSONException {
		boolean isReady = cdl.await(25, TimeUnit.SECONDS);
		if (!isReady)
			throw new CantHappenException("the tests were not made available");
		if (!testsToRun.isEmpty()) {
			// run the first test
			this.testSteps = null;
			String test = testsToRun.remove(0);
			logger.info("Run test: " + test);
			bridge.prepareTest(test);
			cdl = new CountDownLatch(1);
			return true;
		} else {
			logger.info("done all tests");
			return false;
		}
	}

	private boolean runNextStep() throws InterruptedException, JSONException, TimeoutException {
		if (this.testSteps == null) {
			// wait for the client to tell us the steps
			boolean isReady = cdl.await(25, TimeUnit.SECONDS);
			if (!isReady)
				throw new CantHappenException("the test steps were not made available");
		}
		if (!testSteps.isEmpty()) {
			bridge.counter.start();
			String step = testSteps.remove(0);
			logger.info("running step " + step);
			bridge.runStep(step);
			bridge.counter.waitForZero(5, TimeUnit.SECONDS);
			return true;
		} else {
			logger.info("all steps run");
			return false;
		}
	}

	private void startServer() throws Exception {
		server = new GrizzlyTDAServer(14040);
		PathTree<RequestProcessor> tree = new SimplePathTree<>();
		Map<String, Object> items = new TreeMap<>();
		{
			Map<String, Object> map = new TreeMap<>();
			map.put("class", BridgeGenHandler.class.getName());
//			map.put("path", basePath);
//			map.put("flasck", flasckPath);
			map.put("server", server);
			map.put("secure", false);
			
			tree.add("/gen/*", new DehydratedHandler<>(new Instantiator("gen", map), items));
		}
		{
			Map<String, Object> map = new TreeMap<>();
			map.put("class", RunTestHandler.class.getName());
			map.put("path", basePath);
			map.put("flasck", flasckPath);

			tree.add("/test/*", new DehydratedHandler<>(new Instantiator("test", map), items));
		}
		server.httpMappingTree(tree);
		PathTree<WSProcessor> wstree = new SimplePathTree<>();
		wstree.add("/bridge", new MakeAHandler<WSProcessor>() {
			@Override
			public WSProcessor instantiate(TDAConfiguration config) throws Exception {
				BrowserJSJavaBridge bridge = new BrowserJSJavaBridge(JSRunner.this);
				JSRunner.this.bridge = bridge;
				return bridge;
			}
		});
		NewConnectionHandler<? extends WSReceiver> handler = new NewConnectionHandler<WSReceiver>() {
			@Override
			public void newConnection(Transport transport, WSReceiver handler) {
				transport.addReceiver(handler);
			}
		};
		server.wsMappingTree(new GrizzlyTDAWebSocketHandler(), wstree, handler);
		server.start();
	}

	public void startDriver() {
		ChromeOptions options = new ChromeOptions();
//		if (headless) {
//			options.addArguments("headless", "window-size=1200x900");
//		}
		options.setBinary(new File(new File(chromeRoot), chromeBinary));
		wd = new ChromeDriver(options);
		nav = wd.navigate();
	}

	private void visitUri(String uri) {
		cdl = new CountDownLatch(1);
		nav.to("http://localhost:14040/" + uri);

	}
	private void shutdown() {
		if (server != null) {
			server.stop(1, TimeUnit.SECONDS);
		}
		if (wd != null) {
			wd.close();
			wd.quit();
		}
	}


	/*
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
	*/

	@Override
	public void runUnitTest(TestResultWriter pw, UnitTestCase utc) {
		String clz = utc.name.jsName();
		String desc = utc.description;
		
		if (!haveflascklib) {
			pw.fail("JS", desc + ": cannot run tests without flascklib");
			return;
		}
		/*
		SingleJSTest t1 = new SingleJSTest(page, errors, pw, clz, desc);
		t1.create(desc);
		String name = "dotest";
		runSteps(pw, desc, t1, name);
		 */
		String name = "dotest";
		runSteps(pw, desc, null, name);
	}

	private void runSteps(TestResultWriter pw, String desc, SingleJSTest t1, String name) {
//		List<String> steps = t1.getSteps(desc, name);
		if (desc != null)
			pw.begin("JS", desc);
//		for (String s : steps) {
//			if (t1.state != null && t1.state.failed > 0)
//				break;
//			if (desc != null)
//				pw.begin("JS", desc + ": " + s);
//			counter.start();
//			t1.step(desc, s);
//			counter.end(s);
//			try {
//				counter.waitForZero(15000);
//			} catch (Throwable t) {
//				logger.warn("Error waiting for test to end", t);
//				pw.error("JS", desc, t);
//				errors.add("JS ERROR " + (desc == null ? "configure":desc));
//				break;
//			}
//		}
//		t1.checkContextSatisfied(desc);
//		if (desc != null && t1.ok())
		if (desc != null/* && t1.ok() */)
			pw.pass("JS", desc);
//		else if (!t1.ok() && errors.isEmpty())
//			errors.add("JS ERROR " + (desc == null ? "configure" : desc));
	}

	@Override
	protected JSTestState createSystemTest(TestResultWriter pw, SystemTest st) {
		String clz = st.name().jsName();
		pw.systemTest("JS", st);
		SingleJSTest t1 = new SingleJSTest(/*page, errors, pw, clz, null*/);
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
			File html = new File(testDir, testName + ".html");
			basePath = config.root;
			if (!basePath.isAbsolute())
				basePath = FileUtils.combine(new File(System.getProperty("user.dir")), basePath);
			htmlUri = "test/html/" + testName + ".html";
			PrintWriter pw = new PrintWriter(html);
			pw.println("<!DOCTYPE html>");
			pw.println("<html>");
			pw.println("<head>");
			pw.println("<link rel=\"icon\" href=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVQI12P4//8/AAX+Av7czFnnAAAAAElFTkSuQmCC\">");
			pw.println("<script type='importmap'>");
			pw.println("{");
			pw.println("\t\"imports\": {");
			boolean prev = false;
			for (ContentObject incl : jse.jsIncludes(config, testDirJS)) {
				importMapName(pw, prev, incl);
				prev = true;
			}
			if (prev)
				pw.println("");
			pw.println("\t}");
			pw.println("}");
			pw.println("</script>");
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
			pw.println("<script src='/gen/run.js' type='module'></script>");
			pw.println("</head>");
			pw.println("<body>");
			/*
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
			*/
			pw.println("</body>");
			pw.println("</html>");
			pw.close();
			System.out.println("Loading " + html + " as " + htmlUri);
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

	private void importMapName(PrintWriter pw, boolean prev, ContentObject co) {
		if (prev) {
			pw.println(",");
		}
		String path = co.url();
		if (co instanceof FileContentObject) {
			path = path.replace("file://", "");
			if (path.startsWith(flasckPath.toString()))
				path = path.replace(flasckPath.toString() + "/", "");
			else if (path.startsWith(basePath.toString()))
				path = path.replace(basePath.toString() + "/jsout", "js");
			else
				throw new CantHappenException("what is this path? " + path);
		}
		if (useCachebuster)
			path += "?cachebuster=" + System.currentTimeMillis();
		pw.print("\t\t\"/js/" + new File(path).getName() + "\": \"/test/html/" + path + "\"");
	}

	private void includeAsScript(PrintWriter pw, ContentObject co) {
		String path = co.url();
		if (co instanceof FileContentObject) {
			path = path.replace("file://", "");
			if (path.startsWith(flasckPath.toString()))
				path = path.replace(flasckPath.toString() + "/", "");
			else if (path.startsWith(basePath.toString()))
				path = path.replace(basePath.toString() + "/jsout", "js");
			else
				throw new CantHappenException("what is this path? " + path);
		}
		if (useCachebuster)
			path += "?cachebuster=" + System.currentTimeMillis();
		pw.println("<script src='" + path + "' type='module'></script>");
	}
/*
	protected JSObject getVar(String var) {
		return (JSObject)page.executeScript(var);
	}

	protected void execute(String instr) {
		JSObject err = (JSObject)page.executeScript("_tmp_error = null; try { " + instr + " } catch (err) { _tmp_error = err; }; _tmp_error;");
		if (err != null)
			throw new UtilException("Error processing javascript: " + err);
	}
	*/
}
