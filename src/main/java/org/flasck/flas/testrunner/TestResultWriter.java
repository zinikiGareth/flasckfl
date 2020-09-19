package org.flasck.flas.testrunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class TestResultWriter {
	private final boolean writeEverything;
	private final PrintWriter pw;
	private boolean doClose;

	public TestResultWriter(boolean writeEverything, PrintStream out) {
		this.writeEverything = writeEverything;
		this.pw = new PrintWriter(out);
	}
	
	public TestResultWriter(boolean writeEverything, StringWriter sw) {
		this.writeEverything = writeEverything;
		this.pw = new PrintWriter(sw);
	}

	public TestResultWriter(boolean writeEverything, File out) throws FileNotFoundException {
		this.writeEverything = writeEverything;
		this.pw = new PrintWriter(out);
		this.doClose = true;
	}

	public void pass(String which, String description) {
		if (writeEverything) {
			pw.println(which + " PASS " + description);
			pw.flush();
		}
	}
	
	public void fail(String which, String description) {
		pw.println(which + " FAIL " + description);
		pw.flush();
	}

	public void error(String which, String description, Throwable t) {
		pw.println(which + " ERROR " + description);
		t.printStackTrace(pw);
		pw.flush();
	}

	public void println(String data) {
		pw.println(data);
		pw.flush();
	}

	public void close() {
		if (doClose)
			pw.close();
		else
			pw.flush();
	}
}
