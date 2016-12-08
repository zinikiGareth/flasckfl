package org.flasck.flas.compiler;

import org.flasck.flas.newtypechecker.TypeChecker2;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.xml.XML;

public class CompileResult {
	private final String pkg;
	public final ByteCodeEnvironment bce;
	private final TypeChecker2 tc;

	public CompileResult(String pkg, ByteCodeEnvironment bce, TypeChecker2 tc) {
		this.pkg = pkg;
		this.bce = bce;
		this.tc = tc;
	}

	public XML exports() {
		return tc.buildXML(pkg, false);
	}

	public String getPackage() {
		return pkg;
	}

}
