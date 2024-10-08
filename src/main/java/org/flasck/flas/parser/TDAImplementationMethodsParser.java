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
		String withResult = "";
		InputPosition lastLoc = methName.location.locAtEnd();
		
		// 2024-06-18 To avoid cascade errors, when there are errors processing the arguments,
		// we still add the method to the repo, even though it is incorrect to avoid cascading errors about "method not implemented" 
		boolean sawError = false;
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
					errors.logReduction("implementation-result", tok, h);
					lastLoc = tok.location;
					withResult = "-with-result";
					break;
				} else {
					errors.message(tok.location, "invalid argument name");
					sawError = true;
					break;
				}
			}
			final VarPattern vp = new VarPattern(arg.location, new VarName(arg.location, methName, arg.text));
			args.add(vp);
			topLevel.argument(errors, vp);
			lastLoc = vp.location();
		}
		final ObjectMethod meth = new ObjectMethod(name.location, methName, args, handler, holder);
		errors.logReduction("implementation-method-first-line" + withResult, methName.location, lastLoc);
		super.updateLoc(name.location);
		consumer.addImplementationMethod(meth);
		topLevel.newObjectMethod(errors, meth);
		if (!sawError && toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			sawError = true;
		}
		if (sawError) {
			// now return 
			meth.broke();
			return new IgnoreNestedParser(errors);
		}
		InnerPackageNamer innerNamer = new InnerPackageNamer(methName);
		LastOneOnlyNestedParser nestedParser = new LastActionScopeParser(errors, innerNamer, topLevel, "action", holder, this);
		return new TDAParsingWithAction(
			new TDAMethodGuardParser(errors, meth, nestedParser, this),
			() -> {
				if (!meth.messages().isEmpty()) {
					reduce(meth.messages().get(0).location(), "method-actions");
				}
				reduce(name.location, "implementation-method");
			}
		);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
