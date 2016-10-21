package org.flasck.flas.flim;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.rewrittenForm.RWConstructorMatch;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.typechecker.CardTypeInfo;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeHolder;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.Justification;
import org.zinutils.xml.XML;
import org.zinutils.xml.XMLElement;

public class KnowledgeWriter {
	private final File exportTo;
	private final String pkg; 
	private final XML xml;
	private final XMLElement top;
	private final boolean copyToScreen;
	private final Set<String> imports = new TreeSet<String>();

	public KnowledgeWriter(File exportTo, String pkg, boolean copyToScreen) {
		this.exportTo = exportTo;
		this.pkg = pkg;
		xml = XML.create("1.0", "FLIM");
		top = xml.top();
		top.setAttribute("package", pkg);
		this.copyToScreen = copyToScreen;
	}

	public void add(RWStructDefn sd) {
		XMLElement xe = top.addElement("Struct");
		writeLocation(xe, sd);
		xe.setAttribute("name", sd.uniqueName());
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
		xe.setAttribute("name", td.name());
		writePolys(xe, td.polys());
		for (Type f : td.cases) {
			writeTypeUsage(xe, f);
		}
		if (copyToScreen)
			System.out.println("  type " + td.name());
	}

	public void add(RWContractDecl cd) {
		XMLElement xe = top.addElement("Contract");
		writeLocation(xe, cd);
		xe.setAttribute("name", cd.name());
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
			ae.setAttribute("var", tp.var);
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

	public void add(CardTypeInfo cti) {
		XMLElement xe = top.addElement("Card");
		// TODO: I think we should get the location of the first case ...
		writeLocation(xe, cti);
		xe.setAttribute("name", cti.name);
		/* I don't think this is relevant to outsiders ...
		XMLElement fs = xe.addElement("Fields");
		writeStructFields(fs, cti.struct.fields);
		*/
		for (TypeHolder x : cti.contracts) {
			XMLElement xh = xe.addElement("Implements");
			xh.setAttribute("contract", x.name);
			requirePackageFor(x.name);
		}
		/* I don't think these are relevant to outsiders ...
		for (TypeHolder x : cti.handlers) {
			XMLElement xh = xe.addElement("Handler");
			xh.setAttribute("name", x.name);
		}
		*/
		if (copyToScreen) {
			System.out.println("  card " + cti.name);
			for (TypeHolder x : cti.contracts) {
				System.out.println("    contract " + x.name);
				x.dump(6);
			}
			for (TypeHolder x : cti.handlers) {
				System.out.println("    handler " + x.name);
				x.dump(6);
			}
			cti.dump(4);
		}
	}

	// I believe this is *just* functions, but that includes functions of 0 args, which don't *look* like functions to the naked eye ...
	public void add(String name, Type type) {
		XMLElement xe = top.addElement("Function");
		// TODO: I think we should get the location of the first case ...
		writeLocation(xe, type);
		xe.setAttribute("name", name);
		writeTypeUsage(xe, type);
		if (copyToScreen)
			System.out.println("  function " + name + " :: " + type + " => " + type.location());
	}

	// This is responsible for writing a type when it is used (as opposed to its
	// definition, such as Struct or Union).
	// It gives fully-qualified names, but only deals with "references" to types
	private void writeTypeUsage(XMLElement xe, Type type) {
		switch (type.iam) {
		case BUILTIN:
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

	public void commit() {
		imports.remove(pkg); // obviously we don't want to depend on ourselves ...
		int k = 0;
		for (String s : imports) {
			xml.addElementAt(k++, "Import").setAttribute("package", s);
		}
		xml.write(exportTo);
	}
}
