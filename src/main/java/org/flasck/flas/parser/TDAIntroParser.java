package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.ObjectName;
import org.flasck.flas.commonBase.names.SolidName;
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
import org.flasck.flas.parsedForm.StateHolder;
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

public class TDAIntroParser implements TDAParsing, LocationTracker {
	private final ErrorReporter errors;
	private final TopLevelDefinitionConsumer consumer;
	private final TopLevelNamer namer;
	private Runnable onComplete;
	private InputPosition lastInner;

	public TDAIntroParser(ErrorReporter errors, TopLevelNamer namer, TopLevelDefinitionConsumer consumer) {
		this.errors = errors;
		this.namer = namer;
		this.consumer = consumer;
	}
	
	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (!toks.hasMoreContent(errors))
			return null;
		KeywordToken kw = KeywordToken.from(errors, toks);
		if (kw == null)
			return null; // in the "nothing doing" sense

		lastInner = kw.location;
		switch (kw.text) {
		case "agent":
		case "card":
		case "service": {
			TypeNameToken tn = TypeNameToken.unqualified(errors, toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser(errors);
			}
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "extra tokens at end of line");
				return new IgnoreNestedParser(errors);
			}
			
			CardName qn = namer.cardName(tn.text);
			HandlerNameProvider handlerNamer = text -> new HandlerName(qn, text);
			FunctionNameProvider functionNamer = (loc, text) -> FunctionName.function(loc, qn, text);

