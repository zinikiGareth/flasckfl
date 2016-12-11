package org.flasck.flas.testrunner;

import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class UnitTestConvertor {
	private final TestScriptBuilder builder;
	private final UnitTestStepConvertor steppor;

	public UnitTestConvertor(TestScriptBuilder builder) {
		this.builder = builder;
		this.steppor = new UnitTestStepConvertor(builder);
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
				steppor.handle(new Tokenizable(b.line), b.nested);
			builder.addTestCase(message);
		} else
			builder.error("cannot handle input line: " + kw.text);
	}
}
