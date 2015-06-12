package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.TemplateAttributeVar;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
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
			} else if (tt.type == TemplateToken.ORB) {
				line.reset(mark);
				Object pe = new Expression().tryParsing(line);
				contents.add(pe);
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
			} else if (tt.type == TemplateToken.ARROW) {
				if (seenDivOrList || contents.size() == 0 || contents.size() > 2)
					return ErrorResult.oneMessage(line, "syntax error");
				TemplateToken action = (TemplateToken)contents.get(0);
				if (action.type != TemplateToken.IDENTIFIER)
					return ErrorResult.oneMessage(line, "syntax error");
				/* Right now, in generating this, I'm unclear what this var was for
				String var = null;
				if (contents.size() == 2) {
					TemplateToken varis = (TemplateToken)contents.get(1);
					if (varis.type != TemplateToken.IDENTIFIER)
						return ErrorResult.oneMessage(line, "syntax error");
					var = varis.text;
				}
				*/
				Object expr = new Expression().tryParsing(line);
				if (expr == null)
					return ErrorResult.oneMessage(line, "syntax error");
				else if (expr instanceof ErrorResult)
					return expr;
				else
					return new EventHandler(action.text, expr);
			} else if (tt.type == TemplateToken.IDENTIFIER || tt.type == TemplateToken.STRING)
				contents.add(tt);
			else
				throw new UtilException("Cannot handle " + tt);
		}
		if (seenDivOrList && contents.size() != 1)
			return ErrorResult.oneMessage(line, "cannot have other content on line with . or +");
		List<Object> formats = new ArrayList<Object>();
		String customTag = null;
		String customTagVar = null;
		List<Object> attrs = new ArrayList<Object>();
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
				while (line.hasMore()) {
					mark = line.at();
					TemplateToken at = TemplateToken.from(line);
					if (at == null || at.type != TemplateToken.ATTR) {
						line.reset(mark);
						break;
					}
					boolean wantVar = false;
					TemplateToken n = TemplateToken.from(line);
					if (n == null)
						return ErrorResult.oneMessage(line, "syntax error");
					if (n.type == TemplateToken.ATTR) {
						wantVar = true;
						n = TemplateToken.from(line);
						if (n == null)
							return ErrorResult.oneMessage(line, "syntax error");
					}
					if (n.type != TemplateToken.IDENTIFIER)
						return ErrorResult.oneMessage(line, "invalid attribute");
					if (wantVar)
						attrs.add(new TemplateAttributeVar(n.text));
					else {
						mark = line.at();
						TemplateToken eq = TemplateToken.from(line);
						if (eq == null || eq.type != TemplateToken.EQUALS) {
							line.reset(mark);
							break;
						}
						TemplateToken val = TemplateToken.from(line);
						if (val == null || (val.type != TemplateToken.IDENTIFIER && val.type != TemplateToken.STRING))
							return ErrorResult.oneMessage(line, "syntax error");
						attrs.add(new TemplateExplicitAttr(n.text, val.type, val.text));
					}
				}
			} else
				line.reset(mark);
		}
		if (line.hasMore()) {
			TemplateToken tt = TemplateToken.from(line);
			if (tt == null)
				return ErrorResult.oneMessage(line, "could not parse token");
			if (tt.type == TemplateToken.COLON) {
				while (line.hasMore()) {
					TemplateToken f = TemplateToken.from(line);
					if (f!= null && (f.type == TemplateToken.IDENTIFIER || f.type == TemplateToken.STRING))
						formats.add(f);
					else if (f.type == TemplateToken.ORB) {
						Expression ep = new Expression();
						Object fe = ep.tryParsing(line);
						if (fe == null)
							return ErrorResult.oneMessage(line, "could not parse format expression");
						else if (fe instanceof ErrorResult)
							return fe;
						f = TemplateToken.from(line);
						if (f == null || f.type != TemplateToken.CRB)
							return ErrorResult.oneMessage(line, "expected )");
						formats.add(fe);
					} else
						return ErrorResult.oneMessage(line, "invalid CSS format (did you mean to quote it?)");
				}
			} else
				return ErrorResult.oneMessage(line, "syntax error");
		}
//		if (line.hasMore())
//			return ErrorResult.oneMessage(line, "unparsed tokens at end of line");
		return new TemplateLine(contents, customTag, customTagVar, attrs, formats);
	}

}