			TDAParserConstructor sh;
			HandlerBuilder hb;
			StateHolder state = null;
			switch (kw.text) {
			case "agent": {
				AgentDefinition agent = new AgentDefinition(kw.location, tn.location, qn);
				hb = agent;
				state = agent;
				consumer.newAgent(errors, agent);
				sh = errors -> new TDAAgentElementsParser(errors, kw.location, new ObjectNestedNamer(qn), agent, consumer, agent, this);
				break;
			}
			case "card": {
				CardDefinition card = new CardDefinition(kw.location, tn.location, qn);
				hb = card;
				state = card;
				errors.logReduction("card-declaration", kw.location, tn.location);
				consumer.newCard(errors, card);
				sh = errors -> new TDACardElementsParser(errors, kw.location, new ObjectNestedNamer(qn), card, consumer, card, this);
				break;
			}
			case "service": {
				ServiceDefinition svc = new ServiceDefinition(kw.location, tn.location, qn);
				hb = svc;
				consumer.newService(errors, svc);
				sh = errors -> new TDAServiceElementsParser(errors, new ObjectNestedNamer(qn), svc, consumer, this);
				break;
			}
			default:
				throw new NotImplementedException(kw.text);
			}
			final StateHolder holder = state;
			FunctionAssembler assembler = new FunctionAssembler(errors, consumer, holder, this);
			onComplete = () -> { errors.logReduction("card-definition", kw.location, lastInner); };
			return new TDAMultiParser(errors, 
				sh,
				errors -> new TDAHandlerParser(errors, hb, handlerNamer, consumer, holder, this),
				errors -> new TDAFunctionParser(errors, functionNamer, (pos, base, cn) -> FunctionName.caseName(functionNamer.functionName(pos, base), cn), assembler, consumer, holder, this),
				errors -> new TDATupleDeclarationParser(errors, functionNamer, consumer, holder)
			);
		}
		case "struct":
		case "entity":
		case "envelope":
		case "deal":
		case "offer": {
			TypeNameToken tn = TypeNameToken.unqualified(errors, toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser(errors);
			}
			SolidName sn = namer.solidName(tn.text);
			SimpleVarNamer svn = new SimpleVarNamer(sn);
			List<PolyType> polys = new ArrayList<>();
			while (toks.hasMoreContent(errors)) {
				PolyTypeToken ta = PolyTypeToken.from(errors, toks);
				if (ta == null) {
					errors.message(toks, "invalid type argument");
					return new IgnoreNestedParser(errors);
				} else
					polys.add(ta.asType(svn));
			}
			// todo: need to reduce a poly-type name
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "tokens after end of line");
				return new IgnoreNestedParser(errors);
			}
			final FieldsType ty = FieldsDefn.FieldsType.valueOf(kw.text.toUpperCase());
			final StructDefn sd = new StructDefn(kw.location, tn.location, ty, sn, true, polys);
			consumer.newStruct(errors, sd);
			errors.logReduction("fields-defn", kw.location, tn.location);
			return new TDAStructFieldParser(errors, sd, new ConsumeStructFields(errors, consumer, svn, sd), ty, true);
		}
		case "wraps": {
			TypeNameToken tn = TypeNameToken.qualified(errors, toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing envelope name");
				return new IgnoreNestedParser(errors);
			}
			ExprToken send = ExprToken.from(errors, toks);
			if (toks == null || !"<-".equals(send.text)) {
				errors.message(toks, "wraps must have <-");
				return new IgnoreNestedParser(errors);
			}
			TypeNameToken from = TypeNameToken.qualified(errors, toks);
			if (from == null) {
				errors.message(toks, "invalid or missing wrapped type name");
				return new IgnoreNestedParser(errors);
			}
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "tokens after end of line");
				return new IgnoreNestedParser(errors);
			}
			SolidName sn = namer.solidName(tn.text);
			SimpleVarNamer svn = new SimpleVarNamer(sn);
			final StructDefn sd = new StructDefn(kw.location, tn.location, FieldsType.WRAPS, namer.solidName(tn.text), true, new ArrayList<>());
			consumer.newStruct(errors, sd);
			return new TDAStructFieldParser(errors, sd, new ConsumeStructFields(errors, consumer, svn, sd), FieldsType.WRAPS, false);
		}
		case "union": {
			TypeNameToken tn = TypeNameToken.unqualified(errors, toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser(errors);
			}
			SolidName sn = namer.solidName(tn.text);
			SimpleVarNamer svn = new SimpleVarNamer(sn);
			List<PolyType> polys = new ArrayList<>();
			while (toks.hasMoreContent(errors)) {
				PolyTypeToken ta = PolyTypeToken.from(errors, toks);
				if (ta == null) {
					errors.message(toks, "invalid type argument");
					return new IgnoreNestedParser(errors);
				} else
					polys.add(ta.asType(svn));
			}
			errors.logReduction("union-defn", kw.location, tn.location);
			final UnionTypeDefn ud = new UnionTypeDefn(tn.location, true, namer.solidName(tn.text), polys);
			consumer.newUnion(errors, ud);
			return new TDAUnionFieldParser(errors, kw.location, ud);
		}
		case "object": {
			TypeNameToken tn = TypeNameToken.unqualified(errors, toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser(errors);
			}
			InputPosition lastLoc = tn.location;
			ObjectName on = namer.objectName(tn.text);
			SimpleVarNamer svn = new SimpleVarNamer(on);
			List<PolyType> polys = new ArrayList<>();
			while (toks.hasMoreContent(errors)) {
				PolyTypeToken ta = PolyTypeToken.from(errors, toks);
				if (ta == null) {
					errors.message(toks, "syntax error");
					return new IgnoreNestedParser(errors);
				} else
					polys.add(ta.asType(svn));
				lastLoc = ta.location;
			}
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "tokens after end of line");
				return new IgnoreNestedParser(errors);
			}
			errors.logReduction("object-defn-decl", kw.location, lastLoc);
			ObjectDefn od = new ObjectDefn(kw.location, tn.location, on, true, polys);
			consumer.newObject(errors, od);
			HandlerNameProvider handlerNamer = text -> new HandlerName(on, text);
			FunctionNameProvider functionNamer = (loc, text) -> FunctionName.function(loc, on, text);
			FunctionIntroConsumer assembler = new FunctionAssembler(errors, consumer, od, this);
			ObjectNestedNamer onn = new ObjectNestedNamer(on);
			TDAMultiParser ret = new TDAMultiParser(errors, 
				errors -> new TDAObjectElementsParser(errors, onn, od, consumer),
				errors -> new TDAHandlerParser(errors, od, handlerNamer, consumer, od, this),
				errors -> new TDAFunctionParser(errors, functionNamer, (pos, x, cn) -> onn.functionCase(pos, x, cn), assembler, consumer, od, this),
				errors -> new TDATupleDeclarationParser(errors, functionNamer, consumer, od)
			);
			ret.onComplete((errors,location) -> {
				errors.logReduction("object-defn-complete", kw.location, location);
				od.complete(errors, location);
			});
			return ret;
		}
		case "contract": {
			KeywordToken sh = KeywordToken.from(errors, toks);
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
					return new IgnoreNestedParser(errors);
				}
			}
			
			TypeNameToken tn = TypeNameToken.unqualified(errors, toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser(errors);
			}
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "tokens after end of line");
				return new IgnoreNestedParser(errors);
			}
			ContractDecl decl = new ContractDecl(kw.location, tn.location, ct, namer.solidName(tn.text));
			errors.logReduction("contract-decl-type", kw.location, tn.location);
			consumer.newContract(errors, decl);
			return new ContractMethodParser(errors, kw.location, decl, consumer, decl.name());
		}
		case "handler": {
//			HandlerNameProvider provider = text -> consumer.handlerName(text);
			return new TDAHandlerParser(errors, null, namer, consumer, null, this).parseHandler(kw.location, false, toks);
		}
		case "method": {
//			FunctionNameProvider namer = (loc, text) -> FunctionName.standaloneMethod(loc, pkg, text);
//			HandlerNameProvider hnamer = text -> new HandlerName(pkg, text);
			MethodConsumer smConsumer = om -> { consumer.newStandaloneMethod(errors, new StandaloneMethod(om)); };
			return new TDAMethodParser(errors, namer, smConsumer, consumer, null, this).parseMethod(namer, toks);
		}
		default:
			return null;
		}
	}
	
	@Override
	public void updateLoc(InputPosition location) {
		if (location != null && (lastInner == null || location.compareTo(lastInner) > 0))
			lastInner = location;
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (onComplete != null) {
			onComplete.run();
		} else 
			System.out.println("Did not have onComplete set at " + location);
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
