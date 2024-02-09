package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.resolver.NestingChain;
import org.flasck.flas.resolver.TemplateNestingChain;
import org.flasck.flas.resolver.TemplateNestingChain.Link;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public class TDACardElementsParser extends TDAAgentElementsParser {
	private static class TemplateBindingCaptureLoc implements TemplateBindingConsumer {
		private final LocationTracker tracker;
		private final Template template;

		private TemplateBindingCaptureLoc(LocationTracker tracker, Template template) {
			this.tracker = tracker;
			this.template = template;
		}

		@Override
		public void addStyling(TemplateStylingOption style) {
			tracker.updateLoc(style.location());
			template.addStyling(style);
		}

		@Override
		public void addBinding(TemplateBinding binding) {
			tracker.updateLoc(binding.location());
			template.addBinding(binding);
		}
	}

	public TDACardElementsParser(ErrorReporter errors, InputPosition kwloc, TemplateNamer namer, CardElementsConsumer consumer, TopLevelDefinitionConsumer topLevel, StateHolder holder, LocationTracker tracker) {
		super(errors, kwloc, namer, consumer, topLevel, holder, tracker);
	}

	@Override
	protected TDAParsing strategy(KeywordToken kw, Tokenizable toks) {
		CardElementsConsumer consumer = (CardElementsConsumer) this.consumer;
		switch (kw.text) {
		case "template": {
			TemplateNameToken tn = TemplateNameToken.from(errors, toks);
			if (tn == null) {
				errors.message(toks, "template must have a name");
				return new IgnoreNestedParser(errors);
			}
			InputPosition lastLoc = tn.location;
			int pos = consumer.templatePosn();
			if (pos == 0 && toks.hasMoreContent(errors)) {
				errors.message(toks, "main template cannot declare chain");
				return new IgnoreNestedParser(errors);
			}
			ErrorMark em = errors.mark();
			NestingChain chain = null;
			if (pos > 0) {
				chain = parseChain(errors, namer, toks);
				if (em.hasMoreNow())
					return new IgnoreNestedParser(errors);
				if (chain.location() != null)
					lastLoc = chain.location();
			}
			if (chain != null)
				errors.logReduction("card-template-intro-with-chain", kw.location, lastLoc);
			else
				errors.logReduction("card-template-intro", kw.location, lastLoc);
			updateLoc(kw.location);
			final Template template = new Template(kw.location, tn.location, consumer.templateName(tn.location, tn.text), pos, chain);
			consumer.addTemplate(template);
			topLevel.newTemplate(errors, template);
			TemplateBindingConsumer c = new TemplateBindingCaptureLoc(this, template);
			return new TDAParsingWithAction(
				new TDATemplateBindingParser(errors, template, namer, c, this),
				reduction(kw.location, "named-template-definition")
			);
		}
		case "event": {
			updateLoc(kw.location);
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
				ev.isDefinedBy(em);
				em.eventFor((CardDefinition)consumer);
				consumer.addEventHandler(em);
				topLevel.newObjectMethod(errors, em);
				errors.logReduction("event-from-method", kw.location, ev.location());
			};
			return new TDAParsingWithAction(
				new TDAMethodParser(errors, this.namer, evConsumer, topLevel, holder, this, false).parseMethod(kw, namer, toks),
				reduction(kw.location, "event-with-method-actions")
			);
		}
		default:
			return null;
		}
	}

	public static NestingChain parseChain(ErrorReporter errors, TemplateNamer namer, Tokenizable toks) {
		NestingChain chain = new TemplateNestingChain(namer);
		if (toks.hasMoreContent(errors)) {
			ExprToken send = ExprToken.from(errors, toks);
			if (!"<-".equals(send.text)) {
				errors.message(send.location, "expected <-");
				return null;
			}
			InputPosition chainN = null;
			while (toks.hasMoreContent(errors)) {
				chainN = readChainElement(errors, namer, toks, chain);
				if (chainN == null)
					return null;
			}
			errors.logReduction("define-template-chain", send.location, chainN);
		}
		return chain;
	}

	private static InputPosition readChainElement(ErrorReporter errors, TemplateNamer namer, Tokenizable toks, NestingChain chain) {
		ExprToken tok = ExprToken.from(errors, toks);
		if (tok == null || !"(".equals(tok.text)) {
			errors.message(toks, "( expected");
			return null;
		}
		InputPosition orb = tok.location;
		List<TypeReference> ref = new ArrayList<>();
		if (new TDATypeReferenceParser(errors, namer, x->ref.add(x), null).tryParsing(toks) == null) {
			// it didn't parse, so give up hope
			return null;
		}
		TypeReference tr = ref.get(0);

		ValidIdentifierToken var = ValidIdentifierToken.from(errors, toks);
		if (var == null) {
			errors.message(toks, "var name expected");
			return null;
		}
		tok = ExprToken.from(errors, toks);
		if (!")".equals(tok.text)) {
			errors.message(toks, ") expected");
			return null;
		}
		errors.logReduction("template-chain-var", orb, tok.location);
		chain.declare(tr, namer.nameVar(var.location, var.text));
		return orb;
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
