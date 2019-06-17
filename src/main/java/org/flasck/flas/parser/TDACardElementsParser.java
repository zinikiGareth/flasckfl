package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDACardElementsParser implements TDAParsing, FunctionNameProvider, HandlerNameProvider {
	private final ErrorReporter errors;
	private final TemplateNamer namer;
	private final CardElementsConsumer consumer;
	private final TopLevelDefinitionConsumer topLevel;
	private boolean seenState;

	public TDACardElementsParser(ErrorReporter errors, TemplateNamer namer, CardElementsConsumer consumer, TopLevelDefinitionConsumer topLevel) {
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
			
			return new TDAStructFieldParser(errors, new ConsumeStructFields(topLevel, namer, state), FieldsType.STATE);
		}
		case "template": {
			TemplateNameToken tn = TemplateNameToken.from(toks);
			final Template template = new Template(kw.location, tn.location, consumer.templateName(tn.text), null, null);
			consumer.addTemplate(template);
			return new TDATemplateBindingParser(errors, template);
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
			final ContractService contractService = new ContractService(kw.location, tn.location, new TypeReference(tn.location, tn.text), null, null);
			consumer.addProvidedService(contractService);
			return new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.contractMethod(loc, contractService.name(), text), contractService, topLevel);
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
			final ContractImplements ci = new ContractImplements(kw.location, tn.location, new TypeReference(tn.location, tn.text), varloc, varname);
			consumer.addContractImplementation(ci);
			return new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.contractMethod(loc, ci.name(), text), ci, topLevel);
		}
		case "event": {
			FunctionNameProvider namer = (loc, text) -> FunctionName.eventMethod(loc, consumer.cardName(), text);
			MethodConsumer evConsumer = em -> { consumer.addEventHandler(em); topLevel.newObjectMethod(em); };
			return new TDAMethodParser(errors, this.namer, evConsumer, topLevel).parseMethod(namer, toks);
		}
		case "method": {
			FunctionNameProvider namer = (loc, text) -> FunctionName.standaloneMethod(loc, consumer.cardName(), text);
			MethodConsumer smConsumer = om -> { topLevel.newStandaloneMethod(new StandaloneMethod(om)); };
			return new TDAMethodParser(errors, this.namer, smConsumer, topLevel).parseMethod(namer, toks);
		}
		default:
			return null;
		}
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
