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
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.typechecker.CardTypeInfo;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeHolder;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;
import org.zinutils.xml.XML;
import org.zinutils.xml.XMLElement;

public class PackageFinder {
	private class Pass2 {
		Object parent;
		List<XMLElement> children;
		
		public Pass2(Object parent, XMLElement container) {
			this.parent = parent;
			this.children = container.elementChildren();
		}
	}
	
	private final static Logger logger = LoggerFactory.getLogger("Compiler");
	private final Rewriter rw;
	private final List<File> dirs;
	private final Map<String, ImportPackage> imported = new HashMap<String, ImportPackage>();
	
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
					ImportPackage pkg = new ImportPackage(pkgName);
					imported.put(pkgName, pkg);
					XMLElement top = xml.top();
					if (!top.hasTag("FLIM"))
						throw new UtilException("Cannot load FLIM file " + flim + " because it does not have the right tag");
					
					// Pass0 : read and install all imported packages
					for (XMLElement xe : top.elementChildren("Import")) {
						String ipn = xe.required("package");
						xe.attributesDone();
						xe.assertNoSubContents();
						
						ImportPackage exists = imported.get(ipn);
						if (exists == null)
							loadFlim(errors, ipn);
						else if (!exists.isLoaded()) {
							errors.message((Block)null, "cannot import package " + pkgName + " because it has a circular dependency on " + ipn);
							return;
						}
						// else we have already loaded it, so no probs
					}
					
					// Pass1 : read and install all the top-level definitions, and squirrel away the nested references
					List<Pass2> todos = new ArrayList<Pass2>();
					for (XMLElement xe : top.elementChildren()) {
						if (xe.hasTag("Import"))
							; // handled in pass 0
						else if (xe.hasTag("Struct")) {
							List<Type> polys = new ArrayList<>();
							RWStructDefn sd = new RWStructDefn(location(xe), xe.required("name"), false, polys);
							xe.attributesDone();
							pkg.define(sd.name(), sd);
							todos.add(new Pass2(sd, xe));
						} else if (xe.hasTag("Contract")) {
							RWContractDecl cd = new RWContractDecl(null, location(xe), xe.required("name"));
							xe.attributesDone();
							pkg.define(cd.name(), cd);
							todos.add(new Pass2(cd, xe));
						} else if (xe.hasTag("Function")) {
							// we don't have anything to create right now ...
							todos.add(new Pass2(xe, xe));
						} else if (xe.hasTag("Card")) {
							CardTypeInfo cti = new CardTypeInfo(location(xe), xe.required("name"));
							xe.attributesDone();
							pkg.define(cti.name, cti);
							todos.add(new Pass2(cti, xe));
						} else
							System.out.println("Need to import a " + xe.tag() + (xe.hasAttribute("name")?" called "  +xe.get("name") : ""));
					}
					
					// after pass1, make these things available, if incomplete ...
					rw.importPackage1(pkg);
					
