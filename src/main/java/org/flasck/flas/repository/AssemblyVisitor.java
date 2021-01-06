package org.flasck.flas.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import org.flasck.flas.parsedForm.assembly.ApplicationAssembly;
import org.zinutils.bytecode.ByteCodeEnvironment;

// At the end of the day, I don't think this is going to just be about assembly objects,
// but everything that is attached to them: FL code, web bits, etc.
public interface AssemblyVisitor {
	void visitAssembly(ApplicationAssembly a);
	void leaveAssembly(ApplicationAssembly a) throws IOException;
	void compiledPackageFile(File f);
	void includePackageFile(String s);
	void visitPackage(String pkg);
	void uploadJar(ByteCodeEnvironment bce, String s);
	void visitCardTemplate(String cardName, InputStream is, long length) throws IOException;
	void visitCSS(String name, ZipInputStream zis, long length) throws IOException;
	void visitResource(String name, ZipInputStream zis) throws IOException;
	void traversalDone() throws Exception;
}
