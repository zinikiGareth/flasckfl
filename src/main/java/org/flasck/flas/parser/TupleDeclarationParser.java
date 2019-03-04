package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.PattToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TupleDeclarationParser implements TryParsing {
//	private final State state;

	public TupleDeclarationParser(State state) {
//		this.state = state;
	}

	@Override
	public Object tryParsing(Tokenizable line) {
		PattToken orb = PattToken.from(line);
		if (orb.type != PattToken.ORB)
			return null;
		
		List<LocatedName> vars = new ArrayList<LocatedName>();
		while (line.hasMore()) {
			PattToken nx = PattToken.from(line);
			if (nx.type != PattToken.VAR) {
				if (vars.isEmpty())
					return null;
				else
					return ErrorResult.oneMessage(nx.location, "syntax error parsing tuple");
			}
			vars.add(new LocatedName(nx.location, nx.text));
			PattToken cm = PattToken.from(line);
			if (cm.type == PattToken.CRB)
				break;
			else if (cm.type != PattToken.COMMA)
				return ErrorResult.oneMessage(line, "missing comma in tuple declaration");
		}
		
		if (!line.hasMore())
			return ErrorResult.oneMessage(line, "tuple assignment requires expression");
			
		Object expr = new Expression().tryParsing(line);
		if (expr instanceof ErrorResult)
			return expr;

		return new TupleAssignment(true, vars, expr);
	}

}
