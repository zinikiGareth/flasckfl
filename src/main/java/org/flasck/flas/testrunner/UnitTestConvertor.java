package org.flasck.flas.testrunner;

import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parser.Expression;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;

public class UnitTestConvertor {
	private final TestScriptBuilder builder;
	private final String pkg;

	public UnitTestConvertor(TestScriptBuilder builder, String pkg) {
		this.builder = builder;
		this.pkg = pkg;
	}

	public void convert(List<String> list) {
		List<Block> block = Blocker.block(list);
		for (Block b : block) {
			Tokenizable line = new Tokenizable(b);
			convertSection(line, b.nested);
		}
	}

	private void convertSection(Tokenizable line, List<Block> nested) {
		if (!line.hasMore())
			return; // nothing to do
		KeywordToken kw = KeywordToken.from(line);
		if (kw.text.equals("test")) {
			String message = line.remainder().trim();
			for (Block b : nested)
				convertStep(new Tokenizable(b.line), b.nested);
			builder.addTestCase(message);
		} else
			builder.error("cannot handle input line: " + kw.text);
	}

	private void convertStep(Tokenizable line, List<Block> nested) {
		if (!line.hasMore())
			return; // nothing to do
		KeywordToken kw = KeywordToken.from(line);
		if (kw.text.equals("assert"))
			convertValue(line, nested);
		else
			builder.error("cannot handle input line: " + kw.text);
	}

	private void convertValue(Tokenizable line, List<Block> nested) {
		Expression expr = new Expression();
		Object ret = expr.tryParsing(line);
		if (ret instanceof ErrorResult)
			throw new NotImplementedException();
		else if (ret == null)
			throw new NotImplementedException();
		else {
			System.out.println(ret.getClass());
			builder.add(new AssertTestStep(ret));
		}
	}
}
