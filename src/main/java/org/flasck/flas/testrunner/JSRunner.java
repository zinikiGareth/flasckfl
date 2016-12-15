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
import com.ui4j.api.browser.BrowserEngine;
import com.ui4j.api.browser.BrowserFactory;
import com.ui4j.api.browser.Page;

public class JSRunner implements TestRunner {
	private final CompileResult prior;
	private final BrowserEngine browser;
	private Page page;

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
		Object actual = page.executeScript("FLEval.full(test.golden.script.expr1())");
		assertNotNull("There was no actual", actual);
		Object expected = page.executeScript("FLEval.full(test.golden.script.value1())");
		assertNotNull("There was no value1", expected);
		try {
			assertEquals(expected, actual);
		} catch (AssertionError ex) {
			throw new AssertFailed(expected, actual);
		}
	}

}
