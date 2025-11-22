package org.flasck.flas.repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.assembly.Assembly;
import org.flasck.jvm.ziniki.ContentObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.splitter.CSSFile;
import org.ziniki.splitter.CardData;
import org.ziniki.splitter.HTMLData;
import org.ziniki.splitter.Resource;
import org.ziniki.splitter.SplitMetaData;
import org.zinutils.bytecode.ByteCodeEnvironment;

public class AssemblyTraverser implements AssemblyVisitor {
	public static final Logger logger = LoggerFactory.getLogger("assembler");
	private final ErrorReporter errors;
	private final JSEnvironment jse;
	private final ByteCodeEnvironment bce;
	private final AssemblyVisitor v;

	public AssemblyTraverser(ErrorReporter errors, JSEnvironment jse, ByteCodeEnvironment bce, AssemblyVisitor v) {
		this.errors = errors;
		this.jse = jse;
		this.bce = bce;
		this.v = v;
	}

	public void doTraversal(Repository repository) {
		for (RepositoryEntry e : repository.dict.values())
			visitEntry(repository, e);
		try {
			traversalDone();
		} catch (Exception ex) {
			logger.error("Error uploading", ex);
			errors.message((InputPosition)null, "error uploading assembly: " + ex);
		}
	}

	public void traverse(Repository repository, Assembly asm) {
		visitEntry(repository, asm);
		try {
			traversalDone();
		} catch (Exception ex) {
			logger.error("Error uploading", ex);
			errors.message((InputPosition)null, "error uploading assembly: " + ex);
		}
	}

	private void visitWebInfo(SplitMetaData w) {
		try {
			for (HTMLData hm : w.htmls()) {
				for (String cardName : hm.cards()) {
					CardData cd = hm.forCard(cardName);
					visitCardTemplate(cd.id(), new ByteArrayInputStream(cd.template().getBytes()));
				}
			}
			for (CSSFile cssFile : w.cssFiles()) {
				visitCSS(cssFile.name(), new ByteArrayInputStream(cssFile.text().getBytes()));
			}
			for (Resource r : w.resources()) {
				visitResource(r.name(), r.asStream());
			}
		} catch (Exception ex) {
			logger.error("Error uploading", ex);
			errors.message((InputPosition)null, "error uploading web elements: " + ex);
		}
	}

	private void visitEntry(Repository repository, RepositoryEntry e) {
		if (e instanceof Assembly)
			traverseAssemblyWithWebs(repository, (Assembly) e);
	}

	public void traverseAssemblyWithWebs(Repository repository, Assembly a) {
		try {
			visitAssembly(a);
			for (ContentObject co : jse.jsIncludes("live")) {
				includePackageFile(co);
			}
			for (String s : jse.packageStrings()) {
				if (s.contains("_ut_") || s.contains("_st_") || s.endsWith("_ut") || s.endsWith("_st"))
					continue;
				logger.info("have package " + s);
				visitPackage(s);
				uploadJar(bce, s);
			}
			SplitMetaData allWebs = repository.allWebs();
			visitWebInfo(allWebs);
			leaveAssembly(a);
		} catch (Exception ex) {
			logger.error("Error uploading", ex);
			errors.message((InputPosition)null, "error uploading assembly: " + ex);
		}
	}

	public void uploadJar(ByteCodeEnvironment bce, String s) {
		v.uploadJar(bce, s);
	}

	public void visitAssembly(Assembly a) {
		v.visitAssembly(a);
	}

	public void includePackageFile(ContentObject co) {
		v.includePackageFile(co);
	}

	@Override
	public void visitPackage(String pkg) {
		v.visitPackage(pkg);
	}

	@Override
	public void visitCardTemplate(String name, InputStream zis) throws IOException {
		v.visitCardTemplate(name, zis);
	}

	public void visitCSS(String name, InputStream is) throws IOException {
		v.visitCSS(name, is);
	}

	public void visitResource(String name, InputStream is) throws IOException {
		v.visitResource(name, is);
	}

	@Override
	public void leaveAssembly(Assembly a) throws IOException {
		v.leaveAssembly(a);
	}

	@Override
	public void traversalDone() throws Exception {
		v.traversalDone();
	}
}
