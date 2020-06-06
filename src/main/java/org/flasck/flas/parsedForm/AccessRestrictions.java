package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.errors.ErrorReporter;

public interface AccessRestrictions {

	void check(ErrorReporter errors, InputPosition pos, NameOfThing inContext);

}
