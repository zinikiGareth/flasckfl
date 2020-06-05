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

public class TDAServiceElementsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final TemplateNamer namer;
	private final ServiceElementsConsumer consumer;
	private final TopLevelDefinitionConsumer topLevel;
	private final ServiceElementsConsumer service;

	public TDAServiceElementsParser(ErrorReporter errors, TemplateNamer namer, ServiceElementsConsumer service, TopLevelDefinitionConsumer topLevel) {
		this.errors = errors;
		this.namer = namer;
		this.service = service;
		this.consumer = service;
		this.topLevel = topLevel;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		if (kw == null)
			return null;
		switch (kw.text) {
		case "state": {
			errors.message(kw.location, "services may not have state");
			return new IgnoreNestedParser();
		}
		case "implements": {
			errors.message(kw.location, "services may not implement down contracts");
			return new IgnoreNestedParser();
		}
		case "method": {
			FunctionNameProvider namer = (loc, text) -> FunctionName.standaloneMethod(loc, consumer.cardName(), text);
			MethodConsumer smConsumer = sm -> { topLevel.newStandaloneMethod(errors, new StandaloneMethod(sm)); };
			return new TDAMethodParser(errors, this.namer, smConsumer, topLevel, null).parseMethod(namer, toks);
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
			final Provides cs = new Provides(kw.location, tn.location, (NamedType)service, ctr, csn);
			consumer.addProvidedService(cs);
			return new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.contractMethod(loc, csn, text), cs, topLevel, null);
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
		default:
			return null;
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
