package org.flasck.flas.testrunner;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.ziniki.server.grizzly.GrizzlyTDAServer;
import org.ziniki.servlet.tda.RequestProcessor;
import org.ziniki.servlet.tda.Responder;
import org.ziniki.ziwsh.intf.Param;

public class BridgeGenHandler implements RequestProcessor {
	private final GrizzlyTDAServer server;
	private final boolean isSecure;

	public BridgeGenHandler(@Param("server") GrizzlyTDAServer server, @Param("secure") boolean isSecure) {
		this.server = server;
		this.isSecure = isSecure;
	}
	
	@Override
	public void process(Responder r) throws Exception {
		r.setStatus(200);
		r.setContentType("text/javascript");
		UnitTestFileName utn = new UnitTestFileName(new PackageName("test.golden"), "_ut_checkhello");
		StringBuilder sb = new StringBuilder();
		sb.append("import { WSBridge } from '/js/flasjava.js';\n");
		sb.append("import { " + utn.jsName() +" } from '/js/" + utn.uniqueName() + ".js';\n");
		sb.append("\n");
		sb.append("var bridge = new WSBridge('localhost', " + server.getPort() + ", " + utn.jsName() + ");\n"); // and secure ...
		sb.append("bridge.send({ action: 'unit', names: Object.keys(" + utn.jsName() + ")});\n");
//		sb.append("\n");
//		sb.append("globalThis.bridge = bridge;\n");
		
//		String msg = "var heteroSecure = " + isSecure + "\nvar heteroPort = " + server.getPort() + "\nexport { heteroSecure, heteroPort };\n";
		String msg = sb.toString();
		r.setContentLength(msg.getBytes().length);
		r.write(msg, null);
		r.done();
	}

}
