package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.resolver.NestingChain;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAObjectElementsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final TemplateNamer namer;
	private final ObjectElementsConsumer builder;
	private final TopLevelDefinitionConsumer topLevel;
	private TDAParsing currParser;

	public TDAObjectElementsParser(ErrorReporter errors, TemplateNamer namer, ObjectElementsConsumer od, TopLevelDefinitionConsumer topLevel) {
		this.errors = errors;
		this.namer = namer;
		this.builder = od;
		this.topLevel = topLevel;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		InputPosition location = toks.realinfo();
		KeywordToken kw = KeywordToken.from(errors, toks);
		if (kw == null) {
			return null; // try something else - e.g. functions 
		}
		switch (kw.text) {
		case "state": {
			if (toks.hasMoreContent()) {
				errors.message(toks, "extra characters at end of line");
				return new IgnoreNestedParser();
			}
			StateDefinition state = new StateDefinition(toks.realinfo(), ((NamedType)builder).name());
			builder.defineState(state);
			return new TDAStructFieldParser(errors, new ConsumeStructFields(errors, topLevel, namer, state), FieldsType.STATE, false);
		}
		case "requires": {
			TypeNameToken tn = TypeNameToken.qualified(errors, toks);
			if (tn == null) {
				errors.message(toks, "invalid contract reference");
				return new IgnoreNestedParser();
			}
			
			if (!toks.hasMoreContent()) {
				errors.message(toks, "missing variable name");
				return new IgnoreNestedParser();
			}
			ValidIdentifierToken var = VarNameToken.from(errors, toks);
			if (var == null) {
				errors.message(toks, "invalid service var name");
				return new IgnoreNestedParser();
			}
			if (toks.hasMoreContent()) {
				errors.message(toks, "extra tokens at end of line");
				return new IgnoreNestedParser();
			}
			final TypeReference ctr = namer.contract(tn.location, tn.text);
			VarName cv = namer.nameVar(var.location, var.text);
			ObjectContract oc = new ObjectContract(var.location, ctr, cv);
			builder.requireContract(oc);
			topLevel.newObjectContract(errors, oc);
			return new NoNestingParser(errors);
		}
		case "template": {
			TemplateNameToken tn = TemplateNameToken.from(errors, toks);
			ErrorMark em = errors.mark();
			int pos = builder.templatePosn();
			NestingChain chain = null;
			if (toks.hasMoreContent()) 
				chain = TDACardElementsParser.parseChain(errors, namer, toks);
			if (em.hasMoreNow())
				return new IgnoreNestedParser();
			final Template template = new Template(kw.location, tn.location, namer.template(tn.location, tn.text), pos, chain);
			builder.addTemplate(template);
			topLevel.newTemplate(errors, template);
			return new TDATemplateBindingParser(errors, template, namer, template);
		}
		case "event": {
			FunctionNameProvider namer = (loc, text) -> FunctionName.eventMethod(loc, builder.name(), text);
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
				em.eventFor((ObjectDefn)builder);
				ev.isDefinedBy(em);
				builder.addEventHandler(em);
				topLevel.newObjectMethod(errors, em);
			};
			return new TDAMethodParser(errors, this.namer, evConsumer, topLevel, (StateHolder) builder).parseMethod(namer, toks);
		}
		case "ctor": {
			ValidIdentifierToken var = VarNameToken.from(errors, toks);
			FunctionName fnName = namer.ctor(var.location, var.text);
			List<Pattern> args = new ArrayList<>();
			TDAPatternParser pp = new TDAPatternParser(errors, new SimpleVarNamer(fnName), p -> {
				args.add(p);
			}, topLevel);
			while (pp.tryParsing(toks) != null)
				;
			if (toks.hasMoreContent()) {
				errors.message(toks, "extra characters at end of line");
				return new IgnoreNestedParser();
			}
			ObjectCtor ctor = new ObjectCtor(var.location, (Type)builder, fnName, args) {
				public void done() {
					super.done();
					topLevel.newObjectMethod(errors, this);
				}
			};
			for (Pattern p : args) {
				p.isDefinedBy(ctor);
			}
			builder.addConstructor(ctor);
			if (currParser != null) {
				currParser.scopeComplete(location);
				currParser = null;
			}
			FunctionScopeNamer ctorNamer = new PackageNamer(fnName);
			return new TDAMethodGuardParser(errors, ctor, new LastActionScopeParser(errors, ctorNamer, topLevel, "action", (StateHolder) builder));
		}
		case "acor": {
			if (currParser != null)
				currParser.scopeComplete(location);
			FunctionAssembler fa = new FunctionAssembler(errors, new CaptureFunctionDefinition(topLevel, (errors, f) -> { ObjectAccessor oa = new ObjectAccessor((StateHolder) builder, f); f.isObjAccessor(true); builder.addAccessor(oa); topLevel.newObjectAccessor(errors, oa); }), (StateHolder)builder);
			TDAFunctionParser fcp = new TDAFunctionParser(errors, namer, (pos, x, cn) -> namer.functionCase(pos, x, cn), fa, topLevel, (StateHolder)builder);
			currParser = fcp;
			return fcp.tryParsing(toks);
		}
		case "method": {
			FunctionNameProvider methodNamer = (loc, text) -> namer.method(loc, text);
			MethodConsumer dispenser = new MethodConsumer() {
				@Override
				public void addMethod(ObjectMethod method) {
					builder.addMethod(method);
					topLevel.newObjectMethod(errors, method);
				}
			};
			return new TDAMethodParser(errors, namer, dispenser, topLevel, (StateHolder) builder).parseMethod(methodNamer, toks);
		}
		default: {
			return null;
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		builder.complete(errors, location);
		if (currParser != null)
			currParser.scopeComplete(location);
	}
}
