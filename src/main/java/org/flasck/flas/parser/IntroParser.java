package org.flasck.flas.parser;

import java.util.ArrayList;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.QualifiedNameToken;
import org.flasck.flas.tokenizers.QualifiedTypeNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class IntroParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		if (!line.hasMore())
			return null;
		String kw = KeywordToken.from(line);
		if (kw == null)
			return null; // in the "nothing doing" sense
		
		switch (kw) {
		case "package": {
			String pn = QualifiedNameToken.from(line);
			if (line.hasMore())
				return ErrorResult.oneMessage(line, "extra tokens at end of line");
			return new PackageDefn(pn);
		}
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
		case "card": {
			String tn = TypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid contract name");
			return new CardDefinition(tn);
		}
		case "state":
			return "state";
		case "template":
			return "template";
		}
		
		// we didn't find anything we could handle - "not us"
		return null;
	}

}
