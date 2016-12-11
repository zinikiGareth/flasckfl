package org.flasck.flas.testrunner;

import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parser.Expression;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;

public class UnitTestStepConvertor {
	private final TestScriptBuilder builder;

	public UnitTestStepConvertor(TestScriptBuilder builder) {
		this.builder = builder;
	}

	public void handle(Tokenizable line, List<Block> nested) {
		if (!line.hasMore())
			return; // nothing to do
		KeywordToken kw = KeywordToken.from(line);
		if (kw.text.equals("assert"))
			handleAssert(line, nested);
		else
			builder.error("cannot handle input line: " + kw.text);
	}

	private void handleAssert(Tokenizable line, List<Block> nested) {
		Expression expr = new Expression();
		Object ret = expr.tryParsing(line);
		if (ret instanceof ErrorResult)
			throw new NotImplementedException();
		else if (ret == null)
			throw new NotImplementedException();
		else {
			System.out.println(ret.getClass());
			builder.add(new AssertTestStep(line.realinfo(), ret, null, null));
		}
	}

}
