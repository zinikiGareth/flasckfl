package org.flasck.flas.testrunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;

import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.Scope;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;

import com.ui4j.api.browser.BrowserEngine;
import com.ui4j.api.browser.BrowserFactory;
import com.ui4j.api.browser.Page;
import com.ui4j.api.dom.Element;

import netscape.javascript.*;

public class JSRunner implements TestRunner {
	private final CompileResult cr;
	private final BrowserEngine browser;
	private Page page;

	public JSRunner(CompileResult cr) {
        this.cr = cr;
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
			System.out.println(scriptDir);
			try {
				compiler.writeJSTo(scriptDir);
				tcr = compiler.createJS(cr.getPackage() + ".script", cr, scope);
				for (File f : scriptDir.listFiles()) {
					System.out.println("Have " + f);
					FileUtils.cat(f);
				}
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
			System.out.println(System.getProperty("user.dir"));
			// probably wants to be config :-)
			pw.println("<script src='" + System.getProperty("user.dir") + "/src/test/resources/flasck/flas-runtime.js' type='text/javascript'></script>");
//			pw.println("<script src='" + System.getProperty("user.dir") + "/src/test/resources/test1/test1.js' type='text/javascript'></script>");
			for (File f : scriptDir.listFiles())
				pw.println("<script src='file:" + f.getPath() + "' type='text/javascript'></script>");
			pw.println("<script type='text/javascript'>test.golden.x = 420;</script>");
			pw.println("</head>");
			pw.println("<body>");
			pw.println("</body>");
			pw.println("</html>");
			pw.close();
			FileUtils.cat(html);
			System.out.println("file:" + html.getPath());
			page = browser.navigate("file:" + html.getPath());
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new UtilException("Failed", ex);
		}
	}

	@Override
	public void assertCorrectValue(int exprId) throws ClassNotFoundException, Exception {
		Object actual = page.executeScript("test.golden.script.expr1()");
		assertNotNull("There was no actual", actual);
		System.out.println("actual = " + actual);
		Object expected = page.executeScript("test.golden.script.value1()");
		assertNotNull("There was no value1", expected);
		System.out.println("expected = " + expected);
		try {
			assertEquals(expected, actual);
		} catch (AssertionError ex) {
			throw new AssertFailed(expected, actual);
		}
	}

}
