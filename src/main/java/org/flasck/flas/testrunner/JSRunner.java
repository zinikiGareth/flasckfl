package org.flasck.flas.testrunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.ui4j.UI4JWrapperElement;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;

import com.ui4j.api.browser.BrowserEngine;
import com.ui4j.api.browser.BrowserFactory;
import com.ui4j.api.browser.Page;
import com.ui4j.api.dom.Element;

import javafx.application.Platform;
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
	
	private final JSJavaBridge st = new JSJavaBridge();
	private final BrowserEngine browser;
	private Page page;
	private Map<String, JSObject> cards = new TreeMap<String, JSObject>();
	private File html;
	
	public JSRunner(CompileResult cr) {
		super(cr);
		browser = BrowserFactory.getWebKit();
	}

	@Override
	public void prepareScript(ScriptCompiler compiler, Scope scope) {
		CompileResult tcr = null;
		File scriptDir = null;
		try {
			scriptDir = Files.createTempDirectory("testScriptDir").toFile();
			scriptDir.deleteOnExit();
			try {
				compiler.writeJSTo(scriptDir);
				tcr = compiler.createJS(prior.getPackage().uniqueName() + ".script", prior, scope);
			} catch (ErrorResultException ex) {
				ex.errors.showTo(new PrintWriter(System.err), 0);
				fail("Errors compiling test script");
			}
			html = File.createTempFile("testScript", ".html");
			html.deleteOnExit();
			PrintWriter pw = new PrintWriter(html);
			pw.println("<!DOCTYPE html>");
			pw.println("<html>");
			pw.println("<head>");
			// probably wants to be config :-)
			pw.println("<script src='file:" + System.getProperty("user.dir") + "/src/test/resources/flasck/flas-runtime.js' type='text/javascript'></script>");
			for (File f : prior.jsFiles())
				scriptIt(pw, f);
			for (File f : tcr.jsFiles())
				scriptIt(pw, f);
			pw.println("</head>");
			pw.println("<body>");
			pw.println("</body>");
			pw.println("</html>");
			pw.close();
			spkg = tcr.getPackage().uniqueName();
			FileUtils.cat(html);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new UtilException("Failed", ex);
		}
	}

	protected void scriptIt(PrintWriter pw, File f) {
		if (!f.isAbsolute())
			f = new File(new File(System.getProperty("user.dir")), f.getPath());
		pw.println("<script src='file:" + f.getPath() + "' type='text/javascript'></script>");
	}

	
	@Override
	public void prepareCase() {
		page = browser.navigate("file:" + html.getPath());
		page.executeScript("window.console = {};");
		page.executeScript("window.console.log = function() { var ret = ''; var sep = ''; for (var i=0;i<arguments.length;i++) { ret += sep + arguments[i]; sep = ' '; } callJava.log(ret); };");
		JSObject win = (JSObject)page.executeScript("window");
		win.setMember("callJava", st);
		
		// Do I need to do more cleanup than this?
		// Also, should there be an "endCase" to do cleanup?
		cards.clear();
		errors.clear();
	}

	@Override
	public void assertCorrectValue(int exprId) throws ClassNotFoundException, Exception {
		Object actual = page.executeScript("FLEval.full(" + spkg + ".expr" + exprId + "())");
		assertNotNull("There was no actual", actual);
		Object expected = page.executeScript("FLEval.full(" + spkg + ".value" + exprId + "())");
		assertNotNull("There was no value1", expected);
		try {
			assertEquals(expected, actual);
		} catch (AssertionError ex) {
			throw new AssertFailed(expected, actual);
		}
	}

	@Override
	public void createCardAs(CardName cardType, String bindVar) {
		if (cards.containsKey(bindVar))
			throw new UtilException("Duplicate card assignment to '" + bindVar + "'");
		
		ScopeEntry se = prior.getScope().get(cardType.cardName);
		if (se == null)
			throw new UtilException("There is no definition for card '" + cardType.cardName + "' in scope");
		if (se.getValue() == null || !(se.getValue() instanceof CardDefinition))
			throw new UtilException(cardType.cardName + " is not a card");
		CardDefinition cd = (CardDefinition) se.getValue();

		// this first line probably should be earlier
		String l0 = "_tmp_postbox = new Postbox('main', window);";

		String l1 = "_tmp_body = document.getElementsByTagName('body')[0];";
		String l2 = "_tmp_div = document.createElement('div');";
		String l3 = "_tmp_body.appendChild(_tmp_div);";
		// _tmp_services needs to be a map of service name to port to listen on
		String l4 = "_tmp_services = {};";
		execute(l0+l1+l2+l3+l4);
		for (ContractImplements ctr : cd.contracts) {
			String fullName = fullName(ctr.name());
			JSObject win = (JSObject)page.executeScript("window");
			MockServiceWrapper ms = new MockServiceWrapper(fullName);
			// TODO: need to wire ms up in some way to have expectations ...
			win.setMember("_tmp_svc", ms);
			execute("Flasck.provideService(_tmp_postbox, _tmp_services, '" + fullName + "', _tmp_svc)");
			System.out.println("Binding " + fullName + " to " + ms._myAddr);
		}
		String l5 = "_tmp_handle = Flasck.createCard(_tmp_postbox, _tmp_div, { explicit: " + cardType.jsName() + ", mode: 'local' }, _tmp_services)";
		execute(l5);
		
		JSObject card = (JSObject) page.executeScript("_tmp_handle");
		cdefns.put(bindVar, cd);
		cards.put(bindVar, card);
		assertNoErrors();
	}

	@Override
	public void send(String cardVar, String contractName, String methodName, List<Integer> posns) {
		if (!cdefns.containsKey(cardVar))
			throw new UtilException("there is no card '" + cardVar + "'");

		String fullName = getFullContractNameForCard(cardVar, contractName, methodName);
		JSObject card = cards.get(cardVar);
		
		List<Object> args = new ArrayList<Object>();
		args.add(fullName);
		args.add(methodName);
		if (posns != null)
			for (int i : posns) {
				args.add(page.executeScript("FLEval.full(" + spkg + ".arg" + i + "())"));
			}
		card.call("send", args.toArray());
		processEvents();
		assertNoErrors();
		assertAllInvocationsCalled();
	}

	@Override
	public void match(HTMLMatcher matcher, String selector) throws NotMatched {
//		System.out.println(page.getDocument().getBody().getOuterHTML());
		matcher.match(selector, page.getDocument().queryAll(selector).stream().map(e -> new UI4JWrapperElement(e)).collect(Collectors.toList()));
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
		if (!e.hasAttribute("onclick"))
			throw new UtilException("There is no 'onclick' attribute on " + e.getOuterHTML());
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
		while (pendingAsyncs.get() != 0)
			try {
				synchronized (pendingAsyncs) {
					pendingAsyncs.wait(1000);
				}
			} catch (InterruptedException ex) {
				throw new UtilException("timed out waiting for pending async");
			}
	}
}
