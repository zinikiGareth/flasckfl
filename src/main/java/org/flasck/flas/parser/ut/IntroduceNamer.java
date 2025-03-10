package org.flasck.flas.parser.ut;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.VarName;

public interface IntroduceNamer {
	VarName introductionName(InputPosition location, String text, boolean pkgLevel);
}
