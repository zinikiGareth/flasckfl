package org.flasck.flas.stories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CardReference;
import org.flasck.flas.parsedForm.ContainsScope;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionClause;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.IfExpr;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.MessagesHandler;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateCases;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateIntro;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TemplateOr;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.FieldParser;
import org.flasck.flas.parser.FunctionClauseParser;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.parser.IntroParser;
import org.flasck.flas.parser.MethodMessageParser;
import org.flasck.flas.parser.MethodParser;
import org.flasck.flas.parser.TemplateLineParser;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;

public class FLASStory implements StoryProcessor {
	public static class State {
		private String pkg;
		public final Scope scope;
		public final HSIEForm.Type kind;

		public State(Scope scope, String pkg, HSIEForm.Type kind) {
			this.scope = scope;
			this.pkg = pkg;
			this.kind = kind;
		}

		public String withPkg(String name) {
			if (pkg == null || pkg.length() == 0)
				return name;
			return pkg +"." + name;
		}

		public static String simpleName(String key) {
			int idx = key.lastIndexOf(".");
			return key.substring(idx+1);
		}
	}

	@Override
	public Object process(ScopeEntry top, List<Block> blocks) {
		PackageDefn pkg = (PackageDefn) top.getValue();
		State s = new State(pkg.innerScope(), pkg.name, HSIEForm.Type.FUNCTION);
		ErrorResult er = new ErrorResult();
		doScope(er, s, blocks);
		if (er.hasErrors())
			return er;
		return top;
	}
	
	private Object doScope(ErrorResult er, State s, List<Block> blocks) {
		if (blocks.isEmpty())
			return null;

		Scope ret = s.scope;
		List<FunctionCaseDefn> fndefns = new ArrayList<FunctionCaseDefn>();
		String lastFn = null;
		int cs = 0;
		for (Block b : blocks) {
			if (b.isComment())
				continue;
			
			// TODO: if it's a "package", deal with that ... and all blocks must either be or not be packages
			Object o = new MultiParser(s, IntroParser.class, FunctionParser.class).parse(b);
			if (o == null) {
				System.out.println("Could not parse " + b.line.text());
				er.message(new Tokenizable(b), "syntax error");
				continue;
			}
			else if (o instanceof ErrorResult) {
				er.merge((ErrorResult)o);
				continue;
			}
			if (o instanceof FunctionCaseDefn) {
				FunctionCaseDefn fcd = (FunctionCaseDefn)o;
				fndefns.add(fcd);
				if (lastFn != null && !fcd.intro.name.equals(lastFn)) {
					lastFn = fcd.intro.name;
					cs = 0;
				}
				if (!b.nested.isEmpty()) {
					doScope(er, new State(((ContainsScope)o).innerScope(), fcd.intro.name+"_"+(cs++), s.kind), b.nested);
				}
			} else if (o instanceof FunctionIntro) {
				FunctionIntro fi = (FunctionIntro) o;
				Object[] arr = doCompoundFunction(er, b, fi);
				if (arr == null)
					continue;
				Block lastBlock = (Block) arr[1];
				FunctionCaseDefn fcd = new FunctionCaseDefn(ret, fi.name, fi.args, arr[0]);
				fndefns.add(fcd);
				if (lastFn != null && !fcd.intro.name.equals(lastFn)) {
					lastFn = fcd.intro.name;
					cs = 0;
				}
				if (!lastBlock.nested.isEmpty()) {
					doScope(er, new State(((ContainsScope)o).innerScope(), fcd.intro.name+"_"+(cs++), s.kind), lastBlock.nested);
				}
			} else if (o instanceof StructDefn) {
				StructDefn sd = (StructDefn)o;
				ret.define(State.simpleName(sd.typename), sd.typename, sd);
				doStructFields(er, sd, b.nested);
			} else if (o instanceof ContractDecl) {
				ContractDecl cd = (ContractDecl) o;
				if (ret.contains(cd.contractName))
					er.message(b, "duplicate definition for name " + cd.contractName);
				else
					ret.define(State.simpleName(cd.contractName), cd.contractName, cd);
				doContractMethods(er, cd, b.nested);
			} else if (o instanceof CardDefinition) {
				CardDefinition cd = (CardDefinition) o;
//				if (ret.contains(cd.name))
//					er.message(b, "duplicate definition for name " + cd.name);
//				else
//					ret.define(State.simpleName(cd.name), cd.name, cd);
				doCardDefinition(er, new State(cd.innerScope(), cd.name, HSIEForm.Type.CARD), cd, b.nested);
			} else
				throw new UtilException("Need to handle " + o.getClass());
		}
		if (er.hasErrors())
			return er;
		
		gatherFunctions(er, s, ret, fndefns);
		
		if (er.hasErrors())
			return er;
		return ret;
	}

