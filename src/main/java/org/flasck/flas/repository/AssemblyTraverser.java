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
		for (RepositoryEntry e : repository.dict.values())
			visitEntry(repository, e);
		try {
			traversalDone();
		} catch (Exception ex) {
			errors.message((InputPosition)null, "error uploading assembly: " + ex);
		}
	}

	private void visitWebInfo(SplitMetaData w) {
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
				} else if (name.startsWith("items/")) {
					if (name.endsWith(".html")) {
						// long length = ze.getSize(); // this does not work because of https://bugs.openjdk.java.net/browse/JDK-8080092
						long length = w.getLength(name);
						visitCardTemplate(name.replace(".html", ""), zis, length);
					} else if (name.endsWith(".json"))
						;
					else
						throw new NotImplementedException("cannot handle " + name);
				} else if (name.endsWith(".css"))
					visitCSS(name, zis, ze.getSize());
				else
					visitResource(name, zis);
			}
		} catch (Exception ex) {
			logger.error("Error uploading", ex);
			errors.message((InputPosition)null, "error uploading web elements: " + ex);
		}
	}

	private void visitEntry(Repository repository, RepositoryEntry e) {
		if (e instanceof Assembly)
			traverseAssemblyWithWebs(repository, (ApplicationAssembly) e);
	}

	public void traverseAssemblyWithWebs(Repository repository, ApplicationAssembly a) {
		try {
			visitAssembly(a);
			logger.info("have files: " + jse);
			for (String s : jse.packages()) {
				logger.info("have package " + s);
				if (s.contains("_ut_") || s.contains("_st_"))
					continue;
				visitPackage(s);
				File f = jse.fileFor(s);
				if (f != null)
					compiledPackageFile(f);
				else
					includePackageFile(new File("packages/js", s + ".js"));
			}
			Iterable<SplitMetaData> allWebs = repository.allWebs();
			for (SplitMetaData w : allWebs)
				visitWebInfo(w);
			leaveAssembly(a);
		} catch (Exception ex) {
			logger.error("Error uploading", ex);
			errors.message((InputPosition)null, "error uploading assembly: " + ex);
		}
	}

	public void visitAssembly(ApplicationAssembly a) {
		v.visitAssembly(a);
	}

	public void compiledPackageFile(File f) {
		v.compiledPackageFile(f);
	}

	public void includePackageFile(File f) {
		v.includePackageFile(f);
	}

	@Override
	public void visitPackage(String pkg) {
		v.visitPackage(pkg);
	}

	@Override
	public void visitCardTemplate(String replace, InputStream zis, long length) throws IOException {
		v.visitCardTemplate(replace, zis, length);
	}

	public void visitCSS(String name, ZipInputStream zis, long length) throws IOException {
		v.visitCSS(name, zis, length);
	}

	public void visitResource(String name, ZipInputStream zis) throws IOException {
		v.visitResource(name, zis);
	}

	public void leaveAssembly(ApplicationAssembly a) throws IOException {
		v.leaveAssembly(a);
	}

	@Override
	public void traversalDone() throws Exception {
		v.traversalDone();
	}
	
}
