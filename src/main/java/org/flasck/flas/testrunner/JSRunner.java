package org.flasck.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.flasck.flas.Configuration;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.Repository;
import org.flasck.jvm.ziniki.ContentObject;
import org.flasck.jvm.ziniki.FileContentObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Navigation;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.ziniki.server.di.DehydratedHandler;
import org.ziniki.server.di.Instantiator;
import org.ziniki.server.di.MakeAHandler;
import org.ziniki.servlet.tda.TDAConfiguration;
import org.ziniki.ziwsh.intf.WSProcessor;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.InvalidUsageException;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.sync.LockingCounter;
import org.zinutils.utils.FileUtils;

public class JSRunner extends CommonTestRunner<JSTestState> implements JSTestController {
	static String jsPortNum = System.getProperty("org.flasck.jsrunner.port");
	static int jsPort = jsPortNum != null ? Integer.parseInt(jsPortNum) : 14040;
	static String showOption = System.getProperty("org.ziniki.chrome.show");
	static boolean headless = showOption == null || !showOption.equalsIgnoreCase("true");
	static String chromeRoot = System.getProperty("org.ziniki.chrome.root");
	static String chromeDriver = System.getProperty("org.ziniki.chrome.driver");
	static String chromeBinary = System.getProperty("org.ziniki.chrome.binary");
	static String headlessBinary = System.getProperty("org.ziniki.headless.binary");
	static String patienceChild = System.getProperty("org.flasck.patience.child");
	boolean wantTimeout = patienceChild == null || !patienceChild.equals("true");
	private final JSStorage jse;
	private WebDriver wd = null;
	private Navigation nav;
	private List<String> testSteps;
	private CountDownLatch cdl;
	private CountDownLatch waitForBridge;
	private File flasckPath;
	private File basePath;
	private String htmlUri;
	final ClassLoader classloader;
	private boolean useCachebuster = false;
	private String jstestdir;
	private String specifiedTestName;
	final LockingCounter counter = new LockingCounter();
	private boolean haveflascklib;
	private JSTestServer jsServer;
	private BrowserJSJavaBridge bridge = null;
	
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
		
