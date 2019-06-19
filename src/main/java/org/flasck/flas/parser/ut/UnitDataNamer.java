package org.flasck.flas.parser.ut;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;

public interface UnitDataNamer {
	FunctionName dataName(InputPosition location, String text);
}
