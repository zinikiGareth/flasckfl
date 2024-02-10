package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAServiceElementsParser implements TDAParsing, LocationTracker {
	private final ErrorReporter errors;
	private final TemplateNamer namer;
	private final ServiceElementsConsumer consumer;
	private final TopLevelDefinitionConsumer topLevel;
	private final ServiceElementsConsumer service;
	private final LocationTracker tracker;
	protected InputPosition lastInner;
	protected Runnable currentItem;

	public TDAServiceElementsParser(ErrorReporter errors, TemplateNamer namer, ServiceElementsConsumer service, TopLevelDefinitionConsumer topLevel, LocationTracker tracker) {
		this.errors = errors;
		this.namer = namer;
		this.service = service;
		this.consumer = service;
		this.topLevel = topLevel;
		this.tracker = tracker;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (currentItem != null) {
			currentItem.run();
			currentItem = null;
		}
		KeywordToken kw = KeywordToken.from(errors, toks);
		if (kw == null)
			return null;
		switch (kw.text) {
		case "state": {
			errors.message(kw.location, "services may not have state");
			return new IgnoreNestedParser(errors);
		}
		case "implements": {
			errors.message(kw.location, "services may not implement down contracts");
			return new IgnoreNestedParser(errors);
		}
		case "method": {
			FunctionNameProvider namer = (loc, text) -> FunctionName.standaloneMethod(loc, consumer.cardName(), text);
			MethodConsumer smConsumer = sm -> { topLevel.newStandaloneMethod(errors, new StandaloneMethod(sm)); };
			return new TDAMethodParser(errors, this.namer, smConsumer, topLevel, null, this, null, false).parseMethod(kw, namer, toks);
		}
		case "provides": {
			TypeNameToken tn = TypeNameToken.qualified(errors, toks);
			if (tn == null) {
				errors.message(toks, "invalid contract reference");
				return new IgnoreNestedParser(errors);
			}
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "extra tokens at end of line");
				return new IgnoreNestedParser(errors);
			}
			final TypeReference ctr = namer.contract(tn.location, tn.text);
			final CSName csn = namer.csn(tn.location, "S");
			final Provides cs = new Provides(kw.location, tn.location, (NamedType)service, ctr, csn);
			consumer.addProvidedService(cs);
			lastInner = kw.location;
			currentItem = () -> { errors.logReduction("service-provides-block", kw.location, lastInner);};
			if (tracker != null)
				tracker.updateLoc(lastInner);
			return new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.contractMethod(loc, csn, text), cs, topLevel, null, this);
		}
		case "requires": {
			TypeNameToken tn = TypeNameToken.qualified(errors, toks);
			if (tn == null) {
				errors.message(toks, "invalid contract reference");
				return new IgnoreNestedParser(errors);
			}
			
			lastInner = kw.location;
			InputPosition varloc = null;
			String varname = null;
			if (toks.hasMoreContent(errors)) {
				ValidIdentifierToken var = VarNameToken.from(errors, toks);
				if (var == null) {
					errors.message(toks, "invalid service var name");
					return new IgnoreNestedParser(errors);
				}
				varloc = var.location;
				varname = var.text;
				lastInner = varloc;
			}
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "extra tokens at end of line");
				return new IgnoreNestedParser(errors);
			}
			final TypeReference ctr = namer.contract(tn.location, tn.text);
			final CSName cin = namer.csn(tn.location, "R");
			final RequiresContract rc = new RequiresContract(kw.location, tn.location, (NamedType)consumer, ctr, cin, varloc, varname);
			consumer.addRequiredContract(rc);
			topLevel.newRequiredContract(errors, rc);
			errors.logReduction("agent-requires", kw.location, lastInner);
			if (tracker != null)
				tracker.updateLoc(kw.location);
			return new NoNestingParser(errors);
		}
		default:
			return null;
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (currentItem != null)
			currentItem.run();
	}

	@Override
	public void updateLoc(InputPosition location) {
		if (location != null && (lastInner == null || location.compareTo(lastInner) > 0))
			lastInner = location;
	}
}
