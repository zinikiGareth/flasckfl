package org.flasck.flas.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import org.flasck.flas.parsedForm.assembly.ApplicationAssembly;
import org.flasck.flas.parsedForm.assembly.Assembly;
import org.ziniki.interfaces.ContentObject;

public class AssemblyLeaves implements AssemblyVisitor {

	@Override
	public void visitAssembly(ApplicationAssembly a) {
	}

	@Override
	public void leaveAssembly(Assembly a) throws IOException {
	}

	@Override
	public void compiledPackageFile(File f) {
	}
	
	@Override
	public void visitCardTemplate(String cardName, InputStream is, long length) throws IOException {
	}

	@Override
	public void visitCSS(String name, ContentObject co) {
	}

	@Override
	public ContentObject visitResource(String name, ZipInputStream zis) throws IOException {
		return null;
	}

	@Override
	public void traversalDone() throws Exception {
	}
}
