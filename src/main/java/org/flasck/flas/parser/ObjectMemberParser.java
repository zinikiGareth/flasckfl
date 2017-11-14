package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.ObjectMember;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class ObjectMemberParser implements TryParsing {
	private final State state;

	public ObjectMemberParser(State state) {
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
		case "state": {
			if (line.hasMore())
				return ErrorResult.oneMessage(line, "syntax error");
			return "state";
		}
		case "ctor": {
			return new ObjectMember(ObjectMember.CTOR, new FunctionParser(state).tryParsing(line));
		}
		case "acor": {
			return new ObjectMember(ObjectMember.ACCESSOR, new FunctionParser(state).tryParsing(line));
		}
		case "method": {
			return new ObjectMember(ObjectMember.METHOD, new FunctionParser(state).tryParsing(line));
		}
		case "internal": {
			return new ObjectMember(ObjectMember.INTERNAL, new FunctionParser(state).tryParsing(line));
		}
		default:
			// we didn't find anything we could handle - "not us"
			return null;
		}
	}

}
