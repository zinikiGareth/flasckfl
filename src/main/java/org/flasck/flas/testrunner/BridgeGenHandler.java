package org.flasck.flas.testrunner;

import java.util.LinkedHashSet;
import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.ziniki.server.grizzly.GrizzlyTDAServer;
import org.ziniki.servlet.tda.RequestProcessor;
import org.ziniki.servlet.tda.Responder;
import org.ziniki.ziwsh.intf.Param;

public class BridgeGenHandler implements RequestProcessor {
	private final GrizzlyTDAServer server;
	private final Iterable<PackageName> sources;
	private final List<UnitTestCase> unittests;
	private final List<SystemTest> systests;

	public BridgeGenHandler(@Param("server") GrizzlyTDAServer server, @Param("sources") Iterable<PackageName> sources, @Param("unitTests") List<UnitTestCase> unittests, @Param("systemTests") List<SystemTest> systests) {
		this.server = server;
		this.sources = sources;
		this.unittests = unittests;
		this.systests = systests;
	}
	
	@Override
	public void process(Responder r) throws Exception {
		r.setStatus(200);
		r.setContentType("text/javascript");
		StringBuilder sb = new StringBuilder();
		sb.append("import { WSBridge } from '/js/flasjava.js';\n");
		for (NameOfThing n : sources) {
			sb.append("import { " + n.jsName() +" } from '/js/" + n.uniqueName() + ".js';\n");
		}
		sb.append("\n");

		LinkedHashSet<NameOfThing> testNames = new LinkedHashSet<>();
		for (UnitTestCase c : unittests) {
			testNames.add(c.name.container());
		}
		for (SystemTest c : systests) {
			testNames.add(c.name());
		}
		sb.append("var bridge = new WSBridge('localhost', " + server.getPort()  + ");\n");
		for (NameOfThing n : testNames) {
			sb.append("bridge.addtest('" + n.uniqueName() + "', " + n.jsName() + ");\n");
		}
		sb.append("bridge.send({ action: 'ready' });\n");

		String msg = sb.toString();
		r.setContentLength(msg.getBytes().length);
		r.write(msg, null);
		r.done();
	}

}
