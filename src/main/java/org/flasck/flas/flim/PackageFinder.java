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
	private final List<File> dirs;
	private final Map<String, ImportPackage> imported = new HashMap<String, ImportPackage>();
	
	public PackageFinder(List<File> pkgdirs, ImportPackage rootPkg) {
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
					imported.put(pkgName, ret);
					return ret;
				} catch (Exception ex) {
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
			if (te.hasTag("Type")) {
				if (t != null)
					throw new UtilException("Multiple type declarations");
				t = type(te);
			} else
				throw new UtilException("What is " + te.tag() + "?");
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
