package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.resolver.NestingChain;
import org.flasck.flas.resolver.TemplateNestingChain;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public class TDACardElementsParser extends TDAAgentElementsParser {
	public TDACardElementsParser(ErrorReporter errors, TemplateNamer namer, CardElementsConsumer consumer, TopLevelDefinitionConsumer topLevel) {
		super(errors, namer, consumer, topLevel);
	}

	@Override
	protected TDAParsing strategy(KeywordToken kw, Tokenizable toks) {
		CardElementsConsumer consumer = (CardElementsConsumer) this.consumer;
		switch (kw.text) {
		case "template": {
			TemplateNameToken tn = TemplateNameToken.from(errors, toks);
			if (tn == null) {
				errors.message(toks, "template must have a name");
				return new IgnoreNestedParser();
			}
			int pos = consumer.templatePosn();
			if (pos == 0 && toks.hasMore()) {
				errors.message(toks, "main template cannot declare chain");
				return new IgnoreNestedParser();
			}
			ErrorMark em = errors.mark();
			NestingChain chain = null;
			if (pos > 0) {
				chain = parseChain(errors, namer, toks);
				if (em.hasMoreNow())
					return new IgnoreNestedParser();
			}
			final Template template = new Template(kw.location, tn.location, consumer.templateName(tn.location, tn.text), pos, chain);
			consumer.addTemplate(template);
			topLevel.newTemplate(errors, template);
			return new TDATemplateBindingParser(errors, template, namer, template);
		}
		case "event": {
			FunctionNameProvider namer = (loc, text) -> FunctionName.eventMethod(loc, consumer.cardName(), text);
			MethodConsumer evConsumer = em -> {
				if (em.args().size() != 1) {
					errors.message(toks, "event handlers must have exactly one (typed) argument");
					return;
				}
				Pattern ev = em.args().get(0);
				if (ev instanceof VarPattern) {
					errors.message(ev.location(), "event arguments must be typed");
					return;
				}
				em.eventFor((CardDefinition)consumer);
				consumer.addEventHandler(em);
				topLevel.newObjectMethod(errors, em);
			};
			return new TDAMethodParser(errors, this.namer, evConsumer, topLevel).parseMethod(namer, toks);
		}
		default:
			return null;
		}
	}

	public static NestingChain parseChain(ErrorReporter errors, TemplateNamer namer, Tokenizable toks) {
		NestingChain chain = new TemplateNestingChain(namer);
		if (toks.hasMore()) {
			ExprToken send = ExprToken.from(errors, toks);
			if (!"<-".equals(send.text)) {
				errors.message(send.location, "expected <-");
				return null;
			}
			while (toks.hasMore()) {
				if (!readChainElement(errors, namer, toks, chain))
					return null;
			}
		}
		return chain;
	}

	private static boolean readChainElement(ErrorReporter errors, TemplateNamer namer, Tokenizable toks, NestingChain chain) {
		ExprToken tok = ExprToken.from(errors, toks);
		if (!"(".equals(tok.text)) {
			errors.message(toks, "( expected");
			return false;
		}
		TypeNameToken type = TypeNameToken.qualified(toks);
		if (type == null) {
			errors.message(toks, "type name expected");
			return false;
		}
		ValidIdentifierToken var = ValidIdentifierToken.from(toks);
		if (var == null) {
			errors.message(toks, "var name expected");
			return false;
		}
		tok = ExprToken.from(errors, toks);
		if (!")".equals(tok.text)) {
			errors.message(toks, ") expected");
			return false;
		}
		chain.declare(new TypeReference(type.location, type.text), namer.nameVar(var.location, var.text));
		return true;
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
