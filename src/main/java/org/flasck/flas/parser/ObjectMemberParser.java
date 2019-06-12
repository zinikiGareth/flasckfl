package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.ObjectMember;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;

public class ObjectMemberParser implements TryParsing {

	public ObjectMemberParser(Object state) {
	}
	
	@Override
	public Object tryParsing(Tokenizable line) {
		return null;
	}

}