		jsServer = new JSTestServer(jsPort, basePath, flasckPath);
		configureServer();
	}
	
	private void configureServer() throws Exception {
		jsServer.configure();
		waitForBridge = new CountDownLatch(1);
		Map<String, Object> items = new TreeMap<>();
		{
			Map<String, Object> map = new TreeMap<>();
			map.put("class", BridgeGenHandler.class.getName());
			map.put("server", jsServer.server());
			map.put("moduleDir", config.moduleDir);
			map.put("sources", jse.packageNames());
			map.put("unitTests", jse.unitTests());
			map.put("systemTests", jse.systemTests());
			map.put("modules", config.modules);
			
			jsServer.addWebTree("/gen/*", new DehydratedHandler<>(new Instantiator("gen", map), items));
		}
		jsServer.addWSTree("/bridge", new MakeAHandler<WSProcessor>() {
			@Override
			public WSProcessor instantiate(TDAConfiguration c) throws Exception {
				BrowserJSJavaBridge bridge = new BrowserJSJavaBridge(JSRunner.this, classloader, config.projectDir, counter);
				JSRunner.this.bridge = bridge;
				waitForBridge.countDown();
				return bridge;
			}
		});

	}

	public void launch() throws Exception {
		if (!jsServer.isRunning()) {
			jsServer.go();
			startDriver();
			visitUri(htmlUri);
			if (!waitForBridge.await(10, TimeUnit.SECONDS)) {
				throw new TimeoutException("bridge not established");
			}
		}
	}

	public void stepsForTest(List<String> steps) {
		this.testSteps = steps;
		cdl.countDown();
	}
	
	public void systemTestPrepared() {
		cdl.countDown();
	}
	
	public void startDriver() {
		ChromeOptions options = new ChromeOptions();
		if (headless) {
			options.addArguments("--headless=new");
			options.setBinary(new File(new File(chromeRoot), headlessBinary));
		} else {
			options.setBinary(new File(new File(chromeRoot), chromeBinary));
		}
		wd = new ChromeDriver(options);
		nav = wd.navigate();
	}

	private void visitUri(String uri) throws InterruptedException {
		cdl = new CountDownLatch(1);
		nav.to("http://localhost:" + jsPort + "/" + uri);
		if (wantTimeout) {
			boolean isReady = cdl.await(25, TimeUnit.SECONDS);
			if (!isReady) {
				jsServer.stop();
				jsServer = null;
				throw new CantHappenException("the test server did not become available");
			}
		} else {
			cdl.await();
		}
	}

	public void ready() {
		cdl.countDown();
	}

	@Override
	public void runUnitTest(TestResultWriter pw, UnitTestCase utc) {
		if (!utc.shouldRunJS()) {
			logger.warn("not running " + utc.description + " in JS because it contains services");
			return;
		}
		String desc = utc.description;
		try {
			launch();
			
			if (!haveflascklib) {
				pw.fail("JS", desc + ": cannot run tests without flascklib");
				return;
			}
			SingleJSTest t1 = new SingleJSTest(bridge, counter, errors, pw, utc.name);
			cdl = new CountDownLatch(1);
			t1.create(cdl);

			String name = "dotest";
			runSteps(pw, desc, t1, name);
		} catch (Exception ex) {
			pw.error("JS", desc + ": " + ex.getMessage(), ex);
		}
	}

	private void runSteps(TestResultWriter pw, String desc, SingleJSTest t1, String name) {
		List<String> steps = this.testSteps;
		if (desc != null)
			pw.begin("JS", desc);
		for (String s : steps) {
			if (t1.state != null && t1.state.failed > 0)
				break;
			if (desc != null)
				pw.begin("JS", desc + ": " + s);
			t1.step(desc, s);
		}
		t1.checkContextSatisfied(desc);
		if (desc != null && t1.ok())
			pw.pass("JS", desc);
		else if (!t1.ok() && errors.isEmpty())
			errors.add("JS ERROR " + (desc == null ? "configure" : desc));
	}

	public void error(String err) {
		System.out.println("error = " + err);
		counter.raise(new JSCaughtException(err));
	}
	
	@Override
	protected JSTestState createSystemTest(TestResultWriter pw, SystemTest st) {
		pw.systemTest("JS", st);
		try {
			launch();
			SingleJSTest t1 = new SingleJSTest(bridge, counter, errors, pw, st.name());
			cdl = new CountDownLatch(1);
			t1.create(cdl);
			return t1.state;
		} catch (Exception ex) {
			pw.error("JS", "desc" + ": " + ex.getMessage(), ex);
		}
		return null;
	}
	
	@Override
	protected void runSystemTestStage(TestResultWriter pw, JSTestState state, SystemTest st, SystemTestStage e) {
		try {
			cdl = new CountDownLatch(1);
			state.test.prepareStage(cdl, e);
			runSteps(pw, e.desc, state.test, e.name.baseName());
		} catch (Exception ex) {
			pw.error("JS", "desc" + ": " + ex.getMessage(), ex);
		}
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
			FileUtils.cleanDirectory(new File(testDir));
			String testDirJS = testDir + "/js";
			File testDirCSS = new File(testDir + "/css");
			FileUtils.assertDirectory(new File(testDirJS));
			File html = new File(testDir, testName + ".html");
			basePath = config.projectDir;
			if (basePath == null)
				basePath = new File(System.getProperty("user.dir"));
			else if (!basePath.isAbsolute())
				basePath = FileUtils.combine(new File(System.getProperty("user.dir")), basePath);
			basePath = new File(basePath.getPath().replace(" ", "%20"));
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
			Iterable<ContentObject> jsfiles = jse.jsIncludes("mock");
			List<ContentObject> thenUse = new ArrayList<>();
			File jsdir = new File(testDir, "js");
			for (ContentObject incl : jsfiles) {
				File copyTo = new File(jsdir, incl.key());
				FileUtils.copyStreamToFile(incl.asStream(), copyTo);
				FileContentObject as = new FileContentObject(copyTo);
				thenUse.add(as);

				importMapName(pw, prev, as);
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
					pw.println("  <link rel='stylesheet' type='text/css' href='/test/html/css/" + c.getName() + "'>");
				}
			}
			for (Entry<String, String> e : templates.entrySet())
				renderTemplate(pw, e.getKey(), e.getValue());

			for (ContentObject incl : thenUse) {
				includeAsScript(pw, incl);
			}
			pw.println("<script src='/gen/run.js' type='module'></script>");
			pw.println("</head>");
			pw.println("<body>");
			pw.println("</body>");
			pw.println("</html>");
			pw.close();
//			System.out.println("Loading " + html + " as " + htmlUri);
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
		String path = fcoFileName(co);
		pw.print("\t\t\"/js/" + new File(path).getName() + "\": \"/test/html/" + path + "\"");
	}

	private void includeAsScript(PrintWriter pw, ContentObject co) {
		String path = fcoFileName(co);
		pw.println("<script src='" + path + "' type='module'></script>");
	}

	private String fcoFileName(ContentObject co) {
		String path = co.url();
		if (co instanceof FileContentObject) {
			path = path.replace("file://", "").replace("file:", "");
			if (path.startsWith(flasckPath.toString()))
				path = path.replace(flasckPath.toString() + "/", "");
			else if (path.startsWith(basePath.toString())) {
				path = path.replace(basePath.toString(), "");
				if (path.startsWith("/html/"))
					path = path.replace("/html/", "");
			} else
				throw new CantHappenException("what is this path? " + path);
		}
		if (useCachebuster)
			path += "?cachebuster=" + System.currentTimeMillis();
		return path;
	}

	public void shutdown() {
		if (jsServer != null) {
			jsServer.stop();
		}
		if (wd != null) {
			wd.close();
			wd.quit();
		}
		if (bridge != null) {
			bridge.waitForShutdown();
		}
	}
}
