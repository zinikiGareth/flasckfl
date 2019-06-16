package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAObjectElementsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final TemplateNamer namer;
	private final ObjectElementsConsumer builder;
	private final TopLevelDefinitionConsumer topLevel;
	private TDAFunctionParser currParser;

	public TDAObjectElementsParser(ErrorReporter errors, TemplateNamer namer, ObjectElementsConsumer od, TopLevelDefinitionConsumer topLevel) {
		this.errors = errors;
		this.namer = namer;
		this.builder = od;
		this.topLevel = topLevel;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		InputPosition location = toks.realinfo();
		KeywordToken kw = KeywordToken.from(toks);
		switch (kw.text) {
		case "state": {
			if (toks.hasMore()) {
				errors.message(toks, "extra characters at end of line");
				return new IgnoreNestedParser();
			}
			StateDefinition state = new StateDefinition(toks.realinfo());
			builder.defineState(state);
			return new TDAStructFieldParser(errors, new ConsumeStructFields(topLevel, namer, state), FieldsType.STATE);
		}
		case "template": {
			TemplateNameToken tn = TemplateNameToken.from(toks);
			final Template template = new Template(kw.location, tn.location, namer.template(tn.text), null, null);
			builder.addTemplate(template);
			return new TDATemplateBindingParser(errors, template);
		}
		case "ctor": {
			ValidIdentifierToken var = VarNameToken.from(toks);
			FunctionName fnName = namer.ctor(var.location, var.text);
			List<Pattern> args = new ArrayList<>();
			TDAPatternParser pp = new TDAPatternParser(errors, p -> {
				args.add(p);
			});
			while (pp.tryParsing(toks) != null)
				;
			if (toks.hasMore()) {
				errors.message(toks, "extra characters at end of line");
				return new IgnoreNestedParser();
			}
			ObjectCtor ctor = new ObjectCtor(var.location, fnName, args);
			builder.addConstructor(ctor);
			return new TDAMethodMessageParser(errors, ctor, new LastActionScopeParser(errors, namer, topLevel, "action"));
		}
		case "acor": {
			if (currParser != null)
				currParser.scopeComplete(location);
			FunctionAssembler fa = new FunctionAssembler(errors, topLevel);
			FunctionIntroConsumer consumer = new FunctionIntroConsumer() {
				@Override
				public void functionIntro(FunctionIntro o) {
					fa.functionIntro(o);
					builder.addAccessor(new ObjectAccessor());
				}

				@Override
				public void moveOn() {
					fa.moveOn();
				}
			};
			TDAFunctionParser fcp = new TDAFunctionParser(errors, namer, consumer, topLevel);
			currParser = fcp;
			return fcp.tryParsing(toks);
		}
		case "method": {
			FunctionNameProvider methodNamer = (loc, text) -> namer.method(loc, text);
			return new TDAMethodParser(errors, namer, builder, topLevel).parseMethod(methodNamer, toks);
		}
		default: {
			return null;
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (currParser != null)
			currParser.scopeComplete(location);
	}
}
