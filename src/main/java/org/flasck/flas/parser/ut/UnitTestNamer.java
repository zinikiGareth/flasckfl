package org.flasck.flas.parser.ut;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.UnitTestName;

public interface UnitTestNamer {
	UnitTestName unitTest();
	FunctionName dataName(InputPosition location, String text);
}
