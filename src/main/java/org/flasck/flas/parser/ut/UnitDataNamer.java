package org.flasck.flas.parser.ut;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;

public interface UnitDataNamer extends IntroduceNamer {
	FunctionName dataName(InputPosition location, String text);
}
