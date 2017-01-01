package org.flasck.flas.flim;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.PolyName;
import org.flasck.flas.commonBase.names.StructName;
import org.flasck.flas.rewriter.RepoVisitor;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.HandlerGrouping;
import org.flasck.flas.rewrittenForm.RWConstructorMatch;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.types.Type;
import org.flasck.flas.types.Type.WhatAmI;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.Justification;
import org.zinutils.xml.XML;
import org.zinutils.xml.XMLElement;

public class KnowledgeWriter implements RepoVisitor {
	private final String pkg; 
	private final XML xml;
	private final XMLElement top;
	private final boolean copyToScreen;
	private final Set<String> imports = new TreeSet<String>();

	public KnowledgeWriter(String pkg, boolean copyToScreen) {
		this.pkg = pkg;
		xml = XML.create("1.0", "FLIM");
		top = xml.top();
		top.setAttribute("package", pkg);
		this.copyToScreen = copyToScreen;
	}

	@Override
	public void visitStructDefn(RWStructDefn sd) {
		XMLElement xe = top.addElement("Struct");
		writeLocation(xe, sd);
		xe.setAttribute("name", sd.structName().baseName());
		writePolys(xe, sd.polys());
		writeStructFields(xe, sd.fields);
		if (copyToScreen)
			System.out.println("  struct " + sd.asString());
	}

	protected void writeStructFields(XMLElement xe, List<RWStructField> fs) {
		for (RWStructField f : fs) {
			XMLElement sf = xe.addElement("Field");
			writeLocation(sf, f);
			sf.setAttribute("name", f.name);
			sf.setAttribute("accessor", Boolean.toString(f.accessor));
			writeTypeUsage(sf, f.type);
			// if (f.init != null)
			// writeValue(sf, f.init);
		}
	}

	// Note: this case is currently untested because we don't have the syntax to introduce these ...
	public void add(RWUnionTypeDefn td) {
		XMLElement xe = top.addElement("Union");
		writeLocation(xe, td);
		xe.setAttribute("name", ((StructName)td.getTypeName()).baseName());
		writePolys(xe, td.polys());
		for (Type f : td.cases) {
			writeTypeUsage(xe, f);
		}
		if (copyToScreen)
			System.out.println("  type " + td.name());
	}


	public void visitContractDecl(RWContractDecl cd) {
		XMLElement xe = top.addElement("Contract");
		writeLocation(xe, cd);
		xe.setAttribute("name", ((StructName)cd.getTypeName()).baseName());
		for (RWContractMethodDecl meth : cd.methods) {
			XMLElement xm = xe.addElement("Method");
			writeLocation(xm, meth);
			xm.setAttribute("required", Boolean.toString(meth.required));
			xm.setAttribute("dir", meth.dir);
			xm.setAttribute("name", meth.name);
			for (Object arg : meth.args) {
				addPatternArg(xm, arg);
			}
		}
		if (copyToScreen) {
			System.out.println("  contract " + cd.name());
			for (RWContractMethodDecl m : cd.methods) {
				System.out.print(Justification.LEFT.format("", 4));
				System.out.print(Justification.PADRIGHT.format(m.dir, 5));
				System.out.print(Justification.PADRIGHT.format(m.name, 12));
				System.out.print(" ::");
				String sep = " ";
				for (Object o : m.args) {
					System.out.print(sep + ((AsString)o).asString());
					sep = " -> ";
				}
				System.out.println();
			}
		}
	}

	protected void addPatternArg(XMLElement xm, Object arg) {
		if (arg instanceof RWTypedPattern) {
			RWTypedPattern tp = (RWTypedPattern) arg;
			XMLElement ae = xm.addElement("Typed");
			writeLocation(ae, tp);
			ae.setAttribute("var", getBaseName(tp.var)); // tp.var should be a VarName
			writeLocation(ae, tp.varLocation, "v");
			writeTypeUsage(ae, tp.type);
		} else if (arg instanceof RWConstructorMatch) {
			RWConstructorMatch cm = (RWConstructorMatch) arg;
			XMLElement cme = xm.addElement("Ctor");
			writeLocation(cme, cm);
			cme.setAttribute("ctor", cm.ref.uniqueName());
			for (Object obj : cm.args)
				addPatternArg(cme, obj);
		} else
			throw new UtilException("Cannot handle " + arg.getClass());
	}

	private String getBaseName(String var) {
		return var.substring(var.lastIndexOf(".")+1);
	}

	public void visitCardGrouping(CardGrouping cg) {
		if (cg.struct == null)
			return;
		XMLElement xe = top.addElement("Card");
		// TODO: I think we should get the location of the first case ...
		writeLocation(xe, cg.struct);
		xe.setAttribute("name", cg.struct.structName().baseName());
		TreeSet<String> impls = new TreeSet<>();
		for (ContractGrouping x : cg.contracts) {
			impls.add(x.type);
		}
		for (String it : impls) {
			XMLElement xh = xe.addElement("Implements");
			xh.setAttribute("contract", it);
			requirePackageFor(it);
		}
		if (copyToScreen) {
			System.out.println("  card " + cg.struct.name());
			for (ContractGrouping x : cg.contracts) {
				System.out.println("    contract " + x.type);
			}
			for (HandlerGrouping x : cg.handlers) {
				System.out.println("    handler " + x.type);
			}
		}
	}

