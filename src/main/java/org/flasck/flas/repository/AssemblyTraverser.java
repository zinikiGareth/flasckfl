package org.flasck.flas.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.assembly.Assembly;
import org.ziniki.splitter.SplitMetaData;

public class AssemblyTraverser implements AssemblyVisitor {
	private final AssemblyVisitor v;
	private final ErrorReporter errors;
	private final JSEnvironment jse;

	public AssemblyTraverser(ErrorReporter errors, JSEnvironment jse, AssemblyVisitor v) {
		this.errors = errors;
		this.jse = jse;
		this.v = v;
	}

	public void doTraversal(Repository repository) {
		for (RepositoryEntry e : repository.dict.values())
			visitEntry(e);
		for (SplitMetaData w : repository.allWebs()) {
			try (ZipInputStream zis = w.processedZip()) {
				ZipEntry ze;
				while ((ze = zis.getNextEntry()) != null) {
					if (ze.getName().startsWith("cards/")) {
						// long length = ze.getSize(); // this does not work because of https://bugs.openjdk.java.net/browse/JDK-8080092
						long length = w.getLength(ze.getName());
						visitCardTemplate(ze.getName().replace("cards/", ""), zis, length);
					} else
						System.out.println("Not yet handling " + ze.getName());
				}
			} catch (Exception ex) {
				errors.message((InputPosition)null, "error uploading web elements: " + ex);
			}
		}
		traversalDone();
	}

	private void visitEntry(RepositoryEntry e) {
		if (e instanceof Assembly)
			visitAssembly((Assembly) e);
	}

	@Override
	public void visitAssembly(Assembly a) {
		v.visitAssembly(a);
		for (File f : jse.files()) {
			compiledPackageFile(f);
		}
		leaveAssembly(a);
	}

	public void compiledPackageFile(File f) {
		v.compiledPackageFile(f);
	}

	public void leaveAssembly(Assembly a) {
		v.leaveAssembly(a);
	}

	@Override
	public void traversalDone() {
		v.traversalDone();
	}

	@Override
	public void visitCardTemplate(String replace, InputStream zis, long length) throws IOException {
		v.visitCardTemplate(replace, zis, length);
	}
	
}
