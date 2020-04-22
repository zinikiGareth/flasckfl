package org.flasck.flas.parser;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.TypeReference;

public class ObjectNestedNamer extends InnerPackageNamer implements TemplateNamer {
	private int nextId = 0;

	public ObjectNestedNamer(NameOfThing pkgName) {
		super(pkgName);
	}

	@Override
	public TemplateName template(InputPosition location, String text) {
		return new TemplateName(location, pkg, text);
	}

	@Override
	public FunctionName ctor(InputPosition location, String text) {
		return FunctionName.objectCtor(location, (SolidName) pkg, text);
	}

	@Override
	public FunctionName method(InputPosition loc, String text) {
		return FunctionName.objectMethod(loc, (SolidName) pkg, text);
	}

	@Override
	public VarName nameVar(InputPosition loc, String name) {
		return new VarName(loc, pkg, name);
	}

	@Override
	public TypeReference contract(InputPosition location, String text) {
		return new TypeReference(location, text, new ArrayList<>());
	}

	@Override
	public CSName csn(InputPosition location, String type) {
		return new CSName((CardName)pkg, "_" + type + nextId++);
	}
}