	// I believe this is *just* functions, but that includes functions of 0 args, which don't *look* like functions to the naked eye ...
	// I would like this "name" to actually be a function name, but hack for now ...
	public void add(String name, Type type) {
		int idx = name.lastIndexOf(".");
		name = name.substring(idx+1);
		XMLElement xe = top.addElement("Function");
		if (type.iam != WhatAmI.FUNCTION)
			throw new UtilException("Not a function");
		// TODO: I think we should get the location of the first case ...
		writeLocation(xe, type);
		xe.setAttribute("name", name);
		writeTypeUsage(xe, type);
		if (copyToScreen) {
			Type ty = type;
			if (ty.arity() == 0)
				ty = type.arg(0);
			System.out.println("  function " + name + " :: " + ty + " => " + type.location());
		}
	}

	// This is responsible for writing a type when it is used (as opposed to its
	// definition, such as Struct or Union).
	// It gives fully-qualified names, but only deals with "references" to types
	private void writeTypeUsage(XMLElement xe, Type type) {
		switch (type.iam) {
		case PRIMITIVE:
		{
			XMLElement ty = xe.addElement("Builtin");
			ty.setAttribute("name", type.name());
			break;
		}
		case CONTRACT:
		{
			XMLElement ty = xe.addElement("Contract");
			ty.setAttribute("name", type.name());
			requirePackageFor(type.name());
			break;
		}
		case CONTRACTIMPL:
		{
			XMLElement ty = xe.addElement("Implements");
			ty.setAttribute("name", type.name());
			writeLocation(ty, ((RWContractImplements)type).varLocation, "v");
			requirePackageFor(type.name());
			break;
		}
		case CONTRACTSERVICE:
		{
			XMLElement ty = xe.addElement("Service");
			ty.setAttribute("name", type.name());
			requirePackageFor(type.name());
			break;
		}
		case HANDLERIMPLEMENTS:
		{
			XMLElement ty = xe.addElement("Handler");
			ty.setAttribute("name", type.name());
			requirePackageFor(type.name());
			break;
		}
		case STRUCT:
		{
			XMLElement ty = xe.addElement("Struct");
			ty.setAttribute("name", type.name());
			requirePackageFor(type.name());
			break;
		}
		case UNION:
		{
			XMLElement ty = xe.addElement("Union");
			ty.setAttribute("name", type.name());
			requirePackageFor(type.name());
			break;
		}
		case OBJECT:
		{
			XMLElement ty = xe.addElement("Object");
			ty.setAttribute("name", type.name());
			requirePackageFor(type.name());
			break;
		}
		case POLYVAR: {
			XMLElement ty = xe.addElement("Poly");
			ty.setAttribute("name", type.name());
			break;
		}
		case INSTANCE: {
			XMLElement ty = xe.addElement("Instance");
			requirePackageFor(type.name());
			writeLocation(ty, type);
			writeTypeUsage(ty, type.innerType());
			for (Type t : type.polys())
				writeTypeUsage(ty, t);
			break;
		}
		case FUNCTION: {
			XMLElement ty = xe.addElement("Function");
			for (int i=0;i<type.arity();i++) {
				XMLElement ae = ty.addElement("Arg");
				writeTypeUsage(ae, type.arg(i));
			}
			XMLElement re = ty.addElement("Return");
			writeTypeUsage(re, type.arg(type.arity()));
			break;
		}
		case TUPLE: {
			XMLElement ty = xe.addElement("Tuple");
			for (int i=0;i<type.width();i++) {
				XMLElement ae = ty.addElement("Arg");
				writeTypeUsage(ae, type.arg(i));
			}
			break;
		}
		case SOMETHINGELSE:
		{
			XMLElement ty = xe.addElement("DEAL_WITH_THIS");
			ty.setAttribute("whatAmI", type.iam.name());
			ty.setAttribute("name", type.name());
		}
		}
	}

	private String getBaseName(NameOfThing typeName) {
		if (typeName instanceof StructName)
			return ((StructName)typeName).baseName();
		else if (typeName instanceof PackageName)
			return ((PackageName)typeName).simpleName();
		else if (typeName instanceof PolyName)
			return ((PolyName)typeName).simpleName();
		else
			throw new UtilException("Cannot find base name of " + typeName);
	}

	private void requirePackageFor(String name) {
		int idx = name.lastIndexOf(".");
		if (idx != -1)
			imports.add(name.substring(0, idx));
	}

	private void writePolys(XMLElement xe, List<Type> list) {
		for (Type t : list) {
			XMLElement pe = xe.addElement("Poly");
			pe.setAttribute("called", t.name());
		}
	}

	private void writeLocation(XMLElement xe, Locatable locItem) {
		writeLocation(xe, locItem.location(), "");
	}

	private void writeLocation(XMLElement xe, InputPosition loc, String prefix) {
		if (loc == null)
			return;
		xe.setAttribute(prefix+"file", loc.file);
		xe.setAttribute(prefix+"line", Integer.toString(loc.lineNo));
		xe.setAttribute(prefix+"off", Integer.toString(loc.off));
	}

	public XML commit() {
		imports.remove(pkg); // obviously we don't want to depend on ourselves ...
		int k = 0;
		for (String s : imports) {
			xml.addElementAt(k++, "Import").setAttribute("package", s);
		}
		return xml;
	}
}
