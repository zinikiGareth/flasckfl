package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.template.TemplateAttributeVar;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.TemplateCardReference;
import org.flasck.flas.parsedForm.TemplateCases;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateFormat;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TemplateOr;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;
import org.zinutils.exceptions.UtilException;

@Deprecated
public class TemplateLineParser implements TryParsing{

	@Override
	public Object tryParsing(Tokenizable line) {
		List<TemplateLine> contents = new ArrayList<TemplateLine>();
		String webzip = null;
		TemplateLine cmd = null;
		TemplateList list = null;
		InputPosition seenDiv = null;
		boolean template = false;
		boolean extractField = false;
		while (line.hasMore()) {
			InputPosition loc = line.realinfo();
			int mark = line.at();
			TemplateToken tt = TemplateToken.from(line);
			if (tt == null)
				return ErrorResult.oneMessage(line, "unrecognized token");
			if (tt.type == TemplateToken.COLON || tt.type == TemplateToken.HASH || tt.type == TemplateToken.ATTR) {
				line.reset(mark);
				break;
			} else if (seenDiv != null || list != null || cmd != null) {
				return ErrorResult.oneMessage(line.realinfo(), "div or list must be only item on line");
			} else if (tt.type == TemplateToken.ORB) {
				Object pe = new Expression().tryParsing(line);
				contents.add(new ContentExpr(tt.location, pe, new ArrayList<Object>()));
				tt = TemplateToken.from(line);
				if (tt.type != TemplateToken.CRB)
					return ErrorResult.oneMessage(line.realinfo(), "missing )");
			} else if (tt.type == TemplateToken.DIV) {
				if (!contents.isEmpty()) {
					// This logic handles the "special case" where we want to support field extraction without parens
					TemplateLine o = contents.get(contents.size()-1);
					if (o instanceof ContentExpr) {
						if (extractField)
							return ErrorResult.oneMessage(line.realinfo(), "syntax error");
						extractField = true;
						continue;
					}
					return ErrorResult.oneMessage(line.realinfo(), "div or list must be only item on line");
				}
				seenDiv = tt.location;
			} else if (tt.type == TemplateToken.WEBZIP) {
				if (!contents.isEmpty())
					return ErrorResult.oneMessage(line.realinfo(), "webzip div must be only item on line");
				TemplateToken ref = TemplateToken.from(line);
				if (ref == null || ref.type != TemplateToken.IDENTIFIER)
					return ErrorResult.oneMessage(line.realinfo(), "webzip div must be an identifier");
				seenDiv = tt.location;
				webzip = ref.text;
			} else if (tt.type == TemplateToken.LIST) {
				if (!contents.isEmpty())
					return ErrorResult.oneMessage(line.realinfo(), "div or list must be only item on line");
				TemplateToken t2 = TemplateToken.from(line);
				Object lv;
				if (t2.type == TemplateToken.IDENTIFIER) {
					lv = new UnresolvedVar(t2.location, t2.text);
				} else if (t2.type == TemplateToken.ORB) {
					lv = new Expression().tryParsing(line);
					TemplateToken crb = TemplateToken.from(line);
					if (crb.type != TemplateToken.CRB)
						return ErrorResult.oneMessage(line, "invalid list expression");
				} else
					return ErrorResult.oneMessage(line, "list requires a list variable or parenthesized expression");
				int mark2 = line.at();
				TemplateToken t3 = TemplateToken.from(line);
				InputPosition ivp = null;
				String iv = null;
				if (t3 != null && t3.type == TemplateToken.IDENTIFIER) {
					ivp = t3.location;
					iv = t3.text;
				} else
					line.reset(mark2);
				list = new TemplateList(tt.location, t2.location, lv, ivp, iv, null, null, null, null, new ArrayList<Object>(), false);
			} else if (tt.type == TemplateToken.ARROW) {
				if (seenDiv != null|| list != null || contents.size() == 0 || contents.size() > 2)
					return ErrorResult.oneMessage(line, "syntax error");
				TemplateLine action = (TemplateLine)contents.get(0);
				if (!(action instanceof ContentExpr) || !(((ContentExpr)action).expr instanceof UnresolvedVar))
					return ErrorResult.oneMessage(line, "syntax error");
				UnresolvedVar actionToken = (UnresolvedVar)((ContentExpr)action).expr;
				Object expr = new Expression().tryParsing(line);
				if (expr == null)
					return ErrorResult.oneMessage(line, "syntax error");
				else if (expr instanceof ErrorResult)
					return expr;
				else
					return new EventHandler(tt.location, actionToken.location, actionToken.var, expr);
			} else if (tt.type == TemplateToken.IDENTIFIER) {
				Object me = ItemExpr.from(new ExprToken(tt.location, ExprToken.IDENTIFIER, tt.text));
				if (extractField) { // handle the "special" case of a.b
					ContentExpr tl = (ContentExpr) contents.remove(contents.size()-1);
					contents.add(new ContentExpr(tt.location, new ApplyExpr(tt.location, ItemExpr.from(new ExprToken(tt.location, ExprToken.PUNC, ".")), Arrays.asList(tl.expr, me)), new ArrayList<Object>()));
					extractField = false;
				} else
					contents.add(new ContentExpr(tt.location, me, new ArrayList<Object>()));
			} else if (tt.type == TemplateToken.EDITABLE) {
				if (contents.isEmpty())
					return ErrorResult.oneMessage(line, "cannot have edit marker at start of line");
				Object o = contents.get(contents.size()-1);
				if (!(o instanceof ContentExpr))
					return ErrorResult.oneMessage(line, "not an editable field");
				ContentExpr ce = (ContentExpr) o;
				if (ce.editable())
					return ErrorResult.oneMessage(line, "cannot specify editable more than once");
				Object expr = ce.expr;
				if (expr instanceof ApplyExpr) {
					Object fn = ((ApplyExpr)expr).fn;
					if (!(fn instanceof UnresolvedOperator) || !((UnresolvedOperator)fn).op.equals("."))
						return ErrorResult.oneMessage(line, "not an editable field");
				}
				ce.makeEditable();
			} else if (tt.type == TemplateToken.STRING) { 
				contents.add(new ContentString(tt.location, tt.text, new ArrayList<Object>()));
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
				ValidIdentifierToken yoyo;
				String cardName = null;
				String yoyoVar = null;
				TypeNameToken cardNameTok = TypeNameToken.qualified(line);
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
				cmd = new TemplateCardReference(tt.location, loc, cardName, yoyoVar);
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
				cmd = new TemplateCases(tt.location, expr);
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
				cmd = new TemplateOr(tt.location, expr, null);
			} else
				throw new UtilException("Cannot handle " + tt);
		}
		if (extractField)
			return ErrorResult.oneMessage(line, "missing field");
		List<Object> formats = new ArrayList<Object>();
		TemplateToken customTag = null;
		TemplateToken customTagVar = null;
		List<Object> attrs = new ArrayList<Object>();
		if (line.hasMore()) {
			if (template)
				return ErrorResult.oneMessage(line, "extra tokens at end of template line");
			int mark = line.at();
			TemplateToken tt = TemplateToken.from(line);
			if (tt.type == TemplateToken.HASH || tt.type == TemplateToken.ATTR) {
				if (seenDiv == null && list == null && !contents.isEmpty())
					return ErrorResult.oneMessage(line, "can only use # by itself or with . or +");
				if (seenDiv == null && list == null)
					seenDiv = tt.location;
				if (!line.hasMore())
					return ErrorResult.oneMessage(line, "missing #tag");
					
				if (tt.type == TemplateToken.HASH) {
					TemplateToken f = TemplateToken.from(line);
					if (f.type == TemplateToken.HASH) {
						if (!line.hasMore())
							return ErrorResult.oneMessage(line, "missing #tag");
						f = TemplateToken.from(line);
						if (f.type == TemplateToken.IDENTIFIER)
							customTagVar = f;
						else
							return ErrorResult.oneMessage(line, "invalid #tag");
					} else if (f.type == TemplateToken.IDENTIFIER)
						customTag = f;
					else
						return ErrorResult.oneMessage(line, "invalid #tag");
				} else
					line.reset(mark);
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
						if (val == null)
							return ErrorResult.oneMessage(line, "syntax error");
						else if (val.type == TemplateToken.STRING)
							attrs.add(new TemplateExplicitAttr(val.location, n.text, val.type, val.text));
						else if  (val.type == TemplateToken.IDENTIFIER)
							attrs.add(new TemplateExplicitAttr(val.location, n.text, val.type, new UnresolvedVar(val.location, val.text)));
						else if (val.type == TemplateToken.ORB) {
							Expression ep = new Expression();
							Object ave = ep.tryParsing(line);
							if (ave == null)
								return ErrorResult.oneMessage(line, "could not parse attribute value expression");
							else if (ave instanceof ErrorResult)
								return ave;
							TemplateToken crb = TemplateToken.from(line);
							if (crb == null || crb.type != TemplateToken.CRB)
								return ErrorResult.oneMessage(line, "expected )");
							attrs.add(new TemplateExplicitAttr(val.location, n.text, TemplateToken.IDENTIFIER, ave));
						} else
							throw new UtilException("Cannot handle value of TEA: " + val.type);
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
		if (seenDiv != null)
			return new TemplateDiv(seenDiv, webzip, customTag != null ? customTag.location : null, customTag != null ? customTag.text : null, customTagVar != null ? customTagVar.location : null, customTagVar != null ? customTagVar.text : null, attrs, formats);
		else if (list != null) {
			if (!formats.isEmpty() || customTag != null || customTagVar != null)
				return new TemplateList(list.kw, list.listLoc, list.listExpr, list.iterLoc, list.iterVar, customTag != null ? customTag.location : null, customTag != null ? customTag.text : null, customTagVar != null ? customTagVar.location : null, customTagVar != null ? customTagVar.text : null, formats, false);
			else
				return list;
		} else if (cmd != null)
			return cmd;
		else if (!contents.isEmpty()) {
			for (TemplateLine o : contents) {
				if (o instanceof TemplateFormat)
					((TemplateFormat)o).formats.addAll(formats);
			}
			return contents;
		} else
			throw new UtilException("Huh? " + line);
	}

}
