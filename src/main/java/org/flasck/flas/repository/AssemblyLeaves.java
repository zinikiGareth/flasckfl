package org.flasck.flas.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import org.flasck.flas.parsedForm.assembly.Assembly;
import org.flasck.jvm.ziniki.ContentObject;
import org.zinutils.bytecode.ByteCodeEnvironment;

public class AssemblyLeaves implements AssemblyVisitor {

	@Override
	public void visitAssembly(Assembly a) {
	}

	@Override
	public void leaveAssembly(Assembly a) throws IOException {
	}

	@Override
	public void includePackageFile(ContentObject co) {
	}

	@Override
	public void visitPackage(String pkg) {
	}

	@Override
	public void uploadJar(ByteCodeEnvironment bce, String s) {
	}

	@Override
	public void visitCardTemplate(String cardName, InputStream is) throws IOException {
	}

	@Override
	public void visitCSS(String name, ZipInputStream zis, long length) throws IOException {
	}

	@Override
	public void visitResource(String name, ZipInputStream zis) throws IOException {
	}

	@Override
	public void traversalDone() throws Exception {
	}
}
