package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
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

public class TDAAgentElementsParser extends BlockLocationTracker implements TDAParsing, FunctionNameProvider, HandlerNameProvider {
	protected final InputPosition kwloc;
	protected final TemplateNamer namer;
	protected final AgentElementsConsumer consumer;
	protected final TopLevelDefinitionConsumer topLevel;
	protected final StateHolder holder;
	private boolean seenState;
	private KeywordToken kw;

	public TDAAgentElementsParser(ErrorReporter errors, InputPosition kwloc, TemplateNamer namer, AgentElementsConsumer consumer, TopLevelDefinitionConsumer topLevel, StateHolder holder, LocationTracker tracker) {
		super(errors, tracker);
		this.kwloc = kwloc;
		this.namer = namer;
		this.consumer = consumer;
		this.topLevel = topLevel;
		this.holder = holder;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
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
			errors.logReduction("agent-state-line", kw.location, kw.location);
			updateLoc(kw.location);
			
			return new TDAParsingWithAction(
				new TDAStructFieldParser(errors, new ConsumeStructFields(errors, topLevel, namer, state), FieldsType.STATE, false, this),
				reduction(kw.location, "agent-state-block")
			);
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
			updateLoc(tn.location);
			final TypeReference ctr = namer.contract(tn.location, tn.text);
			final CSName csn = namer.csn(tn.location, "S");
			final Provides contractService = new Provides(kw.location, tn.location, (NamedType)consumer, ctr, csn);
			consumer.addProvidedService(contractService);
			return new TDAParsingWithAction(
				new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.contractMethod(loc, csn, text), contractService, topLevel, holder, this),
				reduction(kw.location, "agent-provides-block")
			);
		}
		case "requires": {
			TypeNameToken tn = TypeNameToken.qualified(errors, toks);
			if (tn == null) {
				errors.message(toks, "invalid contract reference");
				return new IgnoreNestedParser(errors);
			}

			InputPosition lastLoc = tn.location;
			InputPosition varloc = null;
			String varname = null;
			if (toks.hasMoreContent(errors)) {
				ValidIdentifierToken var = VarNameToken.from(errors, toks);
				if (var == null) {
					errors.message(toks, "invalid service var name");
					return new IgnoreNestedParser(errors);
				}
				varloc = var.location;
				lastLoc = varloc;
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
			errors.logReduction("agent-requires", kw.location, lastLoc);
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
			updateLoc(kw.location);
			consumer.addContractImplementation(ci);
			topLevel.newContractImpl(errors, ci);
			return new TDAParsingWithAction(
				new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.contractMethod(loc, cin, text), ci, topLevel, ci, this),
				reduction(kw.location, "agent-implements-contract-block")
			);
		}
		case "method": {
			FunctionNameProvider namer = (loc, text) -> FunctionName.standaloneMethod(loc, consumer.cardName(), text);
			MethodConsumer smConsumer = om -> { 
				topLevel.newStandaloneMethod(errors, new StandaloneMethod(om));
			};
			return new TDAMethodParser(errors, this.namer, smConsumer, topLevel, holder, this).parseMethod(kw, namer, toks);
		}
		default:
			return strategy(kw, toks);
		}
	}

	// for children
	protected TDAParsing strategy(KeywordToken kw, Tokenizable toks) {
		return null;
	}

	@Override
	public void scopeComplete(InputPosition location) {
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
