package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.UtilException;

public class TemplateLineParser implements TryParsing{

	@Override
	public Object tryParsing(Tokenizable line) {
		List<Object> contents = new ArrayList<Object>();
		boolean seenDiv = false;
		while (line.hasMore()) {
			int mark = line.at();
			TemplateToken tt = TemplateToken.from(line);
			if (tt == null)
				return ErrorResult.oneMessage(line, "unrecognized token");
			if (tt.type == TemplateToken.COLON || tt.type == TemplateToken.HASH) {
				line.reset(mark);
				break;
			} else if (tt.type == TemplateToken.DIV) {
				seenDiv = true;
				contents.add(tt);
			} else if (tt.type == TemplateToken.IDENTIFIER || tt.type == TemplateToken.STRING)
				contents.add(tt);
			else
				throw new UtilException("Cannot handle " + tt);
		}
		if (seenDiv && contents.size() != 1)
			return ErrorResult.oneMessage(line, "Cannot have other content on line with <div>");
		List<String> formats = new ArrayList<String>();
		if (line.hasMore()) {
			TemplateToken tt = TemplateToken.from(line);
			if (tt.type == TemplateToken.COLON) {
				while (line.hasMore()) {
					TemplateToken f = TemplateToken.from(line);
					if (f.type == TemplateToken.IDENTIFIER)
						formats.add(f.text);
					else
						return ErrorResult.oneMessage(line, "invalid CSS format");
				}
			} else
				return ErrorResult.oneMessage(line, "syntax error");
		}
		return new TemplateLine(contents, formats);
	}

}