	private Object[] doCompoundFunction(ErrorResult er, Block b, FunctionIntro fi) {
		// this is a nested-block case
		// there are two main options:
		//   the nested block is just the value starting with "="
		//   the nested block is an if/else statement
		FunctionClauseParser fcp = new FunctionClauseParser();
		List<FunctionClause> clauses = new ArrayList<FunctionClause>();
		Block lastBlock = null;
		for (Block bi : b.nested) {
			if (bi.isComment())
				continue;
			Object c = fcp.tryParsing(new Tokenizable(bi));
			if (c == null)
				er.message(bi, "not a valid clause");
			else if (c instanceof ErrorResult)
				er.merge((ErrorResult)c);
			else {
				clauses.add(0, (FunctionClause) c); // assemble in reverse order
				if (lastBlock != null)
					assertNoNonCommentNestedLines(er, lastBlock);
				lastBlock = bi;
			}
		}
		// avoid error cascades by only assembling if we successfully parsed everything
		if (er.hasErrors())
			return null;
		Object expr = null;
		if (clauses.size() == 0) {
			er.message(b, "function must have at least one clause");
			return null;
		}
		FunctionClause last = clauses.get(0);
		if (last.guard == null) {
			clauses.remove(0);
			expr = last.expr;
		}
		for (FunctionClause c : clauses)
			expr = new IfExpr(c.guard, c.expr, expr);
		return new Object[] { expr, lastBlock };
	}

	protected void gatherFunctions(ErrorResult er, State s, Scope ret, List<FunctionCaseDefn> fndefns) {
		ListMap<String, FunctionCaseDefn> groups = new ListMap<String, FunctionCaseDefn>();
		String cfn = null;
		int pnargs = 0;
		for (FunctionCaseDefn fcd : fndefns) {
			// group together all function defns for a given function
			String n = fcd.intro.name;
			if (cfn == null || !cfn.equals(n)) {
				cfn = n;
				pnargs = fcd.intro.args.size();
				if (groups.contains(cfn))
					er.message((Tokenizable)null, "split definition of function " + cfn);
				else if (ret.contains(cfn))
					er.message((Tokenizable)null, "duplicate definition of " + cfn);
			} else if (fcd.intro.args.size() != pnargs)
				er.message((Block)null, "inconsistent numbers of arguments in definitions of " + cfn);
			groups.add(cfn, fcd);
		}
		for (Entry<String, List<FunctionCaseDefn>> x : groups.entrySet()) {
			ret.define(State.simpleName(x.getKey()), x.getKey(), new FunctionDefinition(s.kind, x.getValue().get(0).intro, x.getValue()));
		}
	}

	private void doStructFields(ErrorResult er, StructDefn sd, List<Block> fields) {
		FieldParser fp = new FieldParser();
		for (Block b : fields) {
			if (b.isComment())
				continue;
			Tokenizable tkz = new Tokenizable(b);
			Object sf = fp.tryParsing(tkz);
			if (sf == null)
				er.message(tkz, "syntax error");
			else if (sf instanceof ErrorResult)
				er.merge((ErrorResult) sf);
			else
				sd.addField((StructField)sf);
			assertNoNonCommentNestedLines(er, b);
		}
	}

