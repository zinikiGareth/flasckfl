package org.flasck.flas.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.assembly.ApplicationAssembly;
import org.flasck.flas.parsedForm.assembly.Assembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.splitter.SplitMetaData;
import org.zinutils.exceptions.NotImplementedException;

public class AssemblyTraverser implements AssemblyVisitor {
	public static final Logger logger = LoggerFactory.getLogger("assembler");
	private final AssemblyVisitor v;
	private final ErrorReporter errors;
	private final JSEnvironment jse;

	public AssemblyTraverser(ErrorReporter errors, JSEnvironment jse, AssemblyVisitor v) {
		this.errors = errors;
		this.jse = jse;
		this.v = v;
	}

	public void doTraversal(Repository repository) {
		for (SplitMetaData w : repository.allWebs()) {
			try (ZipInputStream zis = w.processedZip()) {
				ZipEntry ze;
				while ((ze = zis.getNextEntry()) != null) {
					String name = ze.getName();
					if (name.startsWith("cards/")) {
						if (name.endsWith(".html")) {
							// long length = ze.getSize(); // this does not work because of https://bugs.openjdk.java.net/browse/JDK-8080092
							long length = w.getLength(name);
							visitCardTemplate(name.replace(".html", ""), zis, length);
						} else if (name.endsWith(".json"))
							;
						else
							throw new NotImplementedException("cannot handle " + name);
					} else
						System.out.println("Not yet handling " + name);
				}
			} catch (Exception ex) {
				logger.error("Error uploading", ex);
				errors.message((InputPosition)null, "error uploading web elements: " + ex);
			}
		}
		for (RepositoryEntry e : repository.dict.values())
			visitEntry(e);
		try {
			traversalDone();
		} catch (Exception ex) {
			errors.message((InputPosition)null, "error uploading assembly: " + ex);
		}
	}

	private void visitEntry(RepositoryEntry e) {
		if (e instanceof Assembly)
			visitAssembly((ApplicationAssembly) e);
	}

	@Override
	public void visitAssembly(ApplicationAssembly a) {
		try {
			v.visitAssembly(a);
			for (File f : jse.files()) {
				compiledPackageFile(f);
			}
			leaveAssembly(a);
		} catch (Exception ex) {
			logger.error("Error uploading", ex);
			errors.message((InputPosition)null, "error uploading assembly: " + ex);
		}
	}

	public void compiledPackageFile(File f) {
		v.compiledPackageFile(f);
	}

	public void leaveAssembly(Assembly a) throws IOException {
		v.leaveAssembly(a);
	}

	@Override
	public void traversalDone() throws Exception {
		v.traversalDone();
	}

	@Override
	public void visitCardTemplate(String replace, InputStream zis, long length) throws IOException {
		v.visitCardTemplate(replace, zis, length);
	}
	
}
