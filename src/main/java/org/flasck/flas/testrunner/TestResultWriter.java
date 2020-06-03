package org.flasck.flas.testrunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class TestResultWriter {
	private final boolean writeEverthing;
	private final PrintWriter pw;

	public TestResultWriter(boolean writeEverthing, PrintStream out) {
		this.writeEverthing = writeEverthing;
		this.pw = new PrintWriter(out);
	}
	
	public TestResultWriter(boolean writeEverything, StringWriter sw) {
		writeEverthing = writeEverything;
		this.pw = new PrintWriter(sw);
	}

	public TestResultWriter(boolean writeEverthing, File out) throws FileNotFoundException {
		this.writeEverthing = writeEverthing;
		this.pw = new PrintWriter(out);
	}

	public void pass(String which, String description) {
		if (writeEverthing) {
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
		pw.close();
	}
}
