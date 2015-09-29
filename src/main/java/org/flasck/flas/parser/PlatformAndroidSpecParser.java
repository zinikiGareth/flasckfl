package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.PlatformSpec;
import org.flasck.flas.parsedForm.android.AndroidLabel;
import org.flasck.flas.parsedForm.android.AndroidLaunch;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class PlatformAndroidSpecParser implements TryParsing {

	public PlatformAndroidSpecParser(PlatformSpec ps) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Object tryParsing(Tokenizable line) {
		KeywordToken kw = KeywordToken.from(line);
		if (kw.text.equals("launcher")) {
			if (line.hasMore())
				return ErrorResult.oneMessage(line, "extraneous text at end of line");
			return new AndroidLaunch(kw.location);
		} else if (kw.text.equals("label")) {
			if (!line.hasMore())
				return ErrorResult.oneMessage(line, "expected label string");
			InputPosition pos = line.realinfo();
			String label = StringToken.from(line);
			if (line.hasMore())
				return ErrorResult.oneMessage(line, "extraneous text at end of line");
			return new AndroidLabel(pos, label);
		}
		// TODO Auto-generated method stub
		return null;
	}

}
