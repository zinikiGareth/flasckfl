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
import java.util.stream.Collectors;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.zinutils.exceptions.UtilException;
import org.zinutils.sync.SyncUtils;

import com.ui4j.api.browser.BrowserEngine;
import com.ui4j.api.browser.BrowserFactory;
import com.ui4j.api.browser.Page;

import javafx.application.Platform;
import netscape.javascript.JSObject;

public class JSRunner extends CommonTestRunner {
	public class SetTimeout {
		public void error(String s) {
			errors.add(s);
		}
		public void log(String s) {
			logger.info(s);
		}

		public void callAsync(final JSObject fn) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					fn.eval("this.f()");
				}
			});
		}
	}
	
	private final SetTimeout st = new SetTimeout();
	private final BrowserEngine browser;
	private Page page;
	private Map<String, JSObject> cards = new TreeMap<String, JSObject>();
	private File html;
	private final List<String> errors = new ArrayList<>();
	
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
		Object actual = page.executeScript("FLEval.full(" + spkg + ".expr1())");
		assertNotNull("There was no actual", actual);
		Object expected = page.executeScript("FLEval.full(" + spkg + ".value1())");
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
		String l4 = "_tmp_services = {};";
		execute(l0+l1+l2+l3+l4);
		for (ContractImplements ctr : cd.contracts) {
			String fullName = fullName(ctr.name());
			execute("_tmp_svc = {} ; _tmp_services['" + fullName+"'] = _tmp_svc;");
//			JSObject svc = getVar("_tmp_svc");
			// TODO: we need to capture this object and make it ready for receiving messages later
		}
		String l5 = "_tmp_handle = Flasck.createCard(_tmp_postbox, _tmp_div, { explicit: " + cardType.jsName() + ", mode: 'local' }, _tmp_services)";
		execute(l5);
		
		JSObject card = (JSObject) page.executeScript("_tmp_handle");
		cdefns.put(bindVar, cd);
		cards.put(bindVar, card);
		assertNoErrors();
	}

	@Override
	public void send(String cardVar, String contractName, String methodName, List<Object> args) {
		if (!cdefns.containsKey(cardVar))
			throw new UtilException("there is no card '" + cardVar + "'");

		String fullName = getFullContractNameForCard(cardVar, contractName, methodName);
		JSObject card = cards.get(cardVar);
		card.call("send", fullName, methodName); // TODO: should also allow args
		// Q: how do we ensure that we wait for the method to actually execute?
		SyncUtils.sleep(50);
		assertNoErrors();
	}

	@Override
	public void match(WhatToMatch what, String selector, String contents) throws NotMatched {
//		System.out.println(page.getDocument().getBody().getOuterHTML());
		what.match(selector, contents, page.getDocument().queryAll(selector).stream().map(e -> new JSWrapperElement(e)).collect(Collectors.toList()));
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
}
