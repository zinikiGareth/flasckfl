package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDACardElementsParser extends TDAAgentElementsParser {
	public TDACardElementsParser(ErrorReporter errors, TemplateNamer namer, CardElementsConsumer consumer, TopLevelDefinitionConsumer topLevel) {
		super(errors, namer, consumer, topLevel);
	}

	@Override
	protected TDAParsing strategy(KeywordToken kw, Tokenizable toks) {
		CardElementsConsumer consumer = (CardElementsConsumer) this.consumer;
		switch (kw.text) {
		case "template": {
			TemplateNameToken tn = TemplateNameToken.from(toks);
			if (tn == null) {
				errors.message(toks, "template must have a name");
				return new IgnoreNestedParser();
			}
			final Template template = new Template(kw.location, tn.location, consumer.templateName(tn.text), null, null);
			consumer.addTemplate(template);
			return new TDATemplateBindingParser(errors, template);
		}
		case "event": {
			FunctionNameProvider namer = (loc, text) -> FunctionName.eventMethod(loc, consumer.cardName(), text);
			MethodConsumer evConsumer = em -> { consumer.addEventHandler(em); topLevel.newObjectMethod(errors, em); };
			return new TDAMethodParser(errors, this.namer, evConsumer, topLevel).parseMethod(namer, toks);
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
