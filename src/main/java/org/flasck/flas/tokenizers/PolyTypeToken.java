package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parser.VarNamer;

public class PolyTypeToken implements LoggableToken {
	public final InputPosition location;
	public final String text;

	public PolyTypeToken(ValidIdentifierToken tok) {
		location = tok.location;
		text = tok.text;
	}

	public static PolyTypeToken from(ErrorReporter errors, Tokenizable line) {
		line.skipWS(errors);
		if (!line.hasMore())
			return null;

		int mark = line.at();
		char c = line.nextChar();
		if (!Character.isUpperCase(c)) {
			// types must be upper case
			line.reset(mark);
			return null;
		}
		ValidIdentifierToken tok = ValidIdentifierToken.from(errors, line);
		String tx = tok.text;
		if (!validate(tx)) {
			// would be a real type
			line.reset(mark);
			return null;
		}
		return errors.logParsingToken(new PolyTypeToken(tok));
	}

	public static PolyType fromToken(InputPosition pos, VarNamer namer, String tok) {
		if (validate(tok))
			return new PolyType(pos, namer.namePoly(pos, tok));
		else
			return null;
	}
	
	public static boolean validate(String tn) {
		if (tn.length() > 2)
			return false;
		if (!Character.isUpperCase(tn.charAt(0)))
			return false;
		if (tn.length() == 2 && !Character.isUpperCase(tn.charAt(1)) && !Character.isDigit(tn.charAt(1)))
			return false;
		return true;
	}

	public PolyType asType(VarNamer namer) {
		return new PolyType(location, namer.namePoly(location, text));
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String type() {
		return "PolyType";
	}

	@Override
	public String text() {
		return text;
	}
}
