package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StateDefinition;
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
	private boolean seenState;

	public TDAAgentElementsParser(ErrorReporter errors, TemplateNamer namer, AgentElementsConsumer consumer, TopLevelDefinitionConsumer topLevel) {
		this.errors = errors;
		this.namer = namer;
		this.consumer = consumer;
		this.topLevel = topLevel;
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
			final StateDefinition state = new StateDefinition(toks.realinfo());
			consumer.defineState(state);
			seenState = true;
			
			return new TDAStructFieldParser(errors, new ConsumeStructFields(topLevel, namer, state), FieldsType.STATE, false);
		}
		case "provides": {
			TypeNameToken tn = TypeNameToken.qualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid contract reference");
				return new IgnoreNestedParser();
			}
			if (toks.hasMore()) {
				errors.message(toks, "extra tokens at end of line");
				return new IgnoreNestedParser();
			}
			final TypeReference ctr = namer.contract(tn.location, tn.text);
			final CSName csn = namer.csn(tn.location, "S");
			final Provides contractService = new Provides(kw.location, tn.location, (NamedType)consumer, ctr, csn);
			consumer.addProvidedService(contractService);
			return new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.contractMethod(loc, csn, text), contractService, topLevel);
		}
		case "implements": {
			TypeNameToken tn = TypeNameToken.qualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid contract reference");
				return new IgnoreNestedParser();
			}
			
			InputPosition varloc = null;
			String varname = null;
			if (toks.hasMore()) {
				ValidIdentifierToken var = VarNameToken.from(toks);
				if (var == null) {
					errors.message(toks, "invalid service var name");
					return new IgnoreNestedParser();
				}
				varloc = var.location;
				varname = var.text;
			}
			if (toks.hasMore()) {
				errors.message(toks, "extra tokens at end of line");
				return new IgnoreNestedParser();
			}
			final TypeReference ctr = namer.contract(tn.location, tn.text);
			final CSName cin = namer.csn(tn.location, "C");
			final ContractImplements ci = new ContractImplements(kw.location, tn.location, (NamedType)consumer, ctr, cin, varloc, varname);
			consumer.addContractImplementation(ci);
			return new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.contractMethod(loc, cin, text), ci, topLevel);
		}
		case "method": {
			FunctionNameProvider namer = (loc, text) -> FunctionName.standaloneMethod(loc, consumer.cardName(), text);
			MethodConsumer smConsumer = om -> { topLevel.newStandaloneMethod(new StandaloneMethod(om)); };
			return new TDAMethodParser(errors, this.namer, smConsumer, topLevel).parseMethod(namer, toks);
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
