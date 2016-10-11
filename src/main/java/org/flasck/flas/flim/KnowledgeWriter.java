package org.flasck.flas.flim;

import java.io.File;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.parsedForm.AsString;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.typechecker.CardTypeInfo;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeHolder;
import org.flasck.flas.typechecker.Type.WhatAmI;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.Justification;
import org.zinutils.xml.XML;
import org.zinutils.xml.XMLElement;

public class KnowledgeWriter {
	private final File exportTo;
	private final XML xml;
	private final XMLElement top;
	private final boolean copyToScreen;

	public KnowledgeWriter(File exportTo, String pkg, boolean copyToScreen) {
		this.exportTo = exportTo;
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
		for (RWStructField f : sd.fields) {
			XMLElement sf = xe.addElement("Field");
			writeLocation(sf, f);
			sf.setAttribute("name", f.name);
			sf.setAttribute("accessor", Boolean.toString(f.accessor));
			writeTypeUsage(sf, f.type);
			// if (f.init != null)
			// writeValue(sf, f.init);
		}
		if (copyToScreen)
			System.out.println("  struct " + sd.asString());
	}

	public void add(RWUnionTypeDefn td) {
		if (copyToScreen)
			System.out.println("  type " + td.name());
	}

	public void add(RWContractDecl cd) {
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

	public void add(CardTypeInfo cti) {
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

	public void add(String name, Type type) {
		if (copyToScreen)
			System.out.println("  function " + name + " :: " + type + " => " + type.location());
	}

	// This is responsible for writing a type when it is used (as opposed to its
	// definition, such as Struct or Union).
	// It gives fully-qualified names, but only deals with "references" to types
	private void writeTypeUsage(XMLElement xe, Type type) {
		switch (type.iam) {
		case BUILTIN: {
			XMLElement ty = xe.addElement("Type");
			ty.setAttribute("name", type.name());
			break;
		}
		case POLYVAR: {
			XMLElement ty = xe.addElement("Poly");
			ty.setAttribute("name", type.name());
			break;
		}
		case INSTANCE: {
			XMLElement ty = xe.addElement("Instance");
			ty.setAttribute("name", type.name());
			for (Type t : type.polys())
				writeTypeUsage(ty, t);
			break;
		}
		default:
			XMLElement ty = xe.addElement("DEAL_WITH_THIS");
			ty.setAttribute("whatAmI", type.iam.name());
			ty.setAttribute("name", type.name());
		}
	}

	private void writePolys(XMLElement xe, List<Type> list) {
		for (Type t : list) {
			XMLElement pe = xe.addElement("Poly");
			pe.setAttribute("called", t.name());
		}
	}

	private void writeLocation(XMLElement xe, Locatable locItem) {
		InputPosition loc = locItem.location();
		xe.setAttribute("file", loc.file);
		xe.setAttribute("line", Integer.toString(loc.lineNo));
		xe.setAttribute("off", Integer.toString(loc.off));
	}

	public void commit() {
		xml.write(exportTo);
		FileUtils.cat(exportTo);
	}
}
