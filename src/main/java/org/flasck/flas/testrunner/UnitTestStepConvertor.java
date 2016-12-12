package org.flasck.flas.testrunner;

import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parser.Expression;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;

public class UnitTestStepConvertor {
	private final TestScriptBuilder builder;
	private final Expression expr = new Expression();

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
		InputPosition pos = line.realinfo();
		Object ret = expr.tryParsing(line);
		if (ret instanceof ErrorResult)
			throw new NotImplementedException();
		else if (ret == null)
			throw new NotImplementedException();
		else {
			if (nested.size() != 1) {
				builder.error("needed exactly one nested line for assert");
				return;
			}
			Block valueBlock = nested.get(0);
			if (!valueBlock.nested.isEmpty()) {
				builder.error("value block cannot have nested lines");
				return;
			}
			convertValue(pos, ret, new Tokenizable(valueBlock));
		}
	}

	private void convertValue(InputPosition evalPos, Object eval, Tokenizable line) {
		InputPosition pos = line.realinfo();
		Object valueExpr = expr.tryParsing(line);
		if (valueExpr instanceof ErrorResult)
			throw new NotImplementedException();
		else if (valueExpr == null)
			throw new NotImplementedException();
		else {
			builder.addAssert(evalPos, eval, pos, valueExpr);
		}
	}
}
