package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;

public class ObjectMethod extends ObjectActionHandler {
	public ObjectMethod(InputPosition location, FunctionName name, List<Pattern> args) {
		super(location, name, args);
	}
}
