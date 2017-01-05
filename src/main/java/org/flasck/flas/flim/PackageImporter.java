package org.flasck.flas.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.types.FunctionType;
import org.flasck.flas.types.PolyVar;
import org.flasck.flas.types.Type;
import org.flasck.flas.types.TypeWithName;
import org.zinutils.exceptions.UtilException;
import org.zinutils.xml.XML;
import org.zinutils.xml.XMLElement;

public class PackageImporter {
	private static class Pass2 {
		Object parent;
		List<XMLElement> children;
		
		public Pass2(Object parent, XMLElement container) {
			this.parent = parent;
			this.children = container.elementChildren();
		}
	}
	
	public static void importInto(PackageFinder finder, ErrorResult errors, Rewriter rw, String pkgName, XML xml) {
		PackageName packageName = new PackageName(pkgName);
		ImportPackage pkg = new ImportPackage(pkgName);
		finder.imported.put(pkgName, pkg);
		XMLElement top = xml.top();
		if (!top.hasTag("FLIM"))
			throw new UtilException("Cannot import package for " + pkgName + " because it does not have the right tag");
		
		// Pass0 : read and install all imported packages
		for (XMLElement xe : top.elementChildren("Import")) {
			String ipn = xe.required("package");
			xe.attributesDone();
			xe.assertNoSubContents();
			
			ImportPackage exists = finder.imported.get(ipn);
			if (exists == null)
				finder.loadFlim(errors, ipn);
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
				List<PolyVar> polys = new ArrayList<>();
				String baseName = xe.required("name");
				RWStructDefn sd = new RWStructDefn(location(xe), new SolidName(packageName, baseName), false, polys);
				xe.attributesDone();
				pkg.define(sd.name(), sd);
				todos.add(new Pass2(sd, xe));
			} else if (xe.hasTag("Contract")) {
				String baseName = xe.required("name");
				RWContractDecl cd = new RWContractDecl(null, location(xe), new SolidName(packageName, baseName), false);
				xe.attributesDone();
				pkg.define(cd.name(), cd);
				todos.add(new Pass2(cd, xe));
			} else if (xe.hasTag("Function")) {
				// we don't have anything to create right now ...
				todos.add(new Pass2(xe, xe));
			} else if (xe.hasTag("Card")) {
				ImportedCard cti = new ImportedCard(location(xe), new CardName(packageName, xe.required("name")));
				xe.attributesDone();
				pkg.define(cti.name.uniqueName(), cti);
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
					RWStructField sf = new RWStructField(location(fe), fe.requiredBoolean("accessor"), getUniqueNestedType(rw, location(fe), fe), fe.required("name"));
					fe.attributesDone();
					sd.fields.add(sf);
				}
			} else if (p.parent instanceof RWContractDecl) {
				RWContractDecl cd = (RWContractDecl) p.parent;
				for (XMLElement cme : p.children) {
					InputPosition loc = location(cme);
					FunctionName fn = FunctionName.functionInCardContext(loc, new CardName(packageName, cd.name()), cme.required("name"));
					List<Object> args = new ArrayList<Object>();
					List<Type> types = new ArrayList<Type>();
					for (XMLElement pe : cme.elementChildren()) {
						if (pe.hasTag("Typed")) {
							RWTypedPattern tp = new RWTypedPattern(location(pe), (TypeWithName) getUniqueNestedType(rw, location(pe, "v"), pe), location(pe, "v"), new VarName(location(pe, "v"), fn, pe.required("var")));
							args.add(tp);
							types.add(tp.type);
						} else
							System.out.println("Handle pattern " + pe);
					}
					types.add((Type) rw.getMe(loc, new SolidName(null, "Send")).defn);
					FunctionType type = new FunctionType(loc, types);
					RWContractMethodDecl cmd = new RWContractMethodDecl(loc, cme.requiredBoolean("required"), cme.required("dir"), fn, args, type);
					cme.attributesDone();
					cd.methods.add(cmd);
				}
			} else if (p.parent instanceof ImportedCard) {
				ImportedCard cti = (ImportedCard) p.parent;
				for (XMLElement ie : p.children) {
					ie.attributesDone();
					cti.contracts.add(new ImportedContract(assembleName(ie.elementChildren())));
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
						args.add(getUniqueNestedType(rw, location(xe), fe));
					}
					// TODO: should we be (writing and) reading the code type?
					// TODO: because we can only import things at the top level, I believe this is correct
					// But it seems to me it would be better to just save/restore the simple name, since we have the package name as what we're importing
					NameOfThing inside = packageName;
					String inCard = xe.optional("incard");
					if (inCard != null) {
						int icx = inCard.lastIndexOf(".");
						inCard = inCard.substring(icx+1);
						inside = new CardName(packageName, inCard);
					}
					RWFunctionDefinition ret = new RWFunctionDefinition(FunctionName.function(location(xe), inside, xe.required("name")), args.size()-1, false);
					Type fntype;
					if (args.size() == 1)
						fntype = args.get(0);
					else
						fntype = new FunctionType(location(xe), args);
					ret.setType(fntype);
					xe.attributesDone();
					pkg.define(ret.name(), ret);
				} else
					throw new UtilException("Unrecognized XML tag " + xe.tag());
			} else
				throw new UtilException("Cannot handle " + p.parent.getClass());
		}
		
		rw.importPackage2(pkg);
		pkg.seal();
	}

	private static InputPosition location(XMLElement xe) {
		return new InputPosition(xe.required("file"), xe.requiredInt("line"), xe.requiredInt("off"), null);
	}

	private static InputPosition location(XMLElement xe, String prefix) {
		return new InputPosition(xe.required(prefix + "file"), xe.requiredInt(prefix + "line"), xe.requiredInt(prefix + "off"), null);
	}

	protected static Type getUniqueNestedType(Rewriter rw, InputPosition loc, XMLElement fe) {
		Type t = null;
		for (XMLElement te : fe.elementChildren()) {
			if (t != null)
				throw new UtilException("Multiple type declarations");
			t = extractType(rw, loc, te);
		}
		if (t == null)
			throw new UtilException("I believe this must imply that we didn't have any definitions");
		return t;
	}

	protected static Type extractType(Rewriter rw, InputPosition loc, XMLElement te) {
		// Need to consider function first
		if (te.hasTag("Instance")) {
			List<Type> types = new ArrayList<Type>();
			for (XMLElement ct : te.elementChildren()) {
				types.add(extractType(rw, loc, ct));
			}
			TypeWithName base = (TypeWithName) types.remove(0);
			return base.instance(location(te), types);
		} else if (te.hasTag("Poly")) {
			String name = te.required("name");
			te.attributesDone();
			te.assertNoSubContents();
			return new PolyVar(loc, name);
		} else {
			NameOfThing name = assembleName(te);
			return (Type)rw.getMe(loc, name).defn;
		}
	}

	private static NameOfThing assembleName(XMLElement te) {
		if (te.hasTag("Package")) {
			te.assertNoSubContents();
			return new PackageName(te.get("name"));
		} else if (te.hasTag("SolidName")) {
			NameOfThing inside = assembleName(te.elementChildren());
			return new SolidName(inside, te.get("name"));
		} else 
			throw new UtilException("What is " + te.tag() + " " + te + "?");
	}

	private static NameOfThing assembleName(List<XMLElement> elementChildren) {
		if (elementChildren.isEmpty())
			return null;
		else if (elementChildren.size() > 1)
			throw new UtilException("Cannot have multiple nodes when assembling a name");
		
		return assembleName(elementChildren.get(0));
	}
}

