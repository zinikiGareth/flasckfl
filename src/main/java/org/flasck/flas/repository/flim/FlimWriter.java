package org.flasck.flas.repository.flim;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.Traverser;
import org.zinutils.bytecode.mock.IndentWriter;

public class FlimWriter {
	private final Repository repository;
	private final File flimdir;

	public FlimWriter(Repository repository, File flimdir) {
		this.repository = repository;
		this.flimdir = flimdir;
	}

	public boolean export(String pkg) {
		try {
			PrintWriter pw = new PrintWriter(new File(flimdir, pkg));
			IndentWriter iw = new IndentWriter(pw, "\t").indent();
			String pkgName = null;
			if (!"root.package".equals(pkg))
				pkgName = pkg;
			new Traverser(new FlimVisitor(pkgName, iw)).withObjectMethods().doTraversal(repository);
			pw.close();
			return true;
		} catch (FileNotFoundException e) {
			System.out.println("could not write flim " + pkg + " to " + flimdir);
			return false;
		}
	}

}
