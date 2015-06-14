package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TemplateIntro;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.QualifiedTypeNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

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
			StructDefn ret = new StructDefn(state.withPkg(tn.text), true);
			while (line.hasMore()) {
				TypeNameToken ta = TypeNameToken.from(line);
				if (ta == null)
					er.message(line, "invalid type argument");
				else
					ret.add(ta.text);
			}
			if (er.hasErrors())
				return er;
			return ret;
		}
		case "contract": {
			TypeNameToken tn = TypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid contract name");
			return new ContractDecl(state.withPkg(tn.text));
		}
		case "card": {
			TypeNameToken tn = TypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid card name");
			return new CardDefinition(state.scope, state.withPkg(tn.text));
		}
		case "state":
			return "state";
		case "template": {
			if (!line.hasMore())
				return new TemplateIntro(null, null);
			ValidIdentifierToken tok = VarNameToken.from(line);
			if (tok == null)
				return ErrorResult.oneMessage(line, "invalid template name");
			TemplateIntro ret = new TemplateIntro(tok.location, tok.text);
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
		case "implements": {
			TypeNameToken tn = QualifiedTypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid contract reference");
			if (!line.hasMore())
				return new ContractImplements(tn.location, tn.text, null, null);
			ValidIdentifierToken var = VarNameToken.from(line);
			if (var == null)
				return ErrorResult.oneMessage(line, "invalid contract var name");
			if (line.hasMore())
				return ErrorResult.oneMessage(line, "extra tokens at end of line");
			return new ContractImplements(tn.location, tn.text, var.location, var.text);
		}
		case "handler": {
			TypeNameToken tn = QualifiedTypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid contract reference");
			ArrayList<String> lambdas = new ArrayList<String>();
			if (!line.hasMore())
				return new HandlerImplements(tn.location, tn.text, lambdas);
			while (line.hasMore()) {
				ValidIdentifierToken var = VarNameToken.from(line);
				if (var == null)
					return ErrorResult.oneMessage(line, "invalid contract var name");
				lambdas.add(var.text);
			}
			return new HandlerImplements(tn.location, tn.text, lambdas);
		}
		case "event": {
			Object o = new FunctionParser(state).tryParsing(line);
			if (o == null)
				return ErrorResult.oneMessage(line, "syntax error");
			else if (o instanceof ErrorResult)
				return o;
			else if (o instanceof FunctionIntro) {
				return new EventCaseDefn((FunctionIntro)o);
			} else
				return ErrorResult.oneMessage(line, "cannot handle " + o.getClass());
		}
		default:
			// we didn't find anything we could handle - "not us"
			return null;
		}
	}

}
