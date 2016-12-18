package org.flasck.flas.testrunner;

import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parser.Expression;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.zinutils.exceptions.NotImplementedException;

public class UnitTestStepConvertor {
	private final TestScriptBuilder builder;
	private final Expression expr = new Expression();

	public UnitTestStepConvertor(TestScriptBuilder builder) {
		this.builder = builder;
	}

	public void handle(Tokenizable line, List<Block> nested) {
		KeywordToken kw = KeywordToken.from(line);
		if (kw == null)
			return;
		if (kw.text.equals("assert"))
			handleAssert(line, nested);
		else if (kw.text.equals("create"))
			handleCreate(kw, line, nested);
		else if (kw.text.equals("matchElement"))
			handleMatch(kw, WhatToMatch.ELEMENT, line, nested);
		else if (kw.text.equals("matchContents"))
			handleMatch(kw, WhatToMatch.CONTENTS, line, nested);
		else if (kw.text.equals("matchCount"))
			handleMatch(kw, WhatToMatch.COUNT, line, nested);
		else if (kw.text.equals("matchClass"))
			handleMatch(kw, WhatToMatch.CLASS, line, nested);
		else
			builder.error(kw.location, "cannot handle input line: " + kw.text);
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
				builder.error(pos, "needed exactly one nested line for assert");
				return;
			}
			Block valueBlock = nested.get(0);
			if (!valueBlock.nested.isEmpty()) {
				builder.error(pos, "value block cannot have nested lines");
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

	private void handleCreate(KeywordToken kw, Tokenizable line, List<Block> nested) {
		if (!nested.isEmpty()) {
			builder.error(kw.location, "create may not have nested instructions");
			return;
		}
		ValidIdentifierToken var = ValidIdentifierToken.from(line);
		if (var == null) {
			builder.error(line.realinfo(), "create needs a var as first argument: '" + line +"'");
			return;
		}
		TypeNameToken card = TypeNameToken.from(line);
		if (card == null) {
			builder.error(line.realinfo(), "create needs a type as first argument: '" + line +"'");
			return;
		}
		line.skipWS();
		if (line.hasMore()) {
			builder.error(line.realinfo(), "extra characters at end of command: '" + line.remainder().trim() +"'");
			return;
		}
		builder.addCreate(kw.location, var.text, card.text);
	}

	private void handleMatch(KeywordToken kw, WhatToMatch what, Tokenizable line, List<Block> nested) {
		line.skipWS();
		if (!line.hasMore()) {
			builder.error(line.realinfo(), "no pattern in match");
			return;
		}
		String selectors = line.remainder().trim();
		String expect = null;
		for (Block b : nested) {
			if (!b.isComment()) {
				if (expect != null) {
					builder.error(kw.location, "matcher may not have multiple nested blocks");
					return;
				}
				expect = b.line.text().toString().trim();
			}
		}
		if (expect == null) {
			if (what == WhatToMatch.CLASS) {
				// it's OK for this to be empty, pass the "" string
				expect = "";
			} else {
				builder.error(kw.location, "match must have exactly one nested block");
				return;
			}
		}
		builder.addMatch(kw.location, what, selectors, expect);
	}
}