	private void doContractMethods(ErrorResult er, ContractDecl cd, List<Block> methods) {
		MethodParser mp = new MethodParser();
		for (Block b : methods) {
			if (b.isComment())
				continue;
			Tokenizable tkz = new Tokenizable(b);
			Object md = mp.tryParsing(tkz);
			if (md == null)
				er.message(tkz, "syntax error");
			else if (md instanceof ErrorResult)
				er.merge((ErrorResult) md);
			else
				cd.addMethod((ContractMethodDecl)md);
			assertNoNonCommentNestedLines(er, b);
		}
	}

	private void doCardDefinition(ErrorResult er, State s, CardDefinition cd, List<Block> components) {
		IntroParser ip = new IntroParser(s);
		List<FunctionCaseDefn> functions = new ArrayList<FunctionCaseDefn>();
		List<EventCaseDefn> events = new ArrayList<EventCaseDefn>();
		int cs = 0;
		int hs = 0;
		int ss = 0;
		List<TemplateThing> templates = new ArrayList<TemplateThing>();
		Set<LocatedToken> frTemplates = new TreeSet<LocatedToken>();
		for (Block b : components) {
			if (b.isComment())
				continue;
			Tokenizable tkz = new Tokenizable(b);
			Object o = ip.tryParsing(tkz);
			if (o == null) {
				o = new FunctionParser(s).tryParsing(new Tokenizable(b));
				if (o  == null) {
					er.message(tkz, "must have valid card component definition here");
					continue;
				}
			}
			if (o instanceof ErrorResult)
				er.merge((ErrorResult)o);
			else if (o instanceof String) {
				switch ((String)o) {
				case "state": {
					doCardState(er, s, cd, b.nested);
					break;
				}
				default: {
					throw new UtilException("Cannot handle " + o);
				}
				}
			} else if (o instanceof TemplateIntro) {
				TemplateIntro intro = (TemplateIntro) o;
				if (templates.isEmpty()) {
					if (intro.name != null) {
						er.message(b, "first template definition may not have a name");
						break;
					}
				} else {
					if (intro.name == null) {
						er.message(b, "template definition must have a name");
						break;
					}
					
					boolean err = false;
					for (TemplateThing t : templates)
						if (intro.name.equals(t.name)) {
							er.message(b, "duplicate template name " + intro.name);
							err = true;
						}
					if (err)
						break;
					boolean found = false;
					for (LocatedToken tok : frTemplates)
						if (tok.text.equals(intro.name))
							found = true;
					if (!found) {
						er.message(b, "template " + intro.name + " was defined before being used");
						break;
					}
				}
				List<TemplateLine> lines = new ArrayList<TemplateLine>();
				doCardTemplate(er, frTemplates, b.nested, lines, null);
				if (!er.hasErrors() && lines.size() > 1)
					er.message(b, "multiple lines must be contained in a div or list");
				else if (!er.hasErrors() && lines.size() == 0)
					er.message(b, "a template must have at least one line");
				else if (!er.hasErrors())
					templates.add(new TemplateThing(intro.name, intro.args, lines.get(0)));
			} else if (o instanceof ContractImplements) {
				cd.addContractImplementation((ContractImplements)o);
				doImplementation(s, er, (Implements)o, b.nested, "_C" + cs++);
			} else if (o instanceof ContractService) {
				cd.addContractService((ContractService)o);
				doImplementation(s, er, (Implements)o, b.nested, "_S" + ss++);
			} else if (o instanceof HandlerImplements) {
				cd.addHandlerImplementation((HandlerImplements)o);
				doImplementation(s, er, (Implements)o, b.nested, "_H" + hs++);
			} else if (o instanceof FunctionCaseDefn) {
				functions.add((FunctionCaseDefn) o);
			} else if (o instanceof FunctionIntro) {
				// TODO: this code has never been tested in anger
				// It was cut-and-paste from the Scope version
				// It may not quite "fit" here
				// In particular, "37" is a magic number, but better than cs++ which interfered with the local contract number
				FunctionIntro fi = (FunctionIntro) o;
				Object[] arr = doCompoundFunction(er, b, fi);
				if (arr == null)
					continue;
				Block lastBlock = (Block) arr[1];
				FunctionCaseDefn fcd = new FunctionCaseDefn(s.scope, fi.name, fi.args, arr[0]);
				functions.add(fcd);
				if (!lastBlock.nested.isEmpty()) {
					doScope(er, new State(((ContainsScope)o).innerScope(), fcd.intro.name+"_"+37, s.kind), lastBlock.nested);
				}
			} else if (o instanceof EventCaseDefn) {
				EventCaseDefn ecd = (EventCaseDefn) o;
				events.add(ecd);
				handleMessageMethods(er, ecd, b.nested);
			} else
				throw new UtilException("Cannot handle " + o.getClass());
		}
		gatherFunctions(er, s, cd.innerScope(), functions);
		defineEventMethods(er, s, cd, events);
		if (!templates.isEmpty())
			cd.template = new Template(cd.name, unroll(er, frTemplates, templates, new TreeMap<String, Object>()), cd.innerScope());
	}	

