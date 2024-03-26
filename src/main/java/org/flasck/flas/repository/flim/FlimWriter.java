package org.flasck.flas.repository.flim;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Set;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.packaging.JSUploader;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.Traverser;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.utils.FileUtils;

public class FlimWriter {
	private final Repository repository;
	private final File flimdir;
	private final JSUploader uploader;

	public FlimWriter(Repository repository, File flimdir) {
		this.repository = repository;
		this.flimdir = flimdir;
		this.uploader = null;
	}

	public FlimWriter(Repository repository, JSUploader uploader) {
		try {
			this.repository = repository;
			this.flimdir = Files.createTempDirectory("flimdir").toFile();
			this.uploader = uploader;
		} catch (Exception ex) {
			throw WrappedException.wrap(ex);
		}
	}

	public Set<String> export(String pkg) {
		try {
			FileUtils.assertDirectory(flimdir);

			File file = new File(flimdir, pkg + ".flim");
			PrintWriter pw = new PrintWriter(file);
			IndentWriter iw = new IndentWriter(pw, "\t").indent();
			PackageName pkgName;
			if ("root.package".equals(pkg))
				pkgName = new PackageName(false);
			else
				pkgName = new PackageName(pkg);
			FlimVisitor vizier = new FlimVisitor(pkgName, iw);
			new Traverser(vizier).forPackage(pkgName).withObjectMethods().doTraversal(repository);
			for (String s : vizier.referencedPackages())
				iw.println("usespackage " + s);
			pw.close();
			if (uploader != null) {
				uploader.uploadFlim(file);
			}
			return vizier.referencedPackages();
		} catch (FileNotFoundException e) {
			System.out.println("could not write flim " + pkg + " to " + flimdir);
			return null;
		}
	}

}
