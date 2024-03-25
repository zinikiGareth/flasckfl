package org.flasck.flas.testrunner;

import java.util.LinkedHashSet;
import java.util.List;

import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.ziniki.server.grizzly.GrizzlyTDAServer;
import org.ziniki.servlet.tda.RequestProcessor;
import org.ziniki.servlet.tda.Responder;
import org.ziniki.ziwsh.intf.Param;

public class BridgeGenHandler implements RequestProcessor {
	private final GrizzlyTDAServer server;
	private final List<UnitTestCase> tests;

	public BridgeGenHandler(@Param("server") GrizzlyTDAServer server, @Param("unitTests") List<UnitTestCase> tests) {
		this.server = server;
		this.tests = tests;
	}
	
	@Override
	public void process(Responder r) throws Exception {
		r.setStatus(200);
		r.setContentType("text/javascript");
//		UnitTestFileName utn = new UnitTestFileName(new PackageName("test.golden"), "_ut_checkhello");
		StringBuilder sb = new StringBuilder();
		sb.append("import { WSBridge } from '/js/flasjava.js';\n");
		LinkedHashSet<UnitTestFileName> names = new LinkedHashSet<>();
		for (UnitTestCase c : tests) {
			names.add((UnitTestFileName) c.name.container());
		}
		for (UnitTestFileName n : names) {
			sb.append("import { " + n.jsName() +" } from '/js/" + n.uniqueName() + ".js';\n");
		}
		sb.append("\n");
		sb.append("var bridge = new WSBridge('localhost', " + server.getPort()  + ");\n");
		for (UnitTestFileName n : names) {
			sb.append("bridge.addtest('" + n.uniqueName() + "', " + n.jsName() + ");\n");
		}
		sb.append("bridge.send({ action: 'ready' });\n");

		String msg = sb.toString();
		r.setContentLength(msg.getBytes().length);
		r.write(msg, null);
		r.done();
	}

}