	private void doCardState(ErrorResult er, State s, CardDefinition cd, List<Block> nested) {
		if (cd.state != null)
			er.message((Block)null, "duplicate state definition in card");
		cd.state = new StateDefinition();
		FieldParser fp = new FieldParser();
		for (Block q : nested)
			if (!q.isComment()) {
				Object o = fp.tryParsing(new Tokenizable(q));
				if (o == null)
					er.message(q, "syntax error");
				else if (o instanceof ErrorResult)
					er.merge((ErrorResult) o);
				else if (o instanceof StructField)
					cd.state.addField((StructField)o);
				else
					er.message(q, "cannot handle " + o.getClass());
			}
	}

	private List<TemplateLine> doCardTemplate(ErrorResult er, Set<LocatedToken> frTemplates, List<Block> nested, List<TemplateLine> ret, List<EventHandler> handlers) {
		TemplateLineParser tlp = new TemplateLineParser();
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Object o = tlp.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult) o);
			else if (o instanceof TemplateLine) {
				TemplateLine tl = (TemplateLine)o;
				if (tl.isTemplate()) {
					TemplateReference tr = (TemplateReference) tl.contents.get(0);
					frTemplates.add(new LocatedToken(tr.location, tr.name));
				}
				ret.add(tl);
				if (!b.nested.isEmpty()) {
					if (tl.isDiv()) { 
						doCardTemplate(er, frTemplates, b.nested, tl.nested, tl.handlers);
					} else if (tl.isList()) {
						doCardTemplate(er, frTemplates, b.nested, tl.nested, tl.handlers);
					} else if (tl.isCases()) {
						doCases(er, frTemplates, b, (TemplateCases)tl.contents.get(0));
					} else {
						Block fb = null;
						for (Block b1 : b.nested) {
							if (!b1.isComment())
								fb = b1;
						}
						if (fb != null)
							er.message(fb, "this node cannot be nested here - parent does not support children");
					}
				}
			} else if (o instanceof EventHandler)
				handlers.add((EventHandler)o);
			else
				er.message(b, "not a valid template line");
		}
		return ret;
	}

	private void doCases(ErrorResult er, Set<LocatedToken> frTemplates, Block container, TemplateCases tc) {
		assertSomeNonCommentNestedLines(er, container);
		TemplateLineParser tlp = new TemplateLineParser();
		for (Block b : container.nested) {
			Object o = tlp.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult)o);
			else if (!(o instanceof TemplateLine) || !((TemplateLine)o).isOr())
				er.message(b, "constituents of template cases must be or items");
			else {
				TemplateOr tor = (TemplateOr)((TemplateLine)o).contents.get(0);
				tc.addCase(tor);
				doCardTemplate(er, frTemplates, b.nested, tor.template, null);
			}
		}
	}

	private TemplateLine unroll(ErrorResult er, Set<LocatedToken> frTemplates, List<TemplateThing> templates, Map<String, Object> subst) {
		Map<String, TemplateThing> map = new TreeMap<String, TemplateThing>();
		TemplateThing ret = templates.get(0);
		for (TemplateThing t : templates) {
			if (t.name == null)
				continue;
			map.put(t.name, t);
		}
		for (LocatedToken s : frTemplates)
			if (!map.containsKey(s.text))
				er.message(s.location, "reference to non-existent template " + s.text);
		
		TemplateThing main = ret;
		return unroll(er, map, main.lines, subst);
	}

	private TemplateLine unroll(ErrorResult er, Map<String, TemplateThing> map, TemplateLine line, Map<String, Object> subst) {
		if (line.isTemplate()) {
			TemplateReference tr = (TemplateReference) line.contents.get(0);
			TemplateThing reffed = map.get(tr.name);
			if (tr.args.size() != reffed.args.size()) {
				er.message(tr.location, "incorrect number of actual parameters to " + tr.name + ": expected " + reffed.args.size());
				return null;
			}
			Map<String, Object> nsubst = new TreeMap<String, Object>(subst);
			for (int i=0;i<tr.args.size();i++) {
				String key = reffed.args.get(i).text;
				if (nsubst.containsKey(key)) {
					er.message(tr.location, "duplicate binding to formal parameter " + key);
					return null;
				}
				nsubst.put(key, tr.args.get(i));
			}
			return unroll(er, map, reffed.lines, nsubst);
		} else {
			// substitute for vars in contents, attrs and formats
			List<Object> contents = new ArrayList<Object>();
			for (Object o : line.contents)
				contents.add(substituteMacroParameters(er, map, o, subst));
			List<Object> attrs = new ArrayList<Object>();
			for (Object o : line.attrs)
				attrs.add(substituteMacroParameters(er, map, o, subst));
			List<Object> formats = new ArrayList<Object>();
			for (Object o : line.formats)
				formats.add(substituteMacroParameters(er, map, o, subst));
			TemplateLine ret = new TemplateLine(contents, line.customTag, line.customTagVar, attrs, formats);
			for (TemplateLine x : line.nested)
				ret.nested.add(unroll(er, map, x, subst));
			for (EventHandler y : line.handlers)
				ret.handlers.add(new EventHandler(y.action, substituteMacroParameters(er, map, y.expr, subst)));
			return ret;
		}
	}

	private Object substituteMacroParameters(ErrorResult er, Map<String, TemplateThing> map, Object o, Map<String, Object> subst) {
		if (o == null)
			return null;
		else if (o instanceof StringLiteral || o instanceof NumericLiteral)
			return o;
		else if (o instanceof TemplateToken) {
			TemplateToken tt = (TemplateToken) o;
			if (tt.type == TemplateToken.IDENTIFIER && subst.containsKey(tt.text))
				return asTT(subst.get(tt.text));
		} else if (o instanceof TemplateExplicitAttr) {
			TemplateExplicitAttr tea = (TemplateExplicitAttr) o;
			if (tea.type == TemplateToken.IDENTIFIER && subst.containsKey(tea.value)) {
				TemplateToken tt = asTT(subst.get(tea.value));
				return new TemplateExplicitAttr(tea.attr, tt.type, tt.text);
			} else if (tea.type == TemplateToken.STRING) {
				return tea;
			} else
				throw new UtilException("Cannot handle: " + tea);
		} else if (o instanceof UnresolvedVar) {
			String s = ((UnresolvedVar)o).var;
			if (subst.containsKey(s))
				return subst.get(s);
			return o;
		} else if (o instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) o;
			List<Object> args = new ArrayList<Object>();
			for (Object o2 : ae.args)
				args.add(substituteMacroParameters(er, map, o2, subst));
			return new ApplyExpr(substituteMacroParameters(er, map, ae.fn, subst), args);
		} else if (o instanceof CardReference) {
			// We don't have any parameters in this yet that could be macro parameters
		} else if (o instanceof TemplateCases) {
			TemplateCases tc = (TemplateCases)o;
			TemplateCases ret = new TemplateCases(tc.loc, substituteMacroParameters(er, map, tc.switchOn, subst));
			for (TemplateOr x : tc.cases)
				ret.addCase((TemplateOr) substituteMacroParameters(er, map, x, subst));
			return ret;
		} else if (o instanceof TemplateOr) {
			TemplateOr tor = (TemplateOr) o;
			TemplateOr ret = new TemplateOr(substituteMacroParameters(er, map, tor.cond, subst));
			for (TemplateLine x : tor.template)
				ret.template.add(unroll(er, map, x, subst));
			return ret;
		} else
			System.out.println("subMacroParms cannot handle: " + o + " "  + o.getClass());
			
		return o;
	}

	private TemplateToken asTT(Object sub) {
		if (sub instanceof StringLiteral) {
			StringLiteral s = (StringLiteral) sub;
			return new TemplateToken(null, TemplateToken.STRING, s.text);
		}
		throw new UtilException("Cannot handle: " + sub + ": " + sub.getClass());
	}

	private void doImplementation(State s, ErrorResult er, Implements impl, List<Block> nested, String clz) {
		FunctionParser fp = new FunctionParser(new State(s.scope, s.withPkg(clz), s.kind));
		List<MethodCaseDefn> cases = new ArrayList<MethodCaseDefn>();
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Object o = fp.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult) o);
			else if (o instanceof FunctionIntro) {
				FunctionIntro meth = (FunctionIntro)o;
				MethodCaseDefn mcd = new MethodCaseDefn(meth);
				cases.add(mcd);
				handleMessageMethods(er, mcd, b.nested);
			} else
				er.message(b, "cannot handle " + o.getClass());
		}

		ListMap<String, MethodCaseDefn> groups = new ListMap<String, MethodCaseDefn>();
		String cfn = null;
		int pnargs = 0;
		for (MethodCaseDefn fcd : cases) {
			String n = fcd.intro.name;
			if (cfn == null || !cfn.equals(n)) {
				cfn = n;
				pnargs = fcd.intro.args.size();
				if (groups.contains(cfn))
					er.message((Tokenizable)null, "split definition of function " + cfn);
			} else if (fcd.intro.args.size() != pnargs)
				er.message((Tokenizable)null, "inconsistent numbers of arguments in definitions of " + cfn);
			groups.add(cfn, fcd);
		}
		for (Entry<String, List<MethodCaseDefn>> x : groups.entrySet()) {
			impl.addMethod(new MethodDefinition(x.getValue().get(0).intro, x.getValue()));
		}
	}

	private void defineEventMethods(ErrorResult er, State s, CardDefinition cd, List<EventCaseDefn> events) {
		ListMap<String, EventCaseDefn> groups = new ListMap<String, EventCaseDefn>();
		String cfn = null;
		int pnargs = 0;
		for (EventCaseDefn ecd : events) {
			String n = ecd.intro.name;
			if (cfn == null || !cfn.equals(n)) {
				cfn = n;
				pnargs = ecd.intro.args.size();
				if (groups.contains(cfn))
					er.message((Tokenizable)null, "split definition of function " + cfn);
			} else if (ecd.intro.args.size() != pnargs)
				er.message((Tokenizable)null, "inconsistent numbers of arguments in definitions of " + cfn);
			groups.add(cfn, ecd);
		}
		for (Entry<String, List<EventCaseDefn>> x : groups.entrySet()) {
			cd.innerScope().define(State.simpleName(x.getKey()), x.getKey(), new EventHandlerDefinition(x.getValue().get(0).intro, x.getValue()));
		}
	}


	private void handleMessageMethods(ErrorResult er, MessagesHandler mcd, List<Block> nested) {
		MethodMessageParser mm = new MethodMessageParser();
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Object o = mm.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult)o);
			else if (o instanceof MethodMessage)
				mcd.addMessage((MethodMessage) o);
			else
				throw new UtilException("What is " + o + "?");
		}
	}

	private void assertNoNonCommentNestedLines(ErrorResult er, Block b) {
		for (Block q : b.nested)
			if (!q.isComment())
				er.message(q, "nested declarations prohibited");
	}

	private void assertSomeNonCommentNestedLines(ErrorResult er, Block b) {
		for (Block q : b.nested)
			if (!q.isComment())
				return;
		er.message(b, "nested declarations required");
	}
}
