package org.flasck.flas.testrunner;

import java.util.List;

import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.ziniki.server.grizzly.GrizzlyTDAServer;
import org.ziniki.servlet.tda.RequestProcessor;
import org.ziniki.servlet.tda.Responder;
import org.ziniki.ziwsh.intf.Param;

public class BridgeGenHandler implements RequestProcessor {
	private final GrizzlyTDAServer server;
	private final boolean isSecure;
	private final List<UnitTestCase> tests;

	public BridgeGenHandler(@Param("server") GrizzlyTDAServer server, @Param("secure") boolean isSecure, @Param("unitTests") List<UnitTestCase> tests) {
		this.server = server;
		this.isSecure = isSecure;
		this.tests = tests;
	}
	
	@Override
	public void process(Responder r) throws Exception {
		r.setStatus(200);
		r.setContentType("text/javascript");
//		UnitTestFileName utn = new UnitTestFileName(new PackageName("test.golden"), "_ut_checkhello");
		StringBuilder sb = new StringBuilder();
		sb.append("import { WSBridge } from '/js/flasjava.js';\n");
		for (UnitTestCase c : tests) {
			sb.append("import { " + c.name.container().jsName() +" } from '/js/" + c.name.container().uniqueName() + ".js';\n");
			break;
		}
		sb.append("\n");
		for (UnitTestCase c : tests) {
			sb.append("var bridge = new WSBridge('localhost', " + server.getPort() + ", " + c.name.container().jsName() + ");\n"); // and secure ...
			sb.append("bridge.send({ action: 'unit', names: Object.keys(" + c.name.container().jsName() + ")});\n");
			break;
		}
//		sb.append("\n");
//		sb.append("globalThis.bridge = bridge;\n");
		
//		String msg = "var heteroSecure = " + isSecure + "\nvar heteroPort = " + server.getPort() + "\nexport { heteroSecure, heteroPort };\n";
		String msg = sb.toString();
		r.setContentLength(msg.getBytes().length);
		r.write(msg, null);
		r.done();
	}

}
