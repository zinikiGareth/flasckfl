package org.flasck.flas.parser;

import java.util.ArrayList;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.QualifiedTypeNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
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
		String kw = KeywordToken.from(line);
		if (kw == null)
			return null; // in the "nothing doing" sense
		
		switch (kw) {
		case "struct": {
			String tn = TypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid type name");
			ErrorResult er = new ErrorResult();
			StructDefn ret = new StructDefn(tn);
			while (line.hasMore()) {
				String ta = TypeNameToken.from(line);
				if (ta == null)
					er.message(line, "invalid type argument");
				else
					ret.add(ta);
			}
			if (er.hasErrors())
				return er;
			return ret;
		}
		case "contract": {
			String tn = TypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid contract name");
			return new ContractDecl(tn);
		}
		case "card": {
			String tn = TypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid card name");
			return new CardDefinition(state.scope, state.withPkg(tn));
		}
		case "state":
			return "state";
		case "template":
			return "template";
		case "implements": {
			String tn = QualifiedTypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid contract reference");
			if (!line.hasMore())
				return new ContractImplements(tn, null);
			String var = VarNameToken.from(line);
			if (var == null)
				return ErrorResult.oneMessage(line, "invalid contract var name");
			if (line.hasMore())
				return ErrorResult.oneMessage(line, "extra tokens at end of line");
			return new ContractImplements(tn, var);
		}
		case "handler": {
			String tn = QualifiedTypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid contract reference");
			ArrayList<String> lambdas = new ArrayList<String>();
			if (!line.hasMore())
				return new HandlerImplements(tn, lambdas);
			while (line.hasMore()) {
				String var = VarNameToken.from(line);
				if (var == null)
					return ErrorResult.oneMessage(line, "invalid contract var name");
				lambdas.add(var);
			}
			return new HandlerImplements(tn, lambdas);
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
		}
		
		// we didn't find anything we could handle - "not us"
		return null;
	}

}
