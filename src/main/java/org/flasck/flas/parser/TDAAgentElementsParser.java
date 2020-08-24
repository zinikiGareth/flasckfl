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

public class TDAAgentElementsParser implements TDAParsing, FunctionNameProvider, HandlerNameProvider {
	protected final ErrorReporter errors;
	protected final TemplateNamer namer;
	protected final AgentElementsConsumer consumer;
	protected final TopLevelDefinitionConsumer topLevel;
	protected final StateHolder holder;
	private boolean seenState;

	public TDAAgentElementsParser(ErrorReporter errors, TemplateNamer namer, AgentElementsConsumer consumer, TopLevelDefinitionConsumer topLevel, StateHolder holder) {
		this.errors = errors;
		this.namer = namer;
		this.consumer = consumer;
		this.topLevel = topLevel;
		this.holder = holder;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		if (kw == null) {
			return null;
		}
		switch (kw.text) {
		case "state": {
			if (seenState) {
				errors.message(kw.location, "multiple state declarations");
				return new IgnoreNestedParser();
			}
			final StateDefinition state = new StateDefinition(toks.realinfo(), ((NamedType)consumer).name());
			consumer.defineState(state);
			seenState = true;
			
			return new TDAStructFieldParser(errors, new ConsumeStructFields(errors, topLevel, namer, state), FieldsType.STATE, false);
		}
		case "provides": {
			TypeNameToken tn = TypeNameToken.qualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid contract reference");
				return new IgnoreNestedParser();
			}
			if (toks.hasMoreContent()) {
				errors.message(toks, "extra tokens at end of line");
				return new IgnoreNestedParser();
			}
			final TypeReference ctr = namer.contract(tn.location, tn.text);
			final CSName csn = namer.csn(tn.location, "S");
			final Provides contractService = new Provides(kw.location, tn.location, (NamedType)consumer, ctr, csn);
			consumer.addProvidedService(contractService);
			return new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.contractMethod(loc, csn, text), contractService, topLevel, holder);
		}
		case "requires": {
			TypeNameToken tn = TypeNameToken.qualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid contract reference");
				return new IgnoreNestedParser();
			}
			
			InputPosition varloc = null;
			String varname = null;
			if (toks.hasMoreContent()) {
				ValidIdentifierToken var = VarNameToken.from(toks);
				if (var == null) {
					errors.message(toks, "invalid service var name");
					return new IgnoreNestedParser();
				}
				varloc = var.location;
				varname = var.text;
			}
			if (toks.hasMoreContent()) {
				errors.message(toks, "extra tokens at end of line");
				return new IgnoreNestedParser();
			}
			final TypeReference ctr = namer.contract(tn.location, tn.text);
			final CSName cin = namer.csn(tn.location, "R");
			final RequiresContract rc = new RequiresContract(kw.location, tn.location, (NamedType)consumer, ctr, cin, varloc, varname);
			consumer.addRequiredContract(rc);
			topLevel.newRequiredContract(errors, rc);
			return new NoNestingParser(errors);
		}
		case "implements": {
			TypeNameToken tn = TypeNameToken.qualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid contract reference");
				return new IgnoreNestedParser();
			}
			if (toks.hasMoreContent()) {
				errors.message(toks, "extra tokens at end of line");
				return new IgnoreNestedParser();
			}
			final TypeReference ctr = namer.contract(tn.location, tn.text);
			final CSName cin = namer.csn(tn.location, "C");
			final ImplementsContract ci = new ImplementsContract(kw.location, tn.location, (NamedType)consumer, ctr, cin);
			consumer.addContractImplementation(ci);
			topLevel.newContractImpl(errors, ci);
			return new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.contractMethod(loc, cin, text), ci, topLevel, ci);
		}
		case "method": {
			FunctionNameProvider namer = (loc, text) -> FunctionName.standaloneMethod(loc, consumer.cardName(), text);
			MethodConsumer smConsumer = om -> { topLevel.newStandaloneMethod(errors, new StandaloneMethod(om)); };
			return new TDAMethodParser(errors, this.namer, smConsumer, topLevel, holder).parseMethod(namer, toks);
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
