package org.flasck.flas.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parsedForm.Scope;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.xml.XML;

public class CompileResult {
	public final ByteCodeEnvironment bce;
	private final Scope scope;
	private final List<File> jsFiles = new ArrayList<>();

	public CompileResult(Scope scope, ByteCodeEnvironment bce) {
		this.scope = scope;
		this.bce = bce;
	}

	public XML exports() {
		return XML.create("1.0", "Deprecated");
	}

	public NameOfThing getPackage() {
		return scope.scopeName;
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
