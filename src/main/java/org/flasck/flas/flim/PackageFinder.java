package org.flasck.flas.flim;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.ArgumentException;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.rewriter.Rewriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.xml.XML;

public class PackageFinder {
	private final static Logger logger = LoggerFactory.getLogger("Compiler");
	private final Rewriter rw;
	private final List<File> dirs;
	final Map<String, ImportPackage> imported = new HashMap<String, ImportPackage>();
	
	public PackageFinder(Rewriter rw, List<File> pkgdirs, ImportPackage rootPkg) {
		this.rw = rw;
		dirs = pkgdirs;
		imported.put("", rootPkg);
	}

	public void loadFlim(ErrorResult errors, String pkgName) {
		if (imported.containsKey(pkgName))
			return;
		for (File d : dirs) {
			File flim = new File(d, pkgName + ".flim");
			if (flim.canRead()) {
				// Load definitions into it
				try {
					logger.info("Loading definitions for " + pkgName + " from " + flim);
					XML xml = XML.fromFile(flim);
					PackageImporter.importInto(this, errors, rw, pkgName, xml);
				} catch (Exception ex) {
					ex.printStackTrace();
					errors.message((Block)null, ex.toString());
				} finally {
				}
			}
		}
	}

	public void searchIn(File file) {
		if (file == null || !file.isDirectory())
			throw new ArgumentException("Cannot search for FLIMs in " + file + " as it is not a valid directory");
		dirs.add(file);
	}
}
