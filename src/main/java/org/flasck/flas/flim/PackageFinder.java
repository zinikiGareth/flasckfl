package org.flasck.flas.flim;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.ArgumentException;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.typechecker.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;
import org.zinutils.xml.XML;
import org.zinutils.xml.XMLElement;

public class PackageFinder {
	private final static Logger logger = LoggerFactory.getLogger("Compiler");
	private final Rewriter rw;
	private final List<File> dirs;
	private final Map<String, ImportPackage> imported = new HashMap<String, ImportPackage>();
	
	public PackageFinder(Rewriter rw, List<File> pkgdirs, ImportPackage rootPkg) {
		this.rw = rw;
		dirs = pkgdirs;
		imported.put("", rootPkg);
	}

	public ImportPackage loadFlim(ErrorResult errors, String pkgName) {
		if (imported.containsKey(pkgName))
			return imported.get(pkgName);
		for (File d : dirs) {
			File flim = new File(d, pkgName + ".flim");
			if (flim.canRead()) {
				// Load definitions into it
				try {
					logger.error("Loading definitions for " + pkgName + " from " + flim);
					XML xml = XML.fromFile(flim);
					ImportPackage ret = new ImportPackage(pkgName);
					XMLElement top = xml.top();
					if (!top.hasTag("FLIM"))
						throw new UtilException("Cannot load FLIM file " + flim + " because it does not have the right tag");
					for (XMLElement xe : top.elementChildren()) {
						if (xe.hasTag("Struct")) {
							List<Type> polys = new ArrayList<>();
							RWStructDefn sd = new RWStructDefn(location(xe), xe.required("name"), false, polys);
							xe.attributesDone();
							ret.define(sd.name(), sd);
							for (XMLElement fe : xe.elementChildren()) {
								RWStructField sf = new RWStructField(location(fe), fe.requiredBoolean("accessor"), getUniqueNestedType(fe), fe.required("name"));
								fe.attributesDone();
								sd.fields.add(sf);
							}
						} else if (xe.hasTag("Contract")) {
							RWContractDecl cd = new RWContractDecl(null, location(xe), xe.required("name"));
							xe.attributesDone();
							ret.define(cd.name(), cd);
							for (XMLElement cme : xe.elementChildren()) {
								List<Object> args = new ArrayList<Object>();
								for (XMLElement pe : cme.elementChildren()) {
									if (pe.hasTag("Typed")) {
										InputPosition vlocation = null; // TODO: this was an oversight in generation; so back and do it ...
										RWTypedPattern tp = new RWTypedPattern(location(pe), getUniqueNestedType(pe), vlocation, pe.required("var"));
										args.add(tp);
									} else
										System.out.println("Handle pattern " + pe);
								}
								System.out.println("Figure the type");
								Type type = null; // do what now?
								RWContractMethodDecl cmd = new RWContractMethodDecl(location(cme), cme.requiredBoolean("required"), cme.required("dir"), cme.required("name"), args, type);
								cme.attributesDone();
								cd.methods.add(cmd);
							}
						} else
							System.out.println("Have a " + xe.tag() + (xe.hasAttribute("name")?" called "  +xe.get("name") : ""));
					}
					imported.put(pkgName, ret);
					return ret;
				} catch (Exception ex) {
					ex.printStackTrace();
					errors.message((Block)null, ex.toString());
				} finally {
				}
			}
		}
		return null;
	}

	protected Type getUniqueNestedType(XMLElement fe) {
		Type t = null;
		for (XMLElement te : fe.elementChildren()) {
			if (t != null)
				throw new UtilException("Multiple type declarations");
			// Need to consider function first
			String name = te.required("name");
			if (te.hasTag("Builtin")) {
				t = rw.builtins.get(name);
				te.assertNoSubContents();
			} else if (te.hasTag("Contract")) {
				t = rw.contracts.get(name);
				te.assertNoSubContents();
			} else if (te.hasTag("Instance")) {
				
			} else
				throw new UtilException("What is " + te.tag() + " " + name + "?");
			if (t == null)
				throw new UtilException("Failed to find " + te.tag() + " " + name);
		}
		return t;
	}

	private InputPosition location(XMLElement xe) {
		return new InputPosition(xe.required("file"), xe.requiredInt("line"), xe.requiredInt("off"), null);
	}

	private Type type(XMLElement te) {
		System.out.println("Parse type " + te);
		// TODO Auto-generated method stub
		return null;
	}

	public void searchIn(File file) {
		if (file == null || !file.isDirectory())
			throw new ArgumentException("Cannot search for FLIMs in " + file + " as it is not a valid directory");
		dirs.add(file);
	}
}
