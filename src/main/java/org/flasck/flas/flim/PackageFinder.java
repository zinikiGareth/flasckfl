package org.flasck.flas.flim;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.ArgumentException;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.errors.ErrorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.xml.XML;

public class PackageFinder {
	private final static Logger logger = LoggerFactory.getLogger("Compiler");
	private final List<File> dirs;
	private final Map<String, ImportPackage> imported = new HashMap<String, ImportPackage>();
	
	public PackageFinder(List<File> pkgdirs, ImportPackage rootPkg) {
		dirs = pkgdirs;
		imported.put("", rootPkg);
	}

	public ImportPackage loadFlim(ErrorResult errors, String pkgName) {
		for (File d : dirs) {
			File flim = new File(d, pkgName + ".flim");
			if (flim.canRead()) {
				// Load definitions into it
				try {
					logger.info("Loading definitions for " + pkgName + " from " + flim);
					XML xml = XML.fromFile(flim);
					ImportPackage ret = new ImportPackage(pkgName);
					/*
					@SuppressWarnings("unchecked")
					List<RWStructDefn> structs = (List<RWStructDefn>) ois.readObject();
					for (RWStructDefn sd : structs) {
						int idx = sd.name().lastIndexOf(".");
						scope.define(sd.name().substring(idx+1), sd.name(), sd);
					}
					@SuppressWarnings("unchecked")
					List<UnionTypeDefn> types = (List<UnionTypeDefn>) ois.readObject();
					for (UnionTypeDefn td : types) {
						int idx = td.name().lastIndexOf(".");
						scope.define(td.name().substring(idx+1), td.name(), td);
					}
					@SuppressWarnings("unchecked")
					List<ContractDecl> contracts = (List<ContractDecl>)ois.readObject();
					for (ContractDecl c : contracts) {
						int idx = c.name().lastIndexOf(".");
						scope.define(c.name().substring(idx+1), c.name(), c);
					}
					@SuppressWarnings("unchecked")
					List<CardTypeInfo> cards = (List<CardTypeInfo>) ois.readObject();
					for (CardTypeInfo c : cards) {
						int idx = c.name.lastIndexOf(".");
						scope.define(c.name.substring(idx+1), c.name, c);
					}
					@SuppressWarnings("unchecked")
					Map<String, Type> knowledge = (Map<String, Type>) ois.readObject();
					for (Entry<String, Type> t : knowledge.entrySet()) {
						int idx = t.getKey().lastIndexOf(".");
						scope.define(t.getKey().substring(idx+1), t.getKey(), t.getValue());
					}
					*/
					return ret;
				} catch (Exception ex) {
					errors.message((Block)null, ex.toString());
				} finally {
				}
			}
		}
		return null;
	}

	public void searchIn(File file) {
		if (file == null || !file.isDirectory())
			throw new ArgumentException("Cannot search for FLIMs in " + file + " as it is not a valid directory");
		dirs.add(file);
	}
}
