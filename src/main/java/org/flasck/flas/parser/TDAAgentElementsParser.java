package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAAgentElementsParser implements TDAParsing, FunctionNameProvider, HandlerNameProvider, LocationTracker {
	protected final ErrorReporter errors;
	protected final InputPosition kwloc;
	protected final TemplateNamer namer;
	protected final AgentElementsConsumer consumer;
	protected final TopLevelDefinitionConsumer topLevel;
	protected final StateHolder holder;
	private boolean seenState;
	protected InputPosition lastInner;
	private KeywordToken kw;
	protected final LocationTracker tracker;
	protected Runnable currentItem;

	public TDAAgentElementsParser(ErrorReporter errors, InputPosition kwloc, TemplateNamer namer, AgentElementsConsumer consumer, TopLevelDefinitionConsumer topLevel, StateHolder holder, LocationTracker tracker) {
		this.errors = errors;
		this.kwloc = kwloc;
		this.namer = namer;
		this.consumer = consumer;
		this.topLevel = topLevel;
		this.holder = holder;
		this.tracker = tracker;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (currentItem != null) {
			currentItem.run();
			currentItem = null;
		}
		lastInner = null;
		kw = KeywordToken.from(errors, toks);
		if (kw == null) {
			return null;
		}
		switch (kw.text) {
		case "state": {
			if (seenState) {
				errors.message(kw.location, "multiple state declarations");
				return new IgnoreNestedParser(errors);
			}
			final StateDefinition state = new StateDefinition(kw.location, toks.realinfo(), ((NamedType)consumer).name());
			consumer.defineState(state);
			seenState = true;
			lastInner = kw.location;
			tracker.updateLoc(lastInner);
			
			return new TDAStructFieldParser(errors, state, new ConsumeStructFields(errors, topLevel, namer, state), FieldsType.STATE, false);
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
			final Provides contractService = new Provides(kw.location, tn.location, (NamedType)consumer, ctr, csn);
			consumer.addProvidedService(contractService);
			return new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.contractMethod(loc, csn, text), contractService, topLevel, holder);
		}
		case "requires": {
			TypeNameToken tn = TypeNameToken.qualified(errors, toks);
			if (tn == null) {
				errors.message(toks, "invalid contract reference");
				return new IgnoreNestedParser(errors);
			}
			
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
			return new NoNestingParser(errors);
		}
		case "implements": {
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
			final CSName cin = namer.csn(tn.location, "C");
			final ImplementsContract ci = new ImplementsContract(kw.location, tn.location, (NamedType)consumer, ctr, cin);
			errors.logReduction("agent-implements-contract-decl", kw.location, tn.location);
			lastInner = kw.location;
			consumer.addContractImplementation(ci);
			topLevel.newContractImpl(errors, ci);
			ImplementationMethodConsumer imc = om -> { 
				ci.addImplementationMethod(om);
				lastInner = om.location();
			};
			return new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.contractMethod(loc, cin, text), imc, topLevel, ci);
		}
		case "method": {
			FunctionNameProvider namer = (loc, text) -> FunctionName.standaloneMethod(loc, consumer.cardName(), text);
			MethodConsumer smConsumer = om -> { 
				topLevel.newStandaloneMethod(errors, new StandaloneMethod(om));
				lastInner = om.location();
			};
			return new TDAMethodParser(errors, this.namer, smConsumer, topLevel, holder, this).parseMethod(namer, toks);
		}
		default:
			return strategy(kw, toks);
		}
	}

	@Override
	public void updateLoc(InputPosition location) {
		this.lastInner = location;
	}

	// for children
	protected TDAParsing strategy(KeywordToken kw, Tokenizable toks) {
		return null;
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (currentItem != null)
			currentItem.run();
	}

	@Override
	public FunctionName functionName(InputPosition location, String base) {
		return FunctionName.function(location, consumer.cardName(), base);
	}

	@Override
	public HandlerName handlerName(String baseName) {
		return new HandlerName(consumer.cardName(), baseName);
	}
}
