package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.PlatformSpec;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.template.TemplateIntro;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.D3Intro;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PolyTypeToken;
import org.flasck.flas.tokenizers.QualifiedTypeNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;

public class IntroParser implements TryParsing {
	private final State state;

	public IntroParser(State state) {
		this.state = state;
	}
	
	@Override
	public Object tryParsing(Tokenizable line) {
		if (!line.hasMore())
			return null;
		KeywordToken kw = KeywordToken.from(line);
		if (kw == null)
			return null; // in the "nothing doing" sense
		
		switch (kw.text) {
		case "struct": {
			TypeNameToken tn = TypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid type name");
			ErrorResult er = new ErrorResult();
			List<PolyType> args = new ArrayList<PolyType>();
			while (line.hasMore()) {
				PolyTypeToken ta = PolyTypeToken.from(line);
				if (ta == null) {
					er.message(line, "invalid type argument");
					break;
				} else
					args.add(new PolyType(ta.location, ta.text));
			}
			if (er.hasErrors())
				return er;
			return new StructDefn(kw.location, tn.location, state.withPkg(tn.text), true, args);
		}
		case "object": {
			TypeNameToken tn = TypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid type name");
			ErrorResult er = new ErrorResult();
			List<PolyType> args = new ArrayList<PolyType>();
			while (line.hasMore()) {
				PolyTypeToken ta = PolyTypeToken.from(line);
				if (ta == null) {
					er.message(line, "invalid type argument");
					break;
				} else
					args.add(new PolyType(ta.location, ta.text));
			}
			if (er.hasErrors())
				return er;
			return new ObjectDefn(tn.location, state.scope, state.withPkg(tn.text), true, args);
		}
		case "contract": {
			TypeNameToken tn = TypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid contract name");
			return new ContractDecl(kw.location, tn.location, state.withPkg(tn.text));
		}
		case "card": {
			TypeNameToken tn = TypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid card name");
			return new CardDefinition(kw.location, tn.location, state.scope, new CardName(state.pkgName, tn.text));
		}
		case "platform": {
			ValidIdentifierToken tok = VarNameToken.from(line);
			if (tok == null)
				return ErrorResult.oneMessage(line, "must specify a platform descriptor");
			return new PlatformSpec(tok.location, tok.text);
		}
		case "state":
			return "state";
		case "template": {
			if (!line.hasMore())
				return new TemplateIntro(kw.location, null, null);
			ValidIdentifierToken tok = VarNameToken.from(line);
			if (tok == null)
				return ErrorResult.oneMessage(line, "invalid template name");
			TemplateIntro ret = new TemplateIntro(kw.location, tok.location, tok.text);
			Set<String> vars = new TreeSet<String>();
			while (line.hasMore()) {
				tok = VarNameToken.from(line);
				if (tok == null)
					return ErrorResult.oneMessage(line, "invalid var parameter");
				if (vars.contains(tok.text))
					return ErrorResult.oneMessage(tok.location, "duplicate var parameter " + tok.text);
				ret.args.add(new LocatedToken(tok.location, tok.text));
			}
			return ret;
		}
		case "d3": { // d3 name from-expr element-name
			ValidIdentifierToken tok = VarNameToken.from(line);
			if (tok == null)
				return ErrorResult.oneMessage(line, "invalid D3 template name");

			// TODO: this should allow for expressions if parenthesized
			ValidIdentifierToken exprTok = VarNameToken.from(line);
			if (exprTok == null)
				return ErrorResult.oneMessage(line, "invalid D3 expression");
			Object expr = ItemExpr.from(new ExprToken(exprTok.location, ExprToken.IDENTIFIER, exprTok.text));

			ValidIdentifierToken var = VarNameToken.from(line);
			if (var == null)
				return ErrorResult.oneMessage(line, "invalid D3 expression");
			
			return new D3Intro(kw.location, tok.location, tok.text, expr, var.location, var.text);
		}
		case "implements": {
			TypeNameToken tn = QualifiedTypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid contract reference");
			if (!line.hasMore())
				return new ContractImplements(kw.location, tn.location, tn.text, null, null);
			ValidIdentifierToken var = VarNameToken.from(line);
			if (var == null)
				return ErrorResult.oneMessage(line, "invalid contract var name");
			if (line.hasMore())
				return ErrorResult.oneMessage(line, "extra tokens at end of line");
			return new ContractImplements(kw.location, tn.location, tn.text, var.location, var.text);
		}
		case "service": {
			TypeNameToken tn = QualifiedTypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid contract reference");
			if (!line.hasMore())
				return new ContractService(kw.location, tn.location, tn.text, null, null);
			ValidIdentifierToken var = VarNameToken.from(line);
			if (var == null)
				return ErrorResult.oneMessage(line, "invalid service var name");
			if (line.hasMore())
				return ErrorResult.oneMessage(line, "extra tokens at end of line");
			return new ContractService(kw.location, tn.location, tn.text, var.location, var.text);
		}
		case "handler": {
			if (!line.hasMore())
				return ErrorResult.oneMessage(line, "missing contract reference");
			TypeNameToken tn = QualifiedTypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid contract reference");
			if (!line.hasMore())
				return ErrorResult.oneMessage(line, "missing handler name");
			TypeNameToken named = TypeNameToken.from(line);
			if (named == null)
				return ErrorResult.oneMessage(line, "invalid handler name");
			ArrayList<Object> lambdas = new ArrayList<Object>();
			if (!line.hasMore())
				return new HandlerImplements(kw.location, named.location, tn.location, state.withPkg(named.text), tn.text, state.kind == CodeType.CARD, lambdas);
			while (line.hasMore()) {
				PatternParser pp = new PatternParser();
				Object patt = pp.tryParsing(line);
				if (patt == null)
					return ErrorResult.oneMessage(line, "invalid contract argument pattern");
				lambdas.add(patt);
			}
			return new HandlerImplements(kw.location, named.location, tn.location, state.withPkg(named.text), tn.text, state.kind == CodeType.CARD, lambdas);
		}
		case "event": {
			Object o = new FunctionParser(state.as(CodeType.EVENTHANDLER)).tryParsing(line);
			if (o == null)
				return ErrorResult.oneMessage(line, "syntax error");
			else if (o instanceof ErrorResult)
				return o;
			else if (o instanceof FunctionIntro) {
				return new EventCaseDefn(kw.location, (FunctionIntro)o);
			} else
				return ErrorResult.oneMessage(line, "cannot handle " + o.getClass());
		}
		case "method": {
			if (!line.hasMore())
				return ErrorResult.oneMessage(line, "missing method name");
			ValidIdentifierToken tn = VarNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid method name");
			ArrayList<Object> args = new ArrayList<Object>();
			while (line.hasMore()) {
				PatternParser pp = new PatternParser();
				Object patt = pp.tryParsing(line);
				if (patt == null)
					return ErrorResult.oneMessage(line, "invalid contract argument pattern");
				args.add(patt);
			}
			FunctionName fname = state.functionName(tn);
			return new MethodCaseDefn(new FunctionIntro(fname, args));
		}
		default:
			// we didn't find anything we could handle - "not us"
			return null;
		}
	}

}