					for (Pass2 p : todos) {
						if (p.parent instanceof RWStructDefn) {
							RWStructDefn sd = (RWStructDefn) p.parent;
							for (XMLElement fe : p.children) {
								RWStructField sf = new RWStructField(location(fe), fe.requiredBoolean("accessor"), getUniqueNestedType(fe), fe.required("name"));
								fe.attributesDone();
								sd.fields.add(sf);
							}
						} else if (p.parent instanceof RWContractDecl) {
							RWContractDecl cd = (RWContractDecl) p.parent;
							for (XMLElement cme : p.children) {
								List<Object> args = new ArrayList<Object>();
								List<Type> types = new ArrayList<Type>();
								for (XMLElement pe : cme.elementChildren()) {
									if (pe.hasTag("Typed")) {
										RWTypedPattern tp = new RWTypedPattern(location(pe), getUniqueNestedType(pe), location(pe, "v"), pe.required("var"));
										args.add(tp);
										types.add(tp.type);
									} else
										System.out.println("Handle pattern " + pe);
								}
								types.add(rw.structs.get("Send"));
								Type type = Type.function(location(cme), types);
								RWContractMethodDecl cmd = new RWContractMethodDecl(location(cme), cme.requiredBoolean("required"), cme.required("dir"), cme.required("name"), args, type);
								cme.attributesDone();
								cd.methods.add(cmd);
							}
						} else if (p.parent instanceof CardTypeInfo) {
							CardTypeInfo cti = (CardTypeInfo) p.parent;
							for (XMLElement ie : p.children) {
								cti.contracts.add(new TypeHolder(ie.required("contract")));
								ie.attributesDone();
								ie.assertNoSubContents();
							}
						} else if (p.parent instanceof XMLElement) {
							// We decided not to create anything in pass1; do all the work here ...
							XMLElement xe = (XMLElement) p.parent;
							if (xe.hasTag("Function")) {
								if (p.children.size() != 1)// which is also xe.elementChildren()
									throw new UtilException("More than one child of function declaration");
								XMLElement te = p.children.get(0);
								if (!te.hasTag("Function"))
									throw new UtilException("Type was not a function type");
								List<Type> args = new ArrayList<Type>();
								for (XMLElement fe : te.elementChildren()) { 
									// Then that has n-1 "Arg" objects
									// and one "Return" object
									args.add(getUniqueNestedType(fe));
								}
								// TODO: should we be (writing and) reading the code type?
								RWFunctionDefinition ret = new RWFunctionDefinition(location(xe), CodeType.FUNCTION, xe.required("name"), args.size()-1, false);
								Type fntype;
								if (args.size() == 1)
									fntype = args.get(0);
								else
									fntype = Type.function(location(xe), args);
								ret.setType(fntype);
								xe.attributesDone();
								pkg.define(ret.name(), ret);
							} else
								throw new UtilException("Unrecognized XML tag " + xe.tag());
						} else
							throw new UtilException("Cannot handle " + p.parent.getClass());
					}
					
					rw.importPackage2(pkg);
				} catch (Exception ex) {
					ex.printStackTrace();
					errors.message((Block)null, ex.toString());
				} finally {
				}
			}
		}
	}

	protected Type getUniqueNestedType(XMLElement fe) {
		Type t = null;
		for (XMLElement te : fe.elementChildren()) {
			if (t != null)
				throw new UtilException("Multiple type declarations");
			t = extractType(te);
		}
		if (t == null)
			throw new UtilException("I believe this must imply that we didn't have any definitions");
		return t;
	}

	protected Type extractType(XMLElement te) {
		Type t = null;
		// Need to consider function first
		String name = "";
		if (te.hasTag("Instance")) {
			List<Type> types = new ArrayList<Type>();
			for (XMLElement ct : te.elementChildren()) {
				types.add(extractType(ct));
			}
			Type base = types.remove(0);
			t = base.instance(location(te), types);
		} else {
			name = te.required("name");
			if (te.hasTag("Builtin")) {
				t = rw.builtins.get(name);
				te.assertNoSubContents();
			} else if (te.hasTag("Struct")) {
				t = rw.structs.get(name);
				te.assertNoSubContents();
			} else if (te.hasTag("Union")) {
				t = rw.types.get(name);
				te.assertNoSubContents();
			} else if (te.hasTag("Contract")) {
				t = rw.contracts.get(name);
				te.assertNoSubContents();
			} else if (te.hasTag("Object")) {
				t = rw.objects.get(name);
				te.assertNoSubContents();
			} else 
				throw new UtilException("What is " + te.tag() + " " + name + "?");
		}
		if (t == null)
			throw new UtilException("Failed to find " + te.tag() + " " + name);
		return t;
	}

	private InputPosition location(XMLElement xe) {
		return new InputPosition(xe.required("file"), xe.requiredInt("line"), xe.requiredInt("off"), null);
	}

	private InputPosition location(XMLElement xe, String prefix) {
		return new InputPosition(xe.required(prefix + "file"), xe.requiredInt(prefix + "line"), xe.requiredInt(prefix + "off"), null);
	}

	public void searchIn(File file) {
		if (file == null || !file.isDirectory())
			throw new ArgumentException("Cannot search for FLIMs in " + file + " as it is not a valid directory");
		dirs.add(file);
	}
}
