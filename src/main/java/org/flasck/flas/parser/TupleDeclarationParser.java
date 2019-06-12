package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.tokenizers.PattToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public class TupleDeclarationParser implements TryParsing {

	public TupleDeclarationParser(Object state) {
	}

	@Override
	public Object tryParsing(Tokenizable line) {
		return null;
	}

}
