package org.flasck.flas.testrunner.chrome;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;

import org.flasck.flas.testrunner.JSTestServer;
import org.ziniki.server.di.DehydratedHandler;
import org.ziniki.server.di.Instantiator;
import org.ziniki.server.di.MakeAHandler;
import org.ziniki.servlet.tda.RequestProcessor;
import org.ziniki.servlet.tda.Responder;
import org.ziniki.servlet.tda.TDAConfiguration;
import org.ziniki.ziwsh.intf.WSProcessor;
import org.zinutils.exceptions.InvalidUsageException;

public class ServeTestsToChrome {
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
		ServeTestsToChrome me = new ServeTestsToChrome();
		me.parse(argv);
		me.prepare();
		me.go();
		Thread.sleep(5000000);
	}

	private int port = 14040;
	private String rooturl;
	private File testdir;
	private File flasckdir = new File("/Users/gareth/Ziniki/Over/FLAS2/src/main/resources/flasck");
	private JSTestServer server;

	private void parse(String[] argv) {
		String cwd = System.getProperty("user.dir");
		testdir = new File(cwd);
		// TODO: some tests use other dirs, so we need an arg for "test.golden"
		String srcdir = "test.golden";
		if (!new File(testdir, srcdir).isDirectory()) {
			throw new InvalidUsageException("there is no directory '" + srcdir + "' in '" + testdir + "'");
		}
		rooturl = "http://localhost:" + port;
	}

	private void prepare() throws Exception {
		server = new JSTestServer(port, testdir, flasckdir);
		server.configure();
		{
			Map<String, Object> items = new TreeMap<>();
			Map<String, Object> map = new TreeMap<>();
			map.put("class", HelpThemHandler.class.getName());
			map.put("basedir", testdir);
			map.put("rooturl", rooturl);
			server.addWebTree("/", new DehydratedHandler<>(new Instantiator("help", map), items));
		}
		{
			Map<String, Object> items = new TreeMap<>();
			Map<String, Object> map = new TreeMap<>();
			map.put("class", EmptyGenHandler.class.getName());
			server.addWebTree("/gen/*", new DehydratedHandler<>(new Instantiator("gen", map), items));
		}
		server.addWSTree("/bridge", new MakeAHandler<WSProcessor>() {
			@Override
			public WSProcessor instantiate(TDAConfiguration c) throws Exception {
				ChromeTestController forTest = new ChromeTestController(testdir);
				return forTest.bridge;
			}
		});

	}

	private void go() throws IOException, InterruptedException, TimeoutException {
		server.go();
	}
}
