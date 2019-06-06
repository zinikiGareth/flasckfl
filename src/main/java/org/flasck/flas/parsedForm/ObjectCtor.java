package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;

public class ObjectCtor extends ObjectActionHandler {
	public ObjectCtor(InputPosition location, FunctionName name, List<Pattern> args) {
		super(location, name, args);
	}
	
	@Override
	public String toString() {
		return "ctor " + name().uniqueName();
	}
}
