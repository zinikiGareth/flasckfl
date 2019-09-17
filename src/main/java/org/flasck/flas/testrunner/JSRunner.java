package org.flasck.flas.testrunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.flasck.flas.Configuration;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.compiler.jsgen.JSStorage;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.repository.Repository;
import org.flasck.ui4j.UI4JWrapperElement;
import org.ziniki.ziwsh.model.InternalHandle;
import org.zinutils.exceptions.UtilException;
import org.zinutils.exceptions.WrappedException;
import io.webfolder.ui4j.api.browser.BrowserEngine;
import io.webfolder.ui4j.api.browser.BrowserFactory;
import io.webfolder.ui4j.api.browser.Page;
import io.webfolder.ui4j.api.dom.Element;
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
	
	private final JSStorage jse;
	private final JSJavaBridge st = new JSJavaBridge();
	private final BrowserEngine browser;
	private Page page;
	private Map<String, CardHandle> cards = new TreeMap<>();
	private File html;
	
	public JSRunner(Configuration config, Repository repository, JSStorage jse) {
		super(config, repository);
		this.jse = jse;
		this.browser = BrowserFactory.getWebKit();

		// TODO: I'm not sure how much more of this is actually per-package and how much is "global"
		buildHTML();
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
				page.executeScript(utc.name.jsName() + "(new FLContext(window.runner), window.runner)");
				pw.println("JS PASS " + utc.description);
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
	
	protected void includeFileAsScript(PrintWriter pw, File f) {
		if (!f.isAbsolute())
			f = new File(new File(System.getProperty("user.dir")), f.getPath());
		pw.println("<script src='file:" + f.getPath() + "?cachebuster=" + System.currentTimeMillis()  + "' type='text/javascript'></script>");
	}

	private void buildHTML() {
		try {
			html = File.createTempFile("testScript", ".html");
			html.deleteOnExit();
			PrintWriter pw = new PrintWriter(html);
			pw.println("<!DOCTYPE html>");
			pw.println("<html>");
			pw.println("<head>");
			// probably wants to be config :-)
			final String jsfile = System.getProperty("user.dir") + "/src/test/resources/flasck/flas-runtime.js";
			final String utfile = System.getProperty("user.dir") + "/src/test/resources/flasck/flas-unittest.js";
			pw.println("<script src='file:" + jsfile + "' type='text/javascript'></script>");
			pw.println("<script src='file:" + utfile + "' type='text/javascript'></script>");
			for (File f : jse.files())
				includeFileAsScript(pw, f);
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

	@Override
	public void assertCorrectValue(int exprId) throws ClassNotFoundException, Exception {
		final String actualFn = testPkg + ".expr" + exprId;
		Object actual = page.executeScript("FLEval.full(" + actualFn + "())");
		assertNotNull("There was no actual value from " + actualFn + "()", actual);
		final String expectFn = testPkg + ".value" + exprId;
		Object expected = page.executeScript("FLEval.full(" + expectFn + "())");
		assertNotNull("There was no expected value from " + expectFn +"()", expected);
		try {
			assertEquals(expected, actual);
		} catch (AssertionError ex) {
			throw new AssertFailed(expected, actual);
		}
	}

	@Override
	public void createCardAs(CardName cardType, String bindVar) {
//		logger.info("Creating card " + cardType.jsName() + " as " + bindVar);
//		if (cards.containsKey(bindVar))
//			throw new UtilException("Duplicate card assignment to '" + bindVar + "'");
//		
//		ScopeEntry se = null; // compiledScope.get(cardType.cardName);
//		if (se == null)
//			throw new UtilException("There is no definition for card '" + cardType.cardName + "' in scope");
//		if (se.getValue() == null || !(se.getValue() instanceof CardDefinition))
//			throw new UtilException(cardType.cardName + " is not a card");
//		
//		AtomicBoolean choke = new AtomicBoolean(false);
//		Platform.runLater(() -> {
//			CardDefinition cd = (CardDefinition) se.getValue();
//	
//			execute("Flasck.unitTest();");
//			
//			// this first line probably should be earlier
//			String l0 = "_tmp_postbox = new Postbox('main', window);";
//	
//			String l1 = "_tmp_body = document.getElementsByTagName('body')[0];";
//			String l2 = "_tmp_div = document.createElement('div');";
//			String l3 = "_tmp_body.appendChild(_tmp_div);";
//			// _tmp_services needs to be a map of service name to port to listen on
//			String l4 = "_tmp_services = {};";
//			execute(l0+l1+l2+l3+l4);
//			for (ContractImplements ctr : cd.contracts) {
//				String fullName = ""; // fullName(ctr.actualName().jsName());
//				JSObject win = (JSObject)page.executeScript("window");
//				MockServiceWrapper ms = new MockServiceWrapper(fullName);
//				// TODO: need to wire ms up in some way to have expectations ...
//				win.setMember("_tmp_svc", ms);
//				execute("Flasck.provideService(_tmp_postbox, _tmp_services, '" + fullName + "', _tmp_svc)");
//				System.out.println("Binding " + fullName + " to " + ms._myAddr);
//			}
//			String l5 = "_tmp_handle = Flasck.createCard(_tmp_postbox, _tmp_div, { explicit: " + cardType.jsName() + ", mode: 'local' }, _tmp_services)";
//			execute(l5);
//			
//			JSObject handle = (JSObject) page.executeScript("_tmp_handle");
//			cdefns.put(bindVar, cd);
//			cards.put(bindVar, new CardHandle(handle, (JSObject)handle.getMember("_mycard"), (JSObject) handle.getMember("_wrapper")));
//			assertNoErrors();
//			choke.set(true);
//		});
//		waitForChoke(choke);
	}

	@Override
	public void send(InternalHandle ih, String cardVar, String contractName, String methodName, List<Integer> posns) {
		if (!cdefns.containsKey(cardVar))
			throw new UtilException("there is no card '" + cardVar + "'");

		String fullName = ""; // getFullContractNameForCard(cardVar, contractName, methodName);
		CardHandle card = cards.get(cardVar);

		logger.info("Sending " + methodName + " to " + cardVar + "." + fullName);

		AtomicBoolean choke = new AtomicBoolean(false);
		Platform.runLater(() -> {
			List<Object> args = new ArrayList<Object>();
			args.add(fullName);
			args.add(methodName);
			if (posns != null)
				for (int i : posns) {
					args.add(page.executeScript("FLEval.full(" + testPkg + ".arg" + i + "())"));
				}
			card.handle.call("send", args.toArray());
			choke.set(true);
		});
		waitForChoke(choke);
		processEvents();
		assertNoErrors();
		assertAllInvocationsCalled();
	}

	@Override
	public void event(String cardVar, String methodName) throws Exception {
		logger.info("Sending event " + methodName + " to " +  cardVar);
		CardHandle card = cards.get(cardVar);
		AtomicBoolean choke = new AtomicBoolean(false);
		Platform.runLater(() -> {
			// Or else just call "dispatchEvent()" on wrapper
			// But where do I find wrapper?
			// cf JVMRunner, where we call the hanlde and get it to do the dirty work
			// is that possible in this world?
			Object events = card.card.call(methodName);
			card.wrapper.call("evalAndProcess", events);
			choke.set(true);
		});
		waitForChoke(choke);
		processEvents();
		assertNoErrors();
		assertAllInvocationsCalled();
	}

	@Override
	public void match(HTMLMatcher matcher, String selector) throws NotMatched {
		logger.info("Matching " + selector);
//		System.out.println(page.getDocument().getBody().getOuterHTML());
		matcher.match(selector, page.getDocument().queryAll(selector).stream().map(e -> new UI4JWrapperElement(page, e)).collect(Collectors.toList()));
		assertNoErrors();
	}

	private void assertNoErrors() {
		if (!errors.isEmpty()) 
			throw new MultiException(errors);
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
	public void click(String selector) {
		List<Element> elts = page.getDocument().queryAll(selector);
		if (elts.isEmpty())
			throw new UtilException("No elements matched " + selector);
		else if (elts.size() > 1)
			throw new UtilException("Multiple elements matched " + selector);
		Element e = elts.get(0);
		if (!e.hasAttribute("onclick")) {
			String html;
			try {
				html = e.getOuterHTML();
			} catch (Throwable t) {
				html = "";
			}
			throw new UtilException("There is no 'onclick' attribute on " + selector + html);
		}
		e.click();
		processEvents();
		assertAllInvocationsCalled();
	}

	/* There was an exception here telling me to do this some other way,
	 * but I'm not sure what the problem was - although the code didn't compile.
	 * I fixed the compilation problems, but kept the same logic.
	 * 
	 * If that's good enough, sobeit. Otherwise, fix this when it breaks or at
	 * least add a really good comment.
	 */
	private void processEvents() {
		Date waitUntil = new Date(new Date().getTime() + 5000);
		while (pendingAsyncs.get() != 0)
			try {
				if (new Date().after(waitUntil))
					throw new RuntimeException("After 5s still have " + pendingAsyncs.get() +" pending asyncs");
				synchronized (pendingAsyncs) {
					pendingAsyncs.wait(1000);
				}
			} catch (InterruptedException ex) {
				throw new UtilException("timed out waiting for pending async");
			}
	}

	private void waitForChoke(AtomicBoolean choke) {
		for (int i=0;!choke.get() && i<100;i++) {
			try { Thread.sleep(10); } catch (InterruptedException ex) {}
		}
		if (!choke.get())
			fail("did not manage to run platform code");
	}
}
