package org.flasck.flas.parser.ut;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.parser.VarNamer;

public interface UnitDataNamer extends IntroduceNamer, VarNamer {
	FunctionName dataName(InputPosition location, String text);
	TemplateName template(InputPosition location, String text);
}
