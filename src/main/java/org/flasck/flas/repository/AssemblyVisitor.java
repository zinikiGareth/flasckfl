package org.flasck.flas.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.flasck.flas.parsedForm.assembly.Assembly;

// At the end of the day, I don't think this is going to just be about assembly objects,
// but everything that is attached to them: FL code, web bits, etc.
public interface AssemblyVisitor {
	void visitAssembly(Assembly a);
	void leaveAssembly(Assembly a);
	void compiledPackageFile(File f);
	void visitCardTemplate(String cardName, InputStream is, long length) throws IOException;
	void traversalDone();
}
