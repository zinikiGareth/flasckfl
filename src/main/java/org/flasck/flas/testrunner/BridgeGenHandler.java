package org.flasck.flas.testrunner;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.ziniki.server.grizzly.GrizzlyTDAServer;
import org.ziniki.servlet.tda.RequestProcessor;
import org.ziniki.servlet.tda.Responder;
import org.ziniki.ziwsh.intf.Param;
import org.zinutils.exceptions.InvalidUsageException;
import org.zinutils.utils.FileUtils;

public class BridgeGenHandler implements RequestProcessor {
	private final GrizzlyTDAServer server;
	private final File moduleDir;
	private final Iterable<PackageName> sources;
	private final List<UnitTestCase> unittests;
	private final List<SystemTest> systests;
	private final List<String> modules;

	public BridgeGenHandler(@Param("server") GrizzlyTDAServer server, @Param("moduleDir") File moduleDir, @Param("sources") Iterable<PackageName> sources, @Param("unitTests") List<UnitTestCase> unittests, @Param("systemTests") List<SystemTest> systests, @Param("modules") List<String> modules) {
		this.server = server;
		this.moduleDir = moduleDir;
		this.sources = sources;
		this.unittests = unittests;
		this.systests = systests;
		this.modules = modules;
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
		int mk = 0, tk = 0;
		for (String n : modules) {
			File md = new File(moduleDir, n);
			if (!md.isDirectory()) {
				throw new InvalidUsageException("there is no module called " + n + " in " + moduleDir);
			}
			File mjs = new File(md, "js");
			File core = new File(mjs, "core");
			if (core.isDirectory()) {
				for (File f : FileUtils.findFilesMatching(core, "*.js")) {
					sb.append("import { module_init as module_init_" + (mk++) +" } from '/js/" + f.getName() + "';\n");
				}
			}
			File mock = new File(mjs, "mock");
			if (mock.isDirectory()) {
				for (File f : FileUtils.findFilesMatching(mock, "*.js")) {
					sb.append("import { installer as installer_" + (tk++) + " } from '/js/" + f.getName() + "';\n");
				}
			}
		}
		sb.append("\n");

		sb.append("var bridge = new WSBridge('localhost', " + server.getPort()  + ");\n");
		Set<NameOfThing> uns = new HashSet<>();
		for (UnitTestCase c : unittests) {
			uns.add(c.name.container());
		}
		for (NameOfThing n : uns) {
			sb.append("bridge.addUnitTest('" + n.uniqueName() + "', " + n.jsName() + ");\n");
		}
		Set<NameOfThing> sns = new HashSet<>();
		for (SystemTest c : systests) {
			sns.add(c.name());
		}
		for (NameOfThing n : sns) {
			sb.append("bridge.addSystemTest('" + n.uniqueName() + "', " + n.jsName() + ");\n");
		}
		for (int i=0;i<mk;i++) {
			sb.append("module_init_" + i + "(bridge.runner);\n");
		}
		for (int i=0;i<tk;i++) {
			sb.append("installer_" + i + "(bridge);\n");
		}
		sb.append("bridge.send({ action: 'ready' });\n");

		String msg = sb.toString();
		r.setContentLength(msg.getBytes().length);
		r.write(msg, null);
		r.done();
	}

}
