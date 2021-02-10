package org.flasck.flas.testrunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.flasck.flas.parsedForm.st.SystemTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestResultWriter {
	private static final Logger logger = LoggerFactory.getLogger("TestStages");
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

	public void systemTest(String which, SystemTest st) {
		if (writeEverything) {
			pw.println(which + " running system test " + st.name().uniqueName());
		}
	}

	public void begin(String which, String description) {
		logger.info(which + " STEP " + description);
	}
	
	public void pass(String which, String description) {
		if (writeEverything) {
			logger.info(which + " PASS " + description);
			pw.println(which + " PASS " + description);
			pw.flush();
		}
	}
	
	public void fail(String which, String description) {
		logger.info(which + " FAIL " + description);
		pw.println(which + " FAIL " + description);
		pw.flush();
	}

	public void error(String which, String description, Throwable t) {
		if (description == null)
			description = "configure";
		logger.info(which + " ERROR " + description);
		pw.println(which + " ERROR " + description);
		t.printStackTrace(pw);
		pw.flush();
	}

	public void passedSystemTest(String which, SystemTest st) {
		if (writeEverything) {
			pw.println(which + " " + st.name().uniqueName() + " all stages passed");
		}
	}

	public void println(String data) {
		logger.info(data);
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
