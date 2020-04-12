package org.flasck.flas.parser.ut;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;

public class TestStepNamer implements UnitDataNamer {
	protected final NameOfThing name;

	public TestStepNamer(NameOfThing name) {
		this.name = name;
	}

	@Override
	public VarName introductionName(InputPosition location, String text) {
		return new VarName(location, name, text);
	}

	@Override
	public FunctionName dataName(InputPosition location, String text) {
		return FunctionName.function(location, name, text);
	}
}
