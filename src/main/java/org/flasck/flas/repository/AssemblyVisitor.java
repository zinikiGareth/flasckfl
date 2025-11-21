package org.flasck.flas.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import org.flasck.flas.parsedForm.assembly.Assembly;
import org.flasck.jvm.ziniki.ContentObject;
import org.zinutils.bytecode.ByteCodeEnvironment;

// At the end of the day, I don't think this is going to just be about assembly objects,
// but everything that is attached to them: FL code, web bits, etc.
public interface AssemblyVisitor {
	void visitAssembly(Assembly a);
	void leaveAssembly(Assembly a) throws IOException;
	void includePackageFile(ContentObject co);
	void visitPackage(String pkg);
	void uploadJar(ByteCodeEnvironment bce, String s);
	void visitCardTemplate(String cardName, InputStream is) throws IOException;
	void visitCSS(String name, ZipInputStream zis, long length) throws IOException;
	void visitResource(String name, ZipInputStream zis) throws IOException;
	void traversalDone() throws Exception;
}
