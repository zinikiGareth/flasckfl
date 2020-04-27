package org.flasck.flas.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.flasck.flas.parsedForm.assembly.Assembly;

public class AssemblyLeaves implements AssemblyVisitor {

	@Override
	public void visitAssembly(Assembly a) {
	}

	@Override
	public void leaveAssembly(Assembly a) {
	}

	@Override
	public void compiledPackageFile(File f) {
	}
	
	@Override
	public void visitCardTemplate(String cardName, InputStream is, long length) throws IOException {
	}

	@Override
	public void traversalDone() {
	}
}
