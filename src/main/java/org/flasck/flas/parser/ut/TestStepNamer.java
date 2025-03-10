package org.flasck.flas.parser.ut;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.commonBase.names.VarName;

public class TestStepNamer implements UnitDataNamer {
	protected final NameOfThing name;

	public TestStepNamer(NameOfThing name) {
		this.name = name;
	}

	@Override
	public VarName introductionName(InputPosition location, String text, boolean pkgLevel) {
		NameOfThing scope = pkgLevel ? name.packageName() : name;
		return new VarName(location, scope, text);
	}

	@Override
	public FunctionName dataName(InputPosition location, String text) {
		return FunctionName.function(location, name, text);
	}

	@Override
	public TemplateName template(InputPosition location, String text) {
		return new TemplateName(location, name, text);
	}

	@Override
	public VarName nameVar(InputPosition loc, String text) {
		return new VarName(loc, name, text);
	}

	@Override
	public SolidName namePoly(InputPosition pos, String tok) {
		return new SolidName(name, tok);
	}
}
