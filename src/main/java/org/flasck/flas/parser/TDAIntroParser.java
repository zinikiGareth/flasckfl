package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.stories.TDAParserConstructor;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PolyTypeToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.zinutils.exceptions.NotImplementedException;

public class TDAIntroParser implements TDAParsing {
	private final ErrorReporter errors;
	private final TopLevelDefinitionConsumer consumer;
	private final TopLevelNamer namer;

	public TDAIntroParser(ErrorReporter errors, TopLevelNamer namer, TopLevelDefinitionConsumer consumer) {
		this.errors = errors;
		this.namer = namer;
		this.consumer = consumer;
	}
	
	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (!toks.hasMore())
			return null;
		KeywordToken kw = KeywordToken.from(toks);
		if (kw == null)
			return null; // in the "nothing doing" sense

		switch (kw.text) {
		case "agent":
		case "card":
		case "service": {
			TypeNameToken tn = TypeNameToken.unqualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser();
			}
			if (toks.hasMore()) {
				errors.message(toks, "extra tokens at end of line");
				return new IgnoreNestedParser();
			}
			
			CardName qn = namer.cardName(tn.text);
			HandlerNameProvider handlerNamer = text -> new HandlerName(qn, text);
			FunctionNameProvider functionNamer = (loc, text) -> FunctionName.function(loc, qn, text);
			FunctionIntroConsumer assembler = new FunctionAssembler(errors, consumer);

			TDAParserConstructor sh;
			HandlerBuilder hb;
			switch (kw.text) {
			case "agent": {
				AgentDefinition agent = new AgentDefinition(kw.location, tn.location, qn);
				hb = agent;
				consumer.newAgent(errors, agent);
				sh = errors -> new TDAAgentElementsParser(errors, new ObjectNestedNamer(qn), agent, consumer);
				break;
			}
			case "card": {
				CardDefinition card = new CardDefinition(kw.location, tn.location, qn);
				hb = card;
				consumer.newCard(errors, card);
				sh = errors -> new TDACardElementsParser(errors, new ObjectNestedNamer(qn), card, consumer);
				break;
			}
			case "service": {
				ServiceDefinition svc = new ServiceDefinition(kw.location, tn.location, qn);
				hb = svc;
				consumer.newService(errors, svc);
				sh = errors -> new TDAServiceElementsParser(errors, new ObjectNestedNamer(qn), svc, consumer);
				break;
			}
			default:
				throw new NotImplementedException(kw.text);
			}
			return new TDAMultiParser(errors, 
				sh,
				errors -> new TDAHandlerParser(errors, hb, handlerNamer, consumer),
				errors -> new TDAFunctionParser(errors, functionNamer, (pos, x, cn) -> namer.functionCase(pos, x, cn), assembler, consumer),
				errors -> new TDATupleDeclarationParser(errors, functionNamer, consumer)
			);
		}
		case "struct":
		case "entity":
		case "envelope":
		case "deal":
		case "offer": {
			TypeNameToken tn = TypeNameToken.unqualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser();
			}
			List<PolyType> polys = new ArrayList<>();
			while (toks.hasMore()) {
				PolyTypeToken ta = PolyTypeToken.from(toks);
				if (ta == null) {
					errors.message(toks, "invalid type argument");
					return new IgnoreNestedParser();
				} else
					polys.add(new PolyType(ta.location, ta.text));
			}
			if (toks.hasMore()) {
				errors.message(toks, "tokens after end of line");
				return new IgnoreNestedParser();
			}
			final FieldsType ty = FieldsDefn.FieldsType.valueOf(kw.text.toUpperCase());
			final StructDefn sd = new StructDefn(kw.location, tn.location, ty, namer.solidName(tn.text), true, polys);
			consumer.newStruct(errors, sd);
			return new TDAStructFieldParser(errors, new ConsumeStructFields(errors, consumer, (loc, t) -> new VarName(loc, sd.name(), t), sd), ty, true);
		}
		case "wraps": {
			TypeNameToken tn = TypeNameToken.qualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing envelope name");
				return new IgnoreNestedParser();
			}
			ExprToken send = ExprToken.from(errors, toks);
			if (toks == null || !"<-".equals(send.text)) {
				errors.message(toks, "wraps must have <-");
				return new IgnoreNestedParser();
			}
			TypeNameToken from = TypeNameToken.qualified(toks);
			if (from == null) {
				errors.message(toks, "invalid or missing wrapped type name");
				return new IgnoreNestedParser();
			}
			if (toks.hasMore()) {
				errors.message(toks, "tokens after end of line");
				return new IgnoreNestedParser();
			}
			final StructDefn sd = new StructDefn(kw.location, tn.location, FieldsType.WRAPS, namer.solidName(tn.text), true, new ArrayList<>());
			consumer.newStruct(errors, sd);
			return new TDAStructFieldParser(errors, new ConsumeStructFields(errors, consumer, (loc, t) -> new VarName(loc, sd.name(), t), sd), FieldsType.WRAPS, false);
		}
		case "union": {
			TypeNameToken tn = TypeNameToken.unqualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser();
			}
			List<PolyType> polys = new ArrayList<>();
			while (toks.hasMore()) {
				PolyTypeToken ta = PolyTypeToken.from(toks);
				if (ta == null) {
					errors.message(toks, "invalid type argument");
					return new IgnoreNestedParser();
				} else
					polys.add(new PolyType(ta.location, ta.text));
			}
			final UnionTypeDefn ud = new UnionTypeDefn(tn.location, true, namer.solidName(tn.text), polys);
			consumer.newUnion(errors, ud);
			return new TDAUnionFieldParser(errors, ud);
		}
		case "object": {
			TypeNameToken tn = TypeNameToken.unqualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser();
			}
			List<PolyType> polys = new ArrayList<>();
			while (toks.hasMore()) {
				PolyTypeToken ta = PolyTypeToken.from(toks);
				if (ta == null) {
					errors.message(toks, "syntax error");
					return new IgnoreNestedParser();
				} else
					polys.add(new PolyType(ta.location, ta.text));
			}
			if (toks.hasMore()) {
				errors.message(toks, "tokens after end of line");
				return new IgnoreNestedParser();
			}
			final SolidName on = namer.solidName(tn.text);
			ObjectDefn od = new ObjectDefn(kw.location, tn.location, on, true, polys);
			consumer.newObject(errors, od);
			HandlerNameProvider handlerNamer = text -> new HandlerName(on, text);
			FunctionNameProvider functionNamer = (loc, text) -> FunctionName.function(loc, on, text);
			FunctionIntroConsumer assembler = new FunctionAssembler(errors, consumer);
			ObjectNestedNamer onn = new ObjectNestedNamer(on);
			return new TDAMultiParser(errors, 
				errors -> {	return new TDAObjectElementsParser(errors, onn, od, consumer); },
				errors -> new TDAHandlerParser(errors, od, handlerNamer, consumer),
				errors -> new TDAFunctionParser(errors, functionNamer, (pos, x, cn) -> onn.functionCase(pos, x, cn), assembler, consumer),
				errors -> new TDATupleDeclarationParser(errors, functionNamer, consumer)
			);
		}
		case "contract": {
			KeywordToken sh = KeywordToken.from(toks);
			ContractType ct = ContractType.CONTRACT;
			if (sh != null) {
				switch (sh.text) {
				case "service":
					ct = ContractType.SERVICE;
					break;
				case "handler":
					ct = ContractType.HANDLER;
					break;
				default:
					errors.message(sh.location, "invalid contract type");
					return new IgnoreNestedParser();
				}
			}
			
			TypeNameToken tn = TypeNameToken.unqualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser();
			}
			if (toks.hasMore()) {
				errors.message(toks, "tokens after end of line");
				return new IgnoreNestedParser();
			}
			ContractDecl decl = new ContractDecl(kw.location, tn.location, ct, namer.solidName(tn.text));
			consumer.newContract(errors, decl);
			return new ContractMethodParser(errors, decl, consumer, decl.name());
		}
		case "handler": {
//			HandlerNameProvider provider = text -> consumer.handlerName(text);
			return new TDAHandlerParser(errors, null, namer, consumer).parseHandler(kw.location, false, toks);
		}
		case "method": {
//			FunctionNameProvider namer = (loc, text) -> FunctionName.standaloneMethod(loc, pkg, text);
//			HandlerNameProvider hnamer = text -> new HandlerName(pkg, text);
			MethodConsumer smConsumer = om -> { consumer.newStandaloneMethod(errors, new StandaloneMethod(om)); };
			return new TDAMethodParser(errors, namer, smConsumer, consumer).parseMethod(namer, toks);
		}
		default:
			return null;
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

	public static TDAParserConstructor constructor(TopLevelNamer namer, TopLevelDefinitionConsumer consumer) {
		return new TDAParserConstructor() {
			@Override
			public TDAParsing construct(ErrorReporter errors) {
				return new TDAIntroParser(errors, namer, consumer);
			}
		};
	}

}
