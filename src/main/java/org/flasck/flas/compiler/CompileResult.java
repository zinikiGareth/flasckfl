package org.flasck.flas.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.newtypechecker.TypeChecker2;
import org.flasck.flas.parsedForm.Scope;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.xml.XML;

public class CompileResult {
	private final String pkg;
	public final ByteCodeEnvironment bce;
	private final TypeChecker2 tc;
	private final Scope scope;
	private final List<File> jsFiles = new ArrayList<>();

	public CompileResult(String pkg, Scope scope, ByteCodeEnvironment bce, TypeChecker2 tc) {
		this.pkg = pkg;
		this.scope = scope;
		this.bce = bce;
		this.tc = tc;
	}

	public XML exports() {
		return tc.buildXML(pkg, false);
	}

	public String getPackage() {
		return pkg;
	}

	public Scope getScope() {
		if (scope == null)
			throw new NotImplementedException();
		return scope;
	}

	public CompileResult addJS(File file) {
		if (file != null)
			jsFiles.add(file);
		return this;
	}
	
	public Iterable<File> jsFiles() {
		return jsFiles;
	}
}
