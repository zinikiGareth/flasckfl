package org.flasck.flas.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import org.flasck.flas.parsedForm.assembly.ApplicationAssembly;

public class AssemblyLeaves implements AssemblyVisitor {

	@Override
	public void visitAssembly(ApplicationAssembly a) {
	}

	@Override
	public void leaveAssembly(ApplicationAssembly a) throws IOException {
	}

	@Override
	public void compiledPackageFile(File f) {
	}
	
	@Override
	public void visitPackage(String pkg) {
	}

	@Override
	public void visitCardTemplate(String cardName, InputStream is, long length) throws IOException {
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
