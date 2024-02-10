package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAImplementationMethodsParser extends BlockLocationTracker implements TDAParsing {
	private final ImplementationMethodConsumer consumer;
	private final FunctionNameProvider namer;
	private final FunctionScopeUnitConsumer topLevel;
	private final StateHolder holder;
//	private InputPosition firstLoc;
//	private InputPosition firstNestedLoc;
//	private InputPosition lastLoc;

	public TDAImplementationMethodsParser(ErrorReporter errors, FunctionNameProvider namer, ImplementationMethodConsumer consumer, FunctionScopeUnitConsumer topLevel, StateHolder holder, LocationTracker parentTracker) {
		super(errors, parentTracker);
		this.namer = namer;
		this.consumer = consumer;
		this.topLevel = topLevel;
		this.holder = holder;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (!toks.hasMoreContent(errors))
			return null;
		ValidIdentifierToken name = VarNameToken.from(errors, toks);
		if (name == null) {
			errors.message(toks, "invalid method name");
			return new IgnoreNestedParser(errors);
		}
		List<Pattern> args = new ArrayList<>();
		final FunctionName methName = namer.functionName(name.location, name.text);
		VarPattern handler = null;
		InputPosition lastLoc = methName.location.locAtEnd();
		while (toks.hasMoreContent(errors)) {
			ValidIdentifierToken arg = VarNameToken.from(errors, toks);
			if (arg == null) {
				ExprToken tok = ExprToken.from(errors, toks);
				if (tok != null && "->".equals(tok.text)) {
					ValidIdentifierToken h = VarNameToken.from(errors, toks);
					if (h == null) {
						errors.message(toks, "missing or invalid handler name");
						return new IgnoreNestedParser(errors);
					}
					handler = new VarPattern(h.location, new VarName(h.location, methName, h.text));
					topLevel.argument(errors, handler);
					lastLoc = h.location;
					break;
				} else {
					errors.message(tok.location, "invalid argument name");
					return new IgnoreNestedParser(errors);
				}
			}
			final VarPattern vp = new VarPattern(arg.location, new VarName(arg.location, methName, arg.text));
			args.add(vp);
			topLevel.argument(errors, vp);
			lastLoc = vp.location();
		}
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		final ObjectMethod meth = new ObjectMethod(name.location, methName, args, handler, holder);
		errors.logReduction("implementation-method-first-line", methName.location, lastLoc);
		super.updateLoc(name.location);
		consumer.addImplementationMethod(meth);
		topLevel.newObjectMethod(errors, meth);
		InnerPackageNamer innerNamer = new InnerPackageNamer(methName);
		LastOneOnlyNestedParser nestedParser = new LastActionScopeParser(errors, innerNamer, topLevel, "action", holder, this);
		return new TDAParsingWithAction(
			new TDAMethodGuardParser(errors, meth, nestedParser, this),
			reduction(name.location, "implementation-method")
		);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
