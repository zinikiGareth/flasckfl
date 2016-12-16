package org.flasck.flas.testrunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;
import com.ui4j.api.browser.BrowserEngine;
import com.ui4j.api.browser.BrowserFactory;
import com.ui4j.api.browser.Page;
import com.ui4j.api.dom.Element;

import netscape.javascript.JSObject;

public class JSRunner implements TestRunner {
	private static Logger logger = LoggerFactory.getLogger("JSRunner");
	public class MyLogger {
		public void log(String s) {
			logger.info(s);
		}
		/*
		public void log(Object... msgs) {
			StringBuilder sb = new StringBuilder();
			String sep = "";
			for (Object o : msgs) {
				sb.append(sep);
				sep = " ";
				sb.append(o);
			}
			logger.info(sb.toString());
		}
		*/
	}

	private final MyLogger ml = new MyLogger();
	private final CompileResult prior;
	private final BrowserEngine browser;
	private Page page;
	private String spkg;
	private Map<String, JSObject> cards = new TreeMap<String, JSObject>();
	
	public JSRunner(CompileResult cr) {
        this.prior = cr;
		browser = BrowserFactory.getWebKit();
	}

	 // This is feeling more and more like a characterization test
	@Override
	public void prepareScript(ScriptCompiler compiler, Scope scope) {
		CompileResult tcr = null;
		File scriptDir = null;
		try {
			scriptDir = Files.createTempDirectory("testScriptDir").toFile();
			scriptDir.deleteOnExit();
			try {
				compiler.writeJSTo(scriptDir);
				tcr = compiler.createJS(prior.getPackage() + ".script", prior, scope);
			} catch (ErrorResultException ex) {
				ex.errors.showTo(new PrintWriter(System.err), 0);
				fail("Errors compiling test script");
			}
			File html = File.createTempFile("testScript", ".html");
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
			page = browser.navigate("file:" + html.getPath());
			JSObject win = (JSObject)page.executeScript("window");
			win.setMember("console", ml);
			spkg = tcr.getPackage();
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
	public void createCardAs(String cardType, String bindVar) {
		if (cards.containsKey(bindVar))
			throw new UtilException("Duplicate card assignment to '" + bindVar + "'");
		
		// probably should be earlier
		String l0 = "_tmp_postbox = new Postbox('main', window);";
  		String l1 = "_tmp_body = document.getElementsByTagName('body')[0];";
		String l2 = "_tmp_div = document.createElement('div');";
		String l3 = "_tmp_body.appendChild(_tmp_div);";
		String l4 = "_tmp_services = {};"; // surely we need something better
		String l5 = "_tmp_handle = Flasck.createCard(_tmp_postbox, _tmp_div, { explicit: " + cardType + ", mode: 'local' }, _tmp_services)";

		String instr = l0+l1+l2+l3+l4+l5;
		// TODO: refactor this to be standard error handling
		JSObject err = (JSObject)page.executeScript("_tmp_error = null; try { " + instr + " } catch (err) { _tmp_error = err; }; _tmp_error;");
		if (err != null)
			throw new UtilException("Error processing javascript: " + err);
		JSObject card = (JSObject) page.executeScript("_tmp_handle");
		cards.put(bindVar, card);
	}

	@Override
	public void match(WhatToMatch what, String selector, String contents) throws NotMatched {
		System.out.println(page.getDocument().getBody().getOuterHTML());
		List<Element> divX = page.getDocument().queryAll(selector);
		what.match(selector, contents, divX);
	}
}
