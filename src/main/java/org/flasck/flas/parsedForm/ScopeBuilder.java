package org.flasck.flas.parsedForm;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.TopLevelDefnConsumer;

public class ScopeBuilder implements TopLevelDefnConsumer {

	public ScopeBuilder(ErrorReporter errors) {
	}

	@Override
	public void newStruct(StructDefn sd) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}
