package org.flasck.flas.testrunner.chrome;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.flasck.flas.testrunner.BrowserJSJavaBridge;
import org.flasck.flas.testrunner.JSTestController;
import org.zinutils.sync.LockingCounter;

public class ChromeTestController implements JSTestController {
	protected BrowserJSJavaBridge bridge;
	final LockingCounter counter = new LockingCounter();

	public ChromeTestController(File testdir) {
		bridge = new BrowserJSJavaBridge(this, this.getClass().getClassLoader(), testdir, counter);
	}

	@Override
	public void ready() {
		System.out.println("ready called");
	}

	@Override
	public void stepsForTest(List<String> steps) {
		System.out.println("steps for test: " + steps);
		new Thread() {
			public void run() {
				Iterator<String> it = steps.iterator();
				try {
					while (it.hasNext()) {
						String s = it.next();
						counter.waitForZero(10, TimeUnit.MINUTES); // wait a long time because they may be in the debugger
						int ri = counter.newRequestId();
						counter.start(ri, "runstep " + s);
						bridge.runStep(ri, s);
					}
					counter.waitForZero(10, TimeUnit.MINUTES); // wait a long time because they may be in the debugger
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}.start();
	}

	@Override
	public void systemTestPrepared() {
		System.out.println("system test ready to go");
	}

	@Override
	public void error(String err) {
		System.out.println("error: " + err);
	}
}
