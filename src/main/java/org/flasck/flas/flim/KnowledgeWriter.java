package org.flasck.flas.flim;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.rewriter.RepoVisitor;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.HandlerGrouping;
import org.flasck.flas.rewrittenForm.RWConstructorMatch;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.types.FunctionType;
import org.flasck.flas.types.InstanceType;
import org.flasck.flas.types.PolyVar;
import org.flasck.flas.types.TupleType;
import org.flasck.flas.types.Type;
import org.flasck.flas.types.TypeOfSomethingElse;
import org.flasck.flas.types.TypeWithName;
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
		xe.setAttribute("name", ((SolidName)td.getTypeName()).baseName());
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
		xe.setAttribute("name", ((SolidName)cd.getTypeName()).baseName());
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
		for (ContractGrouping x : cg.contracts) {
			XMLElement xh = xe.addElement("Implements");
			writeSolidName(xh, x.contractName);
		}
		if (copyToScreen) {
			System.out.println("  card " + cg.struct.name());
			for (ContractGrouping x : cg.contracts) {
				System.out.println("    contract " + x.contractName.uniqueName());
			}
			for (HandlerGrouping x : cg.handlers) {
				System.out.println("    handler " + x.type);
			}
		}
	}

	@Override
	public void visitContractImpl(RWContractImplements ci) {
		// nothing to do here
	}

	@Override
	public void visitServiceImpl(RWContractService ci) {
		// nothing to do here
	}

	@Override
	public void visitHandlerImpl(RWHandlerImplements hi) {
		// nothing to do here
	}

	// I believe this is *just* functions, but that includes functions of 0 args, which don't *look* like functions to the naked eye ...
	// I would like this "name" to actually be a function name, but hack for now ...
	public void add(String name, FunctionType type) {
		int idx = name.lastIndexOf(".");
		name = name.substring(idx+1);
		XMLElement xe = top.addElement("Function");
		if (!(type instanceof FunctionType))
			throw new UtilException("Not a function");
		// TODO: I think we should get the location of the first case ...
		writeLocation(xe, type);
		xe.setAttribute("name", name);
		writeTypeUsage(xe, type);
		if (copyToScreen) {
			Type ty = type;
			if (type.arity() == 0)
				ty = type.arg(0);
			System.out.println("  function " + name + " :: " + ty + " => " + type.location());
		}
	}

	// This is responsible for writing a type when it is used (as opposed to its
	// definition, such as Struct or Union).
	// It gives fully-qualified names, but only deals with "references" to types
	private void writeTypeUsage(XMLElement xe, Type type) {
		if (type instanceof PolyVar) {
			XMLElement ty = xe.addElement("Poly");
			ty.setAttribute("name", ((TypeWithName)type).name());
		} else if (type instanceof InstanceType) {
			InstanceType it = (InstanceType) type;
			XMLElement ty = xe.addElement("Instance");
			requirePackageFor(it.name());
			writeLocation(ty, it);
			writeTypeUsage(ty, it.innerType());
			for (Type t : it.polys())
				writeTypeUsage(ty, t);
		} else if (type instanceof FunctionType) {
			FunctionType ft = (FunctionType) type;
			XMLElement ty = xe.addElement("Function");
			for (int i=0;i<ft.arity();i++) {
				XMLElement ae = ty.addElement("Arg");
				writeTypeUsage(ae, ft.arg(i));
			}
			XMLElement re = ty.addElement("Return");
			writeTypeUsage(re, ft.arg(ft.arity()));
		} else if (type instanceof TupleType) {
			TupleType tt = (TupleType) type;
			XMLElement ty = xe.addElement("Tuple");
			for (int i=0;i<tt.width();i++) {
				XMLElement ae = ty.addElement("Arg");
				writeTypeUsage(ae, tt.arg(i));
			}
		} else if (type instanceof TypeOfSomethingElse) {
			XMLElement ty = xe.addElement("DEAL_WITH_THIS");
			ty.setAttribute("whatAmI", type.getClass().getName());
			ty.setAttribute("name", ((TypeWithName)type).name());
		} else if (type instanceof TypeWithName) {
			NameOfThing solidName = ((TypeWithName)type).getTypeName();
			writeSolidName(xe, solidName);
		} else
			throw new UtilException("Cannot write " + type.getClass());
	}

	protected void writeSolidName(XMLElement xe, NameOfThing solidName) {
		String usePkg = solidName.writeToXML(xe);
		if (usePkg != null)
			requirePackageFor(usePkg);
	}

	private void requirePackageFor(String name) {
		int idx = name.lastIndexOf(".");
		if (idx != -1)
			imports.add(name.substring(0, idx));
	}

	private void writePolys(XMLElement xe, List<PolyVar> list) {
		for (PolyVar t : list) {
			XMLElement pe = xe.addElement("Poly");
			pe.setAttribute("called", t.getTypeName().uniqueName());
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
