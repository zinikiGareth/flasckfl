package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.ParsingPhase;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.stories.TDAParserConstructor;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.PattToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDATupleDeclarationParser implements TDAParsing, LocationTracker {
	private final ErrorReporter errors;
	private final FunctionNameProvider functionNamer;
	private final FunctionScopeUnitConsumer consumer;
	private final StateHolder holder;
	private final LocationTracker locTracker;
	private InputPosition lastInner;

	public TDATupleDeclarationParser(ErrorReporter errors, FunctionNameProvider functionNamer, FunctionScopeUnitConsumer consumer, StateHolder holder, LocationTracker locTracker) {
		this.errors = errors;
		this.functionNamer = functionNamer;
		this.consumer = consumer;
		this.holder = holder;
		this.locTracker = locTracker;
	}
	
	@Override
	public TDAParsing tryParsing(Tokenizable line) {
		PattToken orb = PattToken.from(errors, line);
		if (orb == null || orb.type != PattToken.ORB)
			return null;

		List<LocatedName> vars = new ArrayList<>();
		InputPosition last = null, comma = null;
		while (line.hasMoreContent(errors)) {
			PattToken nx = PattToken.from(errors, line);
			if (nx.type == PattToken.CRB) {
				if (vars.isEmpty())
					errors.message(line, "missing var in tuple declaration");
				else
					errors.message(line, "syntax error");
				return null;
			}
			if (nx.type != PattToken.VAR) {
				errors.message(line, "syntax error");
				return null;
			}
			vars.add(new LocatedName(nx.location, nx.text));
			if (comma != null) {
				errors.logReduction("comma-var-name", comma, nx.location);
			}
			PattToken cm = PattToken.from(errors, line);
			if (cm == null) {
				errors.message(line, "syntax error");
				return null;
			}
			if (cm.type == PattToken.CRB) {
				last = cm.location;
				break;
			} else if (cm.type != PattToken.COMMA) {
				errors.message(line, "syntax error");
				return null;
			}
			comma = cm.location;
		}
		
		if (last == null) {
			errors.message(line, "syntax error");
			return null;
		}
		if (vars.size() < 2) {
			errors.message(line, "insufficient vars to make tuple declaration");
			return null;
		}
		if (!line.hasMoreContent(errors)) {
			errors.message(line, "syntax error");
			return null;
		}
//		errors.logReduction("tuple-declaration-vars", orb.location, last);
		ExprToken tok = ExprToken.from(errors, line);
		if (!tok.text.equals("=")) {
			errors.message(line, "syntax error");
			return null;
		}
		if (!line.hasMoreContent(errors)) {
			errors.message(line, "tuple assignment requires expression");
			return null;
		}
		FunctionName leadName = functionNamer.functionName(vars.get(0).location, "_tuple_" + vars.get(0).text);
		FunctionName pkgName = functionNamer.functionName(vars.get(0).location, vars.get(0).text);
		new TDAExpressionParser(errors, e -> {
			consumer.tupleDefn(errors, vars, leadName, pkgName, e);
			errors.logReduction("tuple-declaration", orb.location, e.location());
		}).tryParsing(line);

		lastInner = last;
		FunctionIntroConsumer assembler = new FunctionAssembler(errors, consumer, holder, this);
		return new TDAParsingWithAction(
				ParsingPhase.functionScopeUnit(errors, new InnerPackageNamer(pkgName), assembler, consumer, holder, this),
				() -> {
					errors.logReduction("tuple-definition", orb.location, lastInner);
					locTracker.updateLoc(orb.location);
				});
	}

	@Override
	public void updateLoc(InputPosition location) {
		this.lastInner = location;
	}
	
	@Override
	public InputPosition lastInner() {
		return lastInner;
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

	public static TDAParserConstructor constructor(FunctionNameProvider namer, FunctionScopeUnitConsumer topLevel, StateHolder holder, LocationTracker locTracker) {
		return new TDAParserConstructor() {
			@Override
			public TDAParsing construct(ErrorReporter errors) {
				return new TDATupleDeclarationParser(errors, namer, topLevel, holder, locTracker);
			}
		};
	}
}
