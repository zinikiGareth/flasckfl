package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.TemplateBlockIntro;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TemplateBlockIntroParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		TemplateToken name = TemplateToken.from(line);
		if (name == null || name.type != TemplateToken.IDENTIFIER)
			return ErrorResult.oneMessage(line, "identifier expected");
		TemplateToken arrow = TemplateToken.from(line);
		if (arrow == null || arrow.type != TemplateToken.ASSIGN)
			return ErrorResult.oneMessage(line, "<- expected");
		if (line.hasMore())
			return ErrorResult.oneMessage(line, "syntax error");
		return new TemplateBlockIntro(name.location, name.text);
	}
}
