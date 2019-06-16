package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.commonBase.names.VarName;

public class ObjectNestedNamer extends InnerPackageNamer implements TemplateNamer {
	public ObjectNestedNamer(NameOfThing pkgName) {
		super(pkgName);
	}

	@Override
	public TemplateName template(String text) {
		return new TemplateName(pkg, text);
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
}
