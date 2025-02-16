package org.flasck.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.ziniki.server.di.DehydratedHandler;
import org.ziniki.server.di.Instantiator;
import org.ziniki.server.di.MakeAHandler;
import org.ziniki.servlet.tda.RequestProcessor;
import org.ziniki.servlet.tda.Responder;
import org.ziniki.servlet.tda.TDAConfiguration;
import org.ziniki.ziwsh.intf.WSProcessor;
import org.zinutils.sync.LockingCounter;

public class ServeSystemTests implements JSTestController {
	public static class EmptyGenHandler implements RequestProcessor {
		public EmptyGenHandler() {
		}
		
		@Override
		public void process(Responder r) throws Exception {
			StringBuilder sb = new StringBuilder();
			sb.append("import { exposeTests } from \"/js/flastest.js\";\n");
			sb.append("exposeTests(window);\n");

			r.setStatus(200);
			r.setContentType("text/javascript");
			String msg = sb.toString();
			r.setContentLength(msg.getBytes().length);
			r.write(msg, null);
			r.done();
		}

	}

	public static void main(String[] argv) throws Exception {
		ServeSystemTests me = new ServeSystemTests();
		me.parse(argv);
		me.prepare();
		me.go();
		Thread.sleep(5000000);
	}

	private int port = 14040;
	private File testdir = new File("/Users/gareth/Ziniki/Over/stdlib/system-tests/src/golden/routing/downagain");
	private File flasckdir = new File("/Users/gareth/Ziniki/Over/FLAS2/src/main/resources/flasck");
	private JSTestServer server;
	private CountDownLatch waitForBridge;
	protected BrowserJSJavaBridge bridge;
	final LockingCounter counter = new LockingCounter();

	private void parse(String[] argv) {
		// TODO Auto-generated method stub
		
	}

	private void prepare() throws Exception {
		server = new JSTestServer(port, testdir, flasckdir);
		waitForBridge = new CountDownLatch(1);
		server.configure();
		{
			Map<String, Object> items = new TreeMap<>();
			Map<String, Object> map = new TreeMap<>();
			map.put("class", EmptyGenHandler.class.getName());
			server.addWebTree("/gen/*", new DehydratedHandler<>(new Instantiator("gen", map), items));
		}
		server.addWSTree("/bridge", new MakeAHandler<WSProcessor>() {
			@Override
			public WSProcessor instantiate(TDAConfiguration c) throws Exception {
				BrowserJSJavaBridge bridge = new BrowserJSJavaBridge(ServeSystemTests.this, this.getClass().getClassLoader(), testdir, counter);
				ServeSystemTests.this.bridge = bridge;
				waitForBridge.countDown();
				return bridge;
			}
		});

	}

	private void go() throws IOException, InterruptedException, TimeoutException {
		server.go();
		if (!waitForBridge.await(10, TimeUnit.SECONDS)) {
			throw new TimeoutException("bridge not established");
		}
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
						counter.start("runstep " + s);
						bridge.runStep(s);
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
