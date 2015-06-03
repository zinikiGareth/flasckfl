package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.UtilException;

public class TemplateLineParser implements TryParsing{

	@Override
	public Object tryParsing(Tokenizable line) {
		List<Object> contents = new ArrayList<Object>();
		boolean seenDivOrList = false;
		while (line.hasMore()) {
			int mark = line.at();
			TemplateToken tt = TemplateToken.from(line);
			if (tt == null)
				return ErrorResult.oneMessage(line, "unrecognized token");
			if (tt.type == TemplateToken.COLON || tt.type == TemplateToken.HASH) {
				line.reset(mark);
				break;
			} else if (tt.type == TemplateToken.DIV) {
				seenDivOrList = true;
				contents.add(tt);
			} else if (tt.type == TemplateToken.LIST) {
				seenDivOrList = true;
				TemplateToken t2 = TemplateToken.from(line);
				if (t2.type != TemplateToken.IDENTIFIER)
					return ErrorResult.oneMessage(line, "list requires a list variable");
				int mark2 = line.at();
				TemplateToken t3 = TemplateToken.from(line);
				String iv = null;
				if (t3 != null && t3.type == TemplateToken.IDENTIFIER)
					iv = t3.text;
				else
					line.reset(mark2);
				contents.add(new TemplateList(t2.text, iv));
			} else if (tt.type == TemplateToken.IDENTIFIER || tt.type == TemplateToken.STRING)
				contents.add(tt);
			else
				throw new UtilException("Cannot handle " + tt);
		}
		if (seenDivOrList && contents.size() != 1)
			return ErrorResult.oneMessage(line, "Cannot have other content on line with . or +");
		List<TemplateToken> formats = new ArrayList<TemplateToken>();
		String customTag = null;
		String customTagVar = null;
		if (line.hasMore()) {
			int mark = line.at();
			TemplateToken tt = TemplateToken.from(line);
			if (tt.type == TemplateToken.HASH) {
				if (!seenDivOrList && !contents.isEmpty())
					return ErrorResult.oneMessage(line, "can only use # by itself or with . or +");
				if (!line.hasMore())
					return ErrorResult.oneMessage(line, "missing #tag");
					
				TemplateToken f = TemplateToken.from(line);
				if (f.type == TemplateToken.HASH) {
					if (!line.hasMore())
						return ErrorResult.oneMessage(line, "missing #tag");
					f = TemplateToken.from(line);
					if (f.type == TemplateToken.IDENTIFIER)
						customTagVar = f.text;
					else
						return ErrorResult.oneMessage(line, "invalid #tag");
				} else if (f.type == TemplateToken.IDENTIFIER)
					customTag = f.text;
				else
					return ErrorResult.oneMessage(line, "invalid #tag");
			} else
				line.reset(mark);
		}
		if (line.hasMore()) {
			TemplateToken tt = TemplateToken.from(line);
			if (tt.type == TemplateToken.COLON) {
				while (line.hasMore()) {
					TemplateToken f = TemplateToken.from(line);
					if (f.type == TemplateToken.IDENTIFIER || f.type == TemplateToken.STRING)
						formats.add(f);
					else
						return ErrorResult.oneMessage(line, "invalid CSS format");
				}
			} else
				return ErrorResult.oneMessage(line, "syntax error");
		}
		return new TemplateLine(contents, customTag, customTagVar, formats);
	}

}
