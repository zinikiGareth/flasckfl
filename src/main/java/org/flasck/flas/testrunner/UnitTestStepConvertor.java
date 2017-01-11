package org.flasck.flas.testrunner;

import java.util.ArrayList;
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
		else if (kw.text.equals("send"))
			handleSend(kw, line, nested);
		else if (kw.text.equals("matchElement")) {
			String selectors = getMatchSelectors(kw, line);
			String value = getMatchExpectation(kw, nested, false);
			if (selectors != null && value != null)
				builder.addMatch(kw.location, new HTMLMatcher.Element(value), selectors);
		} else if (kw.text.equals("matchContents")) {
			String selectors = getMatchSelectors(kw, line);
			String value = getMatchExpectation(kw, nested, false);
			if (selectors != null && value != null)
				builder.addMatch(kw.location, new HTMLMatcher.Contents(value), selectors);
		} else if (kw.text.equals("matchCount")) {
			String selectors = getMatchSelectors(kw, line);
			String value = getMatchExpectation(kw, nested, false);
			if (selectors != null && value != null)
				builder.addMatch(kw.location, new HTMLMatcher.Count(value), selectors);
		} else if (kw.text.equals("matchClass")) {
			String selectors = getMatchSelectors(kw, line);
			String value = getMatchExpectation(kw, nested, true);
			if (selectors != null)
				builder.addMatch(kw.location, new HTMLMatcher.Class(value), selectors);
		} else
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
		for (Block b : nested)
			if (!b.isComment()) {
				builder.error(kw.location, "create may not have nested instructions");
				return;
			}
		ValidIdentifierToken var = ValidIdentifierToken.from(line);
		if (var == null) {
			builder.error(line.realinfo(), "create needs a var as first argument: '" + line +"'");
			return;
		}
		TypeNameToken card = TypeNameToken.unqualified(line);
		if (card == null) {
			builder.error(line.realinfo(), "create needs a type as second argument: '" + line +"'");
			return;
		}
		line.skipWS();
		if (line.hasMore()) {
			builder.error(line.realinfo(), "extra characters at end of command: '" + line.remainder().trim() +"'");
			return;
		}
		builder.addCreate(kw.location, var.text, card.text);
	}

	private void handleSend(KeywordToken kw, Tokenizable line, List<Block> nested) {
		ValidIdentifierToken var = ValidIdentifierToken.from(line);
		if (var == null) {
			builder.error(line.realinfo(), "send needs a card var as first argument: '" + line +"'");
			return;
		}
		TypeNameToken card = TypeNameToken.qualified(line);
		if (card == null) {
			builder.error(line.realinfo(), "send needs a contract as second argument: '" + line +"'");
			return;
		}
		ValidIdentifierToken method = ValidIdentifierToken.from(line);
		if (method == null) {
			builder.error(line.realinfo(), "send needs a method as third argument: '" + line +"'");
			return;
		}
		ArrayList<Object> args = new ArrayList<>();
		while (line.hasMore()) {
			Object arg = expr.tryParsing(line);
			if (arg instanceof ErrorResult)
				throw new NotImplementedException();
			else if (arg == null)
				throw new NotImplementedException();
			else
				args.add(arg);
		}
		List<Expectation> expecting = new ArrayList<Expectation>(); 
		for (Block b : nested)
			if (!b.isComment()) {
				expecting.add(parseExpectation(new Tokenizable(b.line), b.nested));
			}
		if (builder.hasErrors())
			return;
		builder.addSend(kw.location, var.text, card.text, method.text, args, expecting);
	}

	private Expectation parseExpectation(Tokenizable line, List<Block> nested) {
		for (Block b : nested)
			if (!b.isComment()) {
				builder.error(line.realinfo(), "create may not have nested instructions");
				return null;
			}
		TypeNameToken ctr = TypeNameToken.qualified(line);
		if (ctr == null) {
			builder.error(line.realinfo(), "send needs a contract as second argument: '" + line +"'");
			return null;
		}
		ValidIdentifierToken method = ValidIdentifierToken.from(line);
		if (method == null) {
			builder.error(line.realinfo(), "send needs a method as third argument: '" + line +"'");
			return null;
		}
		ArrayList<Object> args = new ArrayList<>();
		while (line.hasMore()) {
			Object arg = expr.tryParsing(line);
			if (arg instanceof ErrorResult)
				throw new NotImplementedException();
			else if (arg == null)
				throw new NotImplementedException();
			else
				args.add(arg);
		}
		return new Expectation(ctr.text, method.text, args);
	}

	private String getMatchSelectors(KeywordToken kw, Tokenizable line) {
		line.skipWS();
		if (!line.hasMore()) {
			builder.error(line.realinfo(), "no pattern in match");
			return null;
		}
		return line.remainder().trim();
	}
	
	private String getMatchExpectation(KeywordToken kw, List<Block> nested, boolean allowEmpty) {
		String expect = null;
		for (Block b : nested) {
			if (!b.isComment()) {
				if (expect != null) {
					builder.error(kw.location, "matcher may not have multiple nested blocks");
					return null;
				}
				expect = b.line.text().toString().trim();
			}
		}
		if (expect != null)
			return expect;
		else if (allowEmpty) {
			// it's OK for this to be empty, pass the "" string
			return "";
		} else {
			builder.error(kw.location, "match must have exactly one nested block");
			return null;
		}
	}
}
