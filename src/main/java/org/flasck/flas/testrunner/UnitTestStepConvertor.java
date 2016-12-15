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
			handleMatchElement(kw, line, nested);
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

	private void handleMatchElement(KeywordToken kw, Tokenizable line, List<Block> nested) {
		ValidIdentifierToken var = ValidIdentifierToken.from(line);
		if (var == null) {
			builder.error(line.realinfo(), "matchElement needs a card var as first argument: '" + line.remainder().trim() +"'");
			return;
		}
		line.skipWS();
		if (!line.hasMore()) {
			builder.error(line.realinfo(), "no pattern in matchElement");
			return;
		}
		String selectors = line.remainder().trim();
		if (nested.size() != 1) {
			builder.error(kw.location, "match must have exactly one nested block");
			return;
		}
		Block block = nested.get(0);
		if (!block.nested.isEmpty()) {
			builder.error(block.line.locationAtText(0), "matching line cannot have nested blocks");
			return;
		}
		builder.addMatchElement(kw.location, var.text, selectors, block.line.text().toString().trim());
	}

}
