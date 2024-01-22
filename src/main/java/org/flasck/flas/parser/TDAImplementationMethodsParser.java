package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAImplementationMethodsParser implements TDAParsing, LocationTracker {
	private final class UpdateLocConsumer implements FunctionScopeUnitConsumer {
		private FunctionScopeUnitConsumer delegate;
		private LocationTracker updateLoc;

		public UpdateLocConsumer(LocationTracker updateLoc, FunctionScopeUnitConsumer delegate) {
			this.updateLoc = updateLoc;
			this.delegate = delegate;
		}

		@Override
		public void newHandler(ErrorReporter errors, HandlerImplements hi) {
			this.updateLoc.updateLoc(hi.location());
			this.delegate.newHandler(errors, hi);
		}

		@Override
		public void functionDefn(ErrorReporter errors, FunctionDefinition func) {
			this.updateLoc.updateLoc(func.location());
			this.delegate.functionDefn(errors, func);
		}

		@Override
		public void tupleDefn(ErrorReporter errors, List<LocatedName> vars, FunctionName leadName, FunctionName pkgName, Expr expr) {
			this.updateLoc.updateLoc(leadName.location());
			this.delegate.tupleDefn(errors, vars, leadName, pkgName, expr);
		}

		@Override
		public void polytype(ErrorReporter errors, PolyType pt) {
			this.updateLoc.updateLoc(pt.location());
			this.delegate.polytype(errors, pt);
		}

		@Override
		public void newStandaloneMethod(ErrorReporter errors, StandaloneMethod meth) {
			this.updateLoc.updateLoc(meth.location());
			this.delegate.newStandaloneMethod(errors, meth);
		}

		@Override
		public void newObjectMethod(ErrorReporter errors, ObjectActionHandler om) {
			this.updateLoc.updateLoc(om.location());
			this.delegate.newObjectMethod(errors, om);
		}

		@Override
		public void argument(ErrorReporter errors, TypedPattern with) {
			this.updateLoc.updateLoc(with.location());
			this.delegate.argument(errors, with);
		}

		@Override
		public void argument(ErrorReporter errors, VarPattern parm) {
			this.updateLoc.updateLoc(parm.location());
			this.delegate.argument(errors, parm);
		}
	}

	private final ErrorReporter errors;
	private final ImplementationMethodConsumer consumer;
	private final FunctionNameProvider namer;
	private final FunctionScopeUnitConsumer topLevel;
	private final StateHolder holder;
	private InputPosition firstLoc;
	private InputPosition firstNestedLoc;
	private InputPosition lastLoc;

	public TDAImplementationMethodsParser(ErrorReporter errors, FunctionNameProvider namer, ImplementationMethodConsumer consumer, FunctionScopeUnitConsumer topLevel, StateHolder holder) {
		this.errors = errors;
		this.namer = namer;
		this.consumer = consumer;
		this.topLevel = topLevel;
		this.holder = holder;
	}

	@Override
	public void updateLoc(InputPosition location) {
		if (firstNestedLoc == null)
			firstNestedLoc = location;
		this.lastLoc = location;
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
		firstLoc = name.location;
		List<Pattern> args = new ArrayList<>();
		final FunctionName methName = namer.functionName(name.location, name.text);
		VarPattern handler = null;
		InputPosition lastLoc = methName.location;
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
		errors.logReduction("contract-method-implementation-declaration", methName.location, lastLoc);
		consumer.addImplementationMethod(meth);
		topLevel.newObjectMethod(errors, meth);
		InnerPackageNamer innerNamer = new InnerPackageNamer(methName);
		FunctionScopeUnitConsumer fsuc = new UpdateLocConsumer(this, topLevel);
		LastOneOnlyNestedParser nestedParser = new LastActionScopeParser(errors, innerNamer, fsuc, "action", holder, null);
		return new TDAMethodGuardParser(errors, meth, nestedParser);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		// I don't think all three of these should go here, so I think
		// that when we have multiple methods in a block, these are only going to come out 
		// for the last method
		if (firstNestedLoc != null)
			errors.logReduction("implementation-method-inner-scope", firstNestedLoc, lastLoc);
		if (firstLoc != null)
			errors.logReduction("implementation-method-with-inner-scope", firstLoc, lastLoc != null ? lastLoc : firstLoc);
		if (lastLoc != null)
			errors.logReduction("agent-implements-contract-block", firstLoc, lastLoc);
	}
}
