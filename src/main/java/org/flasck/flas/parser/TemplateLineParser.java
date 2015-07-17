package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.CardReference;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.TemplateAttributeVar;
import org.flasck.flas.parsedForm.TemplateCases;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TemplateOr;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.QualifiedTypeNameToken;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;
import org.zinutils.exceptions.UtilException;

public class TemplateLineParser implements TryParsing{

	@Override
	public Object tryParsing(Tokenizable line) {
		List<TemplateLine> contents = new ArrayList<TemplateLine>();
		TemplateLine cmd = null;
		TemplateList list = null;
		boolean seenDiv = false;
		boolean template = false;
		while (line.hasMore()) {
			int mark = line.at();
			TemplateToken tt = TemplateToken.from(line);
			if (tt == null)
				return ErrorResult.oneMessage(line, "unrecognized token");
			if (tt.type == TemplateToken.COLON || tt.type == TemplateToken.HASH) {
				line.reset(mark);
				break;
			} else if (seenDiv || list != null || cmd != null) {
				return ErrorResult.oneMessage(line.realinfo(), "div or list must be only item on line");
			} else if (tt.type == TemplateToken.ORB) {
				line.reset(mark);
				Object pe = new Expression().tryParsing(line);
				contents.add(new ContentExpr(pe, new ArrayList<Object>()));
			} else if (tt.type == TemplateToken.DIV) {
				seenDiv = true;
				if (!contents.isEmpty())
					return ErrorResult.oneMessage(line.realinfo(), "div or list must be only item on line");
			} else if (tt.type == TemplateToken.LIST) {
				if (!contents.isEmpty())
					return ErrorResult.oneMessage(line.realinfo(), "div or list must be only item on line");
				InputPosition pos = line.realinfo();
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
				list = new TemplateList(pos, t2.text, iv, new ArrayList<Object>());
			} else if (tt.type == TemplateToken.ARROW) {
				if (seenDiv || list != null || contents.size() == 0 || contents.size() > 2)
					return ErrorResult.oneMessage(line, "syntax error");
				TemplateLine action = (TemplateLine)contents.get(0);
				if (!(action instanceof ContentExpr) || !(((ContentExpr)action).expr instanceof UnresolvedVar))
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
					return new EventHandler(((UnresolvedVar)((ContentExpr)action).expr).var, expr);
			} else if (tt.type == TemplateToken.IDENTIFIER) {
				contents.add(new ContentExpr(ItemExpr.from(new ExprToken(ExprToken.IDENTIFIER, tt.text)), new ArrayList<Object>()));
			} else if (tt.type == TemplateToken.STRING) { 
				contents.add(new ContentString(tt.text, new ArrayList<Object>()));
			} else if (tt.type == TemplateToken.TEMPLATE) {
				template = true;
				if (!contents.isEmpty()) {
					return ErrorResult.oneMessage(line, "template must be the only content item");
				}
				Expression expr = new Expression();
				List<Object> args = new ArrayList<Object>();
				while (line.hasMore()) {
					ExprToken et = ExprToken.from(line);
					if (et == null)
						return ErrorResult.oneMessage(line, "syntax error");
					Object ex;
					if (et.type == ExprToken.PUNC && et.text.equals("(")) {
						ex = expr.tryParsing(line);
						if (ex instanceof ErrorResult)
							return ex;
						if (!line.hasMore())
							return ErrorResult.oneMessage(line, "syntax error");
						et = ExprToken.from(line);
						if (et == null || et.type != ExprToken.PUNC || !et.text.equals(")"))
							return ErrorResult.oneMessage(line, "syntax error");
					} else
						ex = ItemExpr.from(et);
					if (ex == null)
						return ErrorResult.oneMessage(line, "syntax error");
					args.add(ex);
				}
				cmd = new TemplateReference(tt.location, tt.text, args);
			} else if (tt.type == TemplateToken.CARD) {
				if (!contents.isEmpty()) {
					return ErrorResult.oneMessage(line, "card must be the only content item");
				}
				InputPosition loc;
				ValidIdentifierToken yoyo;
				String cardName = null;
				String yoyoVar = null;
				TypeNameToken cardNameTok = QualifiedTypeNameToken.from(line);
				if (cardNameTok != null) {
					loc = cardNameTok.location;
					cardName = cardNameTok.text;
				} else {
					yoyo = VarNameToken.from(line);
					if (yoyo == null)
						return ErrorResult.oneMessage(line, "Must specify card name or variable");
					loc = yoyo.location;
					yoyoVar = yoyo.text;
				}

				// TODO: more card invocation syntax
				//   * mode = local|sandbox|trusted|dialog
				//   * -> handleVar
				cmd = new CardReference(loc, cardName, yoyoVar);
			} else if (tt.type == TemplateToken.CASES) {
				if (!contents.isEmpty()) {
					return ErrorResult.oneMessage(line, "cases must be the only content item");
				}
				Object expr = null;
				if (line.hasMore()) {
					Expression ep = new Expression();
					expr = ep.tryParsing(line);
					if (expr == null)
						return ErrorResult.oneMessage(line, "syntax error");
					else if (expr instanceof ErrorResult)
						return expr;
				}
				if (line.hasMore())
					return ErrorResult.oneMessage(line, "extraneous symbols at end of cases line");
				cmd = new TemplateCases(line.realinfo(), expr);
			} else if (tt.type == TemplateToken.OR) {
				if (!contents.isEmpty()) {
					return ErrorResult.oneMessage(line, "or must be the only content item");
				}
				Object expr = null;
				if (line.hasMore()) {
					Expression ep = new Expression();
					expr = ep.tryParsing(line);
					if (expr == null)
						return ErrorResult.oneMessage(line, "syntax error");
					else if (expr instanceof ErrorResult)
						return expr;
				}
				if (line.hasMore())
					return ErrorResult.oneMessage(line, "extraneous symbols at end of cases line");
				cmd = new TemplateOr(expr, null);
			} else
				throw new UtilException("Cannot handle " + tt);
		}
		List<Object> formats = new ArrayList<Object>();
		String customTag = null;
		String customTagVar = null;
		List<Object> attrs = new ArrayList<Object>();
		if (line.hasMore()) {
			if (template)
				return ErrorResult.oneMessage(line, "extra tokens at end of template line");
			int mark = line.at();
			TemplateToken tt = TemplateToken.from(line);
			if (tt.type == TemplateToken.HASH) {
				if (!seenDiv && list == null && !contents.isEmpty())
					return ErrorResult.oneMessage(line, "can only use # by itself or with . or +");
				if (!seenDiv && list == null)
					seenDiv = true;
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
					if (f == null)
						return ErrorResult.oneMessage(line, "syntax error");
					if (f != null && (f.type == TemplateToken.IDENTIFIER || f.type == TemplateToken.STRING))
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
		if (seenDiv)
			return new TemplateDiv(customTag, customTagVar, attrs, formats);
		else if (list != null) {
			if (!formats.isEmpty())
				return new TemplateList(list.listLoc, list.listVar, list.iterVar, formats);
			else
				return list;
		} else if (cmd != null)
			return cmd;
		else if (!contents.isEmpty())
			return contents;
		else
			throw new UtilException("Huh? " + line);
	}

}
