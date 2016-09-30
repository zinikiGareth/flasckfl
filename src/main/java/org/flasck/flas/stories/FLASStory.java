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
import org.flasck.flas.errors.FLASError;
import org.flasck.flas.errors.ScopeDefineException;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CardReference;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.D3Intro;
import org.flasck.flas.parsedForm.D3Invoke;
import org.flasck.flas.parsedForm.D3PatternBlock;
import org.flasck.flas.parsedForm.D3Section;
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
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMember;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.PlatformSpec;
import org.flasck.flas.parsedForm.PropertyDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateCases;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateFormat;
import org.flasck.flas.parsedForm.TemplateFormatEvents;
import org.flasck.flas.parsedForm.TemplateIntro;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TemplateOr;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.D3PatternLineParser;
import org.flasck.flas.parser.D3SectionLineParser;
import org.flasck.flas.parser.FieldParser;
import org.flasck.flas.parser.FunctionClauseParser;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.parser.IntroParser;
import org.flasck.flas.parser.MethodMessageParser;
import org.flasck.flas.parser.MethodParser;
import org.flasck.flas.parser.ObjectMemberParser;
import org.flasck.flas.parser.PlatformAndroidSpecParser;
import org.flasck.flas.parser.PropertyParser;
import org.flasck.flas.parser.TemplateLineParser;
import org.flasck.flas.parser.TryParsing;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;

public class FLASStory implements StoryProcessor {
	public static class FCDWrapper {
		List<Block> nested; 
		FunctionCaseDefn starter;

		public FCDWrapper(List<Block> nested, FunctionCaseDefn starter) {
			this.nested = nested;
			this.starter = starter;
		}
	}
	
	public static class MCDWrapper {
		List<Block> nested; 
		MethodCaseDefn starter;

		public MCDWrapper(List<Block> nested, MethodCaseDefn starter) {
			this.nested = nested;
			this.starter = starter;
		}
	}
	
	public static class ECDWrapper {
		List<Block> nested; 
		EventCaseDefn starter;

		public ECDWrapper(List<Block> nested, EventCaseDefn starter) {
			this.nested = nested;
			this.starter = starter;
		}
	}
	
	public static class State {
		private String pkg;
		public final Scope scope;
		public final HSIEForm.CodeType kind;

		public State(Scope scope, String pkg, HSIEForm.CodeType kind) {
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
		State s = new State(pkg.innerScope(), pkg.name, HSIEForm.CodeType.FUNCTION);
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
		List<MCDWrapper> methods = new ArrayList<MCDWrapper>();
		List<FCDWrapper> fndefns = new ArrayList<FCDWrapper>();
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
			try {
				if (o instanceof FunctionCaseDefn) {
					FunctionCaseDefn fcd = (FunctionCaseDefn)o;
					fndefns.add(new FCDWrapper(b.nested, fcd));
				} else if (o instanceof FunctionIntro) {
					FunctionIntro fi = (FunctionIntro) o;
					Object[] arr = doCompoundFunction(er, b, fi);
					if (arr == null)
						continue;
					Block lastBlock = (Block) arr[1];
					FunctionCaseDefn fcd = new FunctionCaseDefn(fi.location, fi.name, fi.args, arr[0]);
					fndefns.add(new FCDWrapper(lastBlock.nested, fcd));
				} else if (o instanceof MethodCaseDefn) {
					methods.add(new MCDWrapper(b.nested, (MethodCaseDefn) o));
				} else if (o instanceof StructDefn) {
					StructDefn sd = (StructDefn)o;
					ret.define(State.simpleName(sd.name()), sd.name(), sd);
					doStructFields(er, sd, b.nested);
				} else if (o instanceof ObjectDefn) {
					ObjectDefn od = (ObjectDefn)o;
					doObjectMembers(er, s, od, b.nested);
				} else if (o instanceof ContractDecl) {
					ContractDecl cd = (ContractDecl) o;
					if (ret.contains(cd.name()))
						er.message(b, "duplicate definition for name " + cd.name());
					else
						ret.define(State.simpleName(cd.name()), cd.name(), cd);
					doContractMethods(er, cd, b.nested);
				} else if (o instanceof CardDefinition) {
					CardDefinition cd = (CardDefinition) o;
					doCardDefinition(er, new State(cd.innerScope(), cd.name, HSIEForm.CodeType.CARD), cd, b.nested);
				} else if (o instanceof HandlerImplements) {
					HandlerImplements hi = (HandlerImplements)o;
					ScopeEntry se = ret.define(State.simpleName(hi.hiName), hi.hiName, hi);
					doImplementation(s, er, se, hi, b.nested, State.simpleName(hi.hiName));
				} else if (o instanceof ContractImplements) {
					er.message(((ContractImplements) o).location(), "implements cannot appear at the top level");
				} else
					throw new UtilException("Need to handle " + o.getClass());
			} catch (ScopeDefineException ex) {
				er.message(new FLASError(b.line.locationAtText(0), ex.getMessage()));
			}
		}
		if (er.hasErrors())
			return er;
		
		gatherFunctions(er, s, ret, fndefns);
		gatherStandaloneMethods(er, s, ret, methods);
		
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

	protected void gatherFunctions(ErrorResult er, State s, Scope ret, List<FCDWrapper> fndefns) {
		ListMap<String, FCDWrapper> groups = new ListMap<String, FCDWrapper>();
		String cfn = null;
		int pnargs = 0;
		for (FCDWrapper w : fndefns) {
			FunctionCaseDefn fcd = w.starter;
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
			groups.add(cfn, w);
		}
		for (Entry<String, List<FCDWrapper>> x : groups.entrySet()) {
			FunctionDefinition fd = new FunctionDefinition(x.getValue().get(0).starter.intro.location, s.kind, x.getValue().get(0).starter.intro, new ArrayList<FunctionCaseDefn>());
			ScopeEntry me = ret.define(State.simpleName(x.getKey()), x.getKey(), fd);
			int cs = 0;
			for (FCDWrapper q : x.getValue()) {
				FunctionCaseDefn fcd = new FunctionCaseDefn(me, q.starter);
				fd.cases.add(fcd);
				if (!q.nested.isEmpty())
					doScope(er, new State(fcd.innerScope(), fcd.intro.name+"_"+cs, s.kind), q.nested);
				cs++;
			}
		}
	}

	protected void gatherStandaloneMethods(ErrorResult er, State s, Scope ret, List<MCDWrapper> methods) {
		ListMap<String, MCDWrapper> groups = new ListMap<String, MCDWrapper>();
		String cfn = null;
		int pnargs = 0;

		for (MCDWrapper w : methods) {
			// group together all function defns for a given function
			MethodCaseDefn mcd = w.starter;
			String n = mcd.intro.name;
			if (cfn == null || !cfn.equals(n)) {
				cfn = n;
				pnargs = mcd.intro.args.size();
				if (groups.contains(cfn))
					er.message((Tokenizable)null, "split definition of function " + cfn);
				else if (ret.contains(cfn))
					er.message((Tokenizable)null, "duplicate definition of " + cfn);
			} else if (mcd.intro.args.size() != pnargs)
				er.message((Block)null, "inconsistent numbers of arguments in definitions of " + cfn);
			groups.add(cfn, w);
		}
		MethodMessageParser mmp = new MethodMessageParser();
		for (Entry<String, List<MCDWrapper>> x : groups.entrySet()) {
			MethodDefinition md = new MethodDefinition(x.getValue().get(0).starter.intro, new ArrayList<MethodCaseDefn>());
			ScopeEntry me = ret.define(State.simpleName(x.getKey()), x.getKey(), md);
			for (MCDWrapper q : x.getValue()) {
				MethodCaseDefn mcd = new MethodCaseDefn(me, q.starter);
				md.cases.add(mcd);
				for (Block b : q.nested) {
					assertNoNonCommentNestedLines(er, b);
					Object ibo = mmp.tryParsing(new Tokenizable(b.line));
					if (ibo == null)
						er.message(b, "expected method message");
					else if (ibo instanceof ErrorResult)
						er.merge((ErrorResult) ibo);
					else if (!(ibo instanceof MethodMessage))
						er.message(b, "expected method message");
					else
						mcd.messages.add((MethodMessage) ibo);
				}
			}
		}
	}

	private void doStructFields(ErrorResult er, StructDefn sd, List<Block> fields) {
		FieldParser fp = new FieldParser(FieldParser.CARD);
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

	private void doObjectMembers(ErrorResult er, State s, ObjectDefn sd, List<Block> nested) {
		ObjectMemberParser omp = new ObjectMemberParser(s);
		FunctionParser fp = new FunctionParser(s);
		List<FCDWrapper> ctors = new ArrayList<FCDWrapper>();
		List<MCDWrapper> methods = new ArrayList<MCDWrapper>();
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Tokenizable tkz = new Tokenizable(b);
			Object om = omp.tryParsing(tkz);
			if (om instanceof ErrorResult)
				er.merge((ErrorResult) om);
			else if ("state".equals(om))
				doObjectState(er, s, sd, b.nested);
			else if (om instanceof ObjectMember) {
				ObjectMember omm = (ObjectMember) om;
				switch (omm.type) {
				case ObjectMember.CTOR: {
					if (omm.what instanceof FunctionIntro)
						throw new UtilException("Should work, but not implemented: see other FunctionIntro cases in FLASStory and doCompoundFunction, but I think everything is broken");
					else if (omm.what instanceof FunctionCaseDefn)
						ctors.add(new FCDWrapper(b.nested, (FunctionCaseDefn) omm.what));
					else
						er.message(b, "syntax error");
					break;
				}
//					else if (om instanceof MethodCaseDefn)
//						methods.add(new MCDWrapper(b.nested, (MethodCaseDefn) om));
				default: {
					er.message(b, "syntax error");
				}
				}
			}
			else {
				Object func = fp.tryParsing(tkz);
				if (func instanceof FunctionCaseDefn || func instanceof FunctionIntro)
					throw new UtilException("Nested internal functions; not handled yet");
				else
					er.message(b, "syntax error");
			}
		}
		gatherFunctions(er, s, sd.innerScope(), ctors);
		gatherStandaloneMethods(er, s, sd.innerScope(), methods);
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
		ScopeEntry se = cd.fnScope.outerEntry;
		IntroParser ip = new IntroParser(s);
		List<FCDWrapper> functions = new ArrayList<FCDWrapper>();
		List<MCDWrapper> methods = new ArrayList<MCDWrapper>();
		List<ECDWrapper> events = new ArrayList<ECDWrapper>();
		int cs = 0;
		int ss = 0;
		List<TemplateThing> templates = new ArrayList<TemplateThing>();
		List<D3Thing> d3s = new ArrayList<D3Thing>();
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
			} else if (o instanceof PlatformSpec) {
				PlatformSpec ps = (PlatformSpec) o;
				if (cd.platforms.containsKey(ps.spec)) {
					er.message(ps.location, "cannot have multiple platform definitions for '" + ps.spec + "'");
					break;
				}
				cd.platforms.put(ps.spec, ps);
				readPlatformSpec(er, b.nested, ps);
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
				TemplateLine t = doCardTemplate(er, frTemplates, b.nested);
				if (!er.hasErrors())
					templates.add(new TemplateThing(intro.name, intro.args, t));
			} else if (o instanceof D3Intro) {
				D3Intro d3 = (D3Intro) o;
				List<D3PatternBlock> lines = new ArrayList<D3PatternBlock>();
				assertSomeNonCommentNestedLines(er, b);
				doD3Pattern(er, b.nested, lines);
				if (!er.hasErrors())
					d3s.add(new D3Thing(cd.name, d3.name, d3.location, d3.expr, d3.var, lines));
			} else if (o instanceof ContractImplements) {
				cd.addContractImplementation((ContractImplements)o);
				doImplementation(s, er, se, (Implements)o, b.nested, "_C" + cs++);
			} else if (o instanceof ContractService) {
				cd.addContractService((ContractService)o);
				doImplementation(s, er, se, (Implements)o, b.nested, "_S" + ss++);
			} else if (o instanceof HandlerImplements) {
				HandlerImplements hi = (HandlerImplements)o;
				cd.addHandlerImplementation(hi);
				doImplementation(s, er, se, hi, b.nested, State.simpleName(hi.hiName));
			} else if (o instanceof FunctionCaseDefn) {
				functions.add(new FCDWrapper(b.nested, (FunctionCaseDefn) o));
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
				FunctionCaseDefn fcd = new FunctionCaseDefn(fi.location, fi.name, fi.args, arr[0]);
				functions.add(new FCDWrapper(lastBlock.nested, fcd));
//				if (!lastBlock.nested.isEmpty()) {
//					doScope(er, new State(((ContainsScope)o).innerScope(), fcd.intro.name+"_"+37, s.kind), lastBlock.nested);
//				}
			} else if (o instanceof MethodCaseDefn) {
				methods.add(new MCDWrapper(b.nested, (MethodCaseDefn) o));
			} else if (o instanceof EventCaseDefn) {
				events.add(new ECDWrapper(b.nested, (EventCaseDefn) o));
			} else if (o instanceof ContractDecl) {
				er.message(((ContractDecl)o).location(), "cannot embed contract declarations in a card");
			} else
				throw new UtilException("Cannot handle " + o.getClass());
		}
		gatherFunctions(er, s, cd.innerScope(), functions);
		gatherStandaloneMethods(er, s, cd.innerScope(), methods);
		defineEventMethods(er, s, cd, events);
//		if (!templates.isEmpty())
		if (er.hasErrors())
			return;
		cd.template = new Template(cd.name, unroll(er, s, frTemplates, templates, d3s, new TreeMap<String, Object>()), cd.innerScope());
	}

	private void readPlatformSpec(ErrorResult er, List<Block> nested, PlatformSpec ps) {
		TryParsing inner;
		if (ps.spec.equals("android"))
			inner = new PlatformAndroidSpecParser(ps);
		else {
			System.out.println("warning: ignoring unsupported platform '" + ps.spec + "'");
			return;
		}
		
		for (Block b : nested) {
			Object o = inner.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult) o);
			else
				ps.defns.add(o);
		}
	}

	private void doCardState(ErrorResult er, State s, CardDefinition cd, List<Block> nested) {
		if (cd.state != null)
			er.message((Block)null, "duplicate state definition in card");
		StateDefinition os = cd.state = new StateDefinition();
		doState(er, new FieldParser(FieldParser.CARD), os, nested);
	}

	private void doObjectState(ErrorResult er, State s, ObjectDefn od, List<Block> nested) {
		if (od.state != null)
			er.message((Block)null, "duplicate state definition in card");
		StateDefinition os = od.state = new StateDefinition();
		doState(er, new FieldParser(FieldParser.OBJECT), os, nested);
	}

	protected void doState(ErrorResult er, FieldParser fp, StateDefinition os, List<Block> nested) {
		for (Block q : nested)
			if (!q.isComment()) {
				Object o = fp.tryParsing(new Tokenizable(q));
				if (o == null)
					er.message(q, "syntax error");
				else if (o instanceof ErrorResult)
					er.merge((ErrorResult) o);
				else if (o instanceof StructField)
					os.addField((StructField)o);
				else
					er.message(q, "cannot handle " + o.getClass());
			}
	}

	private TemplateLine doCardTemplate(ErrorResult er, Set<LocatedToken> frTemplates, List<Block> nested) {
		TemplateLineParser tlp = new TemplateLineParser();
		TemplateLine ret = null;
		for (Block b : nested) {
			if (b.isComment())
				continue;
			if (ret != null) {
				er.message(b, "multiple lines must be contained in a div");
				return null;
			}
			Object o = tlp.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult) o);
			else if (o instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> os = (List<Object>) o;
				if (os.size() != 1) {
					er.message(b, "multiple content items must be contained in a div");
					return null;
				}
				ret = doOneLine(er, frTemplates, b, os.get(0));
			} else if (o instanceof TemplateLine) {
				ret = doOneLine(er, frTemplates, b, o);
			} else
				er.message(b, "not a valid template line");
		}
		return ret;
	}

	private TemplateLine doOneLine(ErrorResult er, Set<LocatedToken> frTemplates, Block b, Object o) {
		TemplateLine tl = (TemplateLine)o;
		if (tl instanceof ContentString || tl instanceof ContentExpr) {
			TemplateFormatEvents asEH = (TemplateFormatEvents)tl;
			TemplateLineParser tlp = new TemplateLineParser();
			for (Block ib : b.nested) {
				if (ib.isComment())
					continue;
				Object eh = tlp.tryParsing(new Tokenizable(ib));
				if (eh == null)
					er.message(b, "syntax error");
				else if (eh instanceof ErrorResult)
					er.merge((ErrorResult) eh);
				else if (eh instanceof EventHandler)
					asEH.handlers.add((EventHandler)eh);
				else
					er.message(ib, "not a valid template line");
			}
			return tl;
		}
		TemplateLine ret = null;
		if (tl instanceof TemplateReference) {
			TemplateReference tr = (TemplateReference) tl;
			frTemplates.add(new LocatedToken(tr.location, tr.name));
			return tl;
		} else if (tl instanceof CardReference) {
			return tl;
		} else if (tl instanceof TemplateList) {
			ret = tl;
			TemplateList asList = (TemplateList) ret;
			if (!hasNonCommentNestedLines(b)) {
				er.message(b, "list must have exactly one nested element");
				return null;
			}
			asList.template = doCardTemplate(er, frTemplates, b.nested);
			if (!(asList.template instanceof TemplateDiv)) {
				er.message(getNCNestedBlock(b, 0), "element inside list must be a div");
				return null;
			}
			TemplateDiv td = (TemplateDiv) asList.template;
			if (td.customTag == null && td.customTagVar == null && asList.customTag == null && asList.customTagVar == null) {
				asList.template = new TemplateDiv("li", null, td.attrs, td.formats);
				((TemplateDiv)asList.template).nested.addAll(td.nested);
				((TemplateDiv)asList.template).handlers.addAll(td.handlers);
			}
		} else if (tl instanceof TemplateDiv) { 
			ret = tl;
			TemplateDiv asDiv = (TemplateDiv) ret;
			doCardDiv(er, frTemplates, asDiv, b.nested);
		} else if (tl instanceof TemplateCases) {
			ret = tl;
			doCases(er, frTemplates, b, (TemplateCases)ret);
		} else
			throw new UtilException("Something should handle " + tl.getClass());
		return ret;
	}

	@SuppressWarnings("unchecked")
	private void doCardDiv(ErrorResult er, Set<LocatedToken> frTemplates, TemplateDiv asDiv, List<Block> nested) {
		TemplateLineParser tlp = new TemplateLineParser();
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Object o = tlp.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult) o);
			else if (o instanceof List) {
				for (Object o1 : ((List<Object>)o)) {
					TemplateLine item = doOneLine(er, frTemplates, b, o1);
					if (item != null)
						asDiv.nested.add(item);
				}
			} else if (o instanceof TemplateLine) {
				TemplateLine item = doOneLine(er, frTemplates, b, o);
				if (item != null)
					asDiv.nested.add(item);
			} else if (o instanceof EventHandler)
				asDiv.handlers.add((EventHandler)o);
			else
				er.message(b, "not a valid template line");
		}		
	}

	private void doCases(ErrorResult er, Set<LocatedToken> frTemplates, Block container, TemplateCases tc) {
		assertSomeNonCommentNestedLines(er, container);
		TemplateLineParser tlp = new TemplateLineParser();
		for (Block b : container.nested) {
			if (b.isComment())
				continue;
			Object o = tlp.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult)o);
			else if (!(o instanceof TemplateOr))
				er.message(b, "constituents of template cases must be OR items");
			else {
				TemplateLine it = doCardTemplate(er, frTemplates, b.nested);
				if (it != null) {
					TemplateOr tor = (TemplateOr)o;
					tc.addCase(new TemplateOr(tor.location(), tor.cond, it));
				}
			}
		}
	}

	private void doD3Pattern(ErrorResult er, List<Block> nested, List<D3PatternBlock> ret) {
		D3PatternLineParser d3lp = new D3PatternLineParser();
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Object o = d3lp.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult)o);
			else if (!(o instanceof D3PatternBlock))
				er.message(b, "constituents of D3 template must be valid d3 line");
			else {
				D3PatternBlock blk = (D3PatternBlock)o;
				ret.add(blk);
				doD3Section(er, b.nested, blk.sections);
			}
		}
	}
	
	private void doD3Section(ErrorResult er, List<Block> nested, Map<String, D3Section> sections) {
		D3SectionLineParser d3lp = new D3SectionLineParser();
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Object o = d3lp.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult)o);
			else if (!(o instanceof D3Section))
				er.message(b, "constituents of D3 template must be valid d3 line");
			else {
				D3Section s = (D3Section)o;
				if (sections.containsKey(s.name))
					er.message(b, "cannot have duplicate sections of name " + s.name);
				else {
					sections.put(s.name, s);
					if (s.name.equals("enter"))
						doD3Methods(er, b.nested, s.actions);
					else if (s.name.equals("layout"))
						doD3Layout(er, b.nested, s.properties);
					else
						throw new UtilException("Have not handled processing of section " + s.name);
				}
			}
		}
	}

	private void doD3Methods(ErrorResult er, List<Block> nested, List<MethodMessage> actions) {
		MethodMessageParser mmp = new MethodMessageParser();
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Object o = mmp.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult)o);
			else if (!(o instanceof MethodMessage))
				er.message(b, "constituents of D3 template must be valid d3 line");
			else {
				MethodMessage mm = (MethodMessage)o;
				actions.add(mm);
			}
		}
	}

	private void doD3Layout(ErrorResult er, List<Block> nested, Map<String, PropertyDefn> properties) {
		PropertyParser mmp = new PropertyParser();
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Object o = mmp.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult)o);
			else if (!(o instanceof PropertyDefn))
				er.message(b, "constituents of D3 template must be valid d3 line");
			else {
				PropertyDefn prop = (PropertyDefn)o;
				if (properties.containsKey(prop.name))
					er.message(b, "cannot specify property " + prop.name +" more than once");
				properties.put(prop.name, prop);
			}
		}
	}

	private TemplateLine unroll(ErrorResult er, State st, Set<LocatedToken> frTemplates, List<TemplateThing> templates, List<D3Thing> d3s, Map<String, Object> subst) {
		if (templates == null || templates.isEmpty())
			return null;
		Map<String, Object> map = new TreeMap<String, Object>();
		TemplateThing ret = templates.get(0);
		for (TemplateThing t : templates) {
			if (t.name == null)
				continue;
			map.put(t.name, t);
		}
		for (D3Thing t : d3s) {
			map.put(t.name, t);
		}
		for (LocatedToken s : frTemplates)
			if (!map.containsKey(s.text))
				er.message(s.location, "reference to non-existent template " + s.text);
		
		TemplateThing main = ret;
		return unroll(er, st, map, main.content, subst);
	}

	private TemplateLine unroll(ErrorResult er, State s, Map<String, Object> map, TemplateLine content, Map<String, Object> subst) {
		if (content == null)
			throw new UtilException("Null template line");
		if (content instanceof CardReference)
			return content;
		if (content instanceof TemplateReference) {
			TemplateReference tr = (TemplateReference) content;
			Object reffed = map.get(tr.name);
			if (reffed instanceof TemplateThing) {
				TemplateThing tt = (TemplateThing) reffed;
				if (tr.args.size() != tt.args.size()) {
					er.message(tr.location, "incorrect number of actual parameters to " + tr.name + ": expected " + tt.args.size());
					return null;
				}
				Map<String, Object> nsubst = new TreeMap<String, Object>(subst);
				for (int i=0;i<tr.args.size();i++) {
					String key = tt.args.get(i).text;
					if (nsubst.containsKey(key)) {
						er.message(tr.location, "duplicate binding to formal parameter " + key);
						return null;
					}
					nsubst.put(key, tr.args.get(i));
				}
				return unroll(er, s, map, tt.content, nsubst);
			} else {
				D3Thing d3 = (D3Thing) reffed;
				return new D3Invoke(s.scope, d3);
			}
		} else if (content instanceof TemplateFormat) {
			TemplateFormat tf = (TemplateFormat) content;
			List<Object> formats = new ArrayList<Object>();
			for (Object o : tf.formats)
				formats.add(substituteMacroParameters(er, s, map, o, subst));
			if (tf instanceof ContentString) {
				ContentString cs = (ContentString)tf;
				ContentString ret = new ContentString(((ContentString)tf).text, formats);
				for (EventHandler y : cs.handlers)
					ret.handlers.add(new EventHandler(y.action, substituteMacroParameters(er, s, map, y.expr, subst)));
				return ret;
			} else if (tf instanceof ContentExpr) {
				ContentExpr ce = (ContentExpr)tf;
				Object sub = substituteMacroParameters(er, s, map, ce.expr, subst);
				TemplateFormatEvents ret;
				if (sub instanceof StringLiteral)
					ret = new ContentString(((StringLiteral)sub).text, formats);
				else if (sub instanceof NumericLiteral)
					ret = new ContentString(((NumericLiteral)sub).text, formats);
				else
					ret = new ContentExpr(sub, ce.editable(), ce.rawHTML, formats);
				for (EventHandler y : ce.handlers)
					ret.handlers.add(new EventHandler(y.action, substituteMacroParameters(er, s, map, y.expr, subst)));
				return ret;
			} else if (tf instanceof TemplateDiv) {
				TemplateDiv td = (TemplateDiv) tf;
				List<Object> attrs = new ArrayList<Object>();
				for (Object o : td.attrs)
					attrs.add(substituteMacroParameters(er, s, map, o, subst));
				TemplateDiv ret = new TemplateDiv(td.customTag, td.customTagVar, attrs, formats);
				for (TemplateLine x : td.nested)
					ret.nested.add(unroll(er, s, map, x, subst));
				for (EventHandler y : td.handlers)
					ret.handlers.add(new EventHandler(y.action, substituteMacroParameters(er, s, map, y.expr, subst)));
				return ret;
			} else if (tf instanceof TemplateList) {
				TemplateList tl = (TemplateList) tf;
				TemplateList ret = new TemplateList(tl.listLoc, tl.listVar, tl.iterLoc, tl.iterVar, tl.customTag, tl.customTagVar, formats, false);
				ret.template = unroll(er, s, map, tl.template, subst);
				return ret;
//				return new TemplateList(tl.listLoc, substituteMacroParameters(er, map, tl.listVar, subst), tl.iterVar, formats);
			}
			else
				throw new UtilException("Not supported: " + tf.getClass());
			/*
			TemplateLine ret = new TemplateLine(contents, content.customTag, content.customTagVar, attrs, formats);
			for (EventHandler y : content.handlers)
				ret.handlers.add(new EventHandler(y.action, substituteMacroParameters(er, map, y.expr, subst)));
				*/
		} else if (content instanceof TemplateCases) {
			TemplateCases tc = (TemplateCases) content;
			TemplateCases ret = new TemplateCases(tc.loc, substituteMacroParameters(er, s, map, tc.switchOn, subst));
			for (TemplateOr i : tc.cases)
				ret.cases.add((TemplateOr) unroll(er, s, map, i, subst));
			return ret;
		} else if (content instanceof TemplateOr) {
			TemplateOr tc = (TemplateOr) content;
			return new TemplateOr(tc.location(), substituteMacroParameters(er, s, map, tc.cond, subst),  unroll(er, s, map, tc.template, subst));
		} else
			throw new UtilException("Not handled: " + content.getClass());
	}

	private Object substituteMacroParameters(ErrorResult er, State s, Map<String, Object> map, Object o, Map<String, Object> subst) {
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
			if (tea.type == TemplateToken.IDENTIFIER) { // any kind of expression
				return new TemplateExplicitAttr(tea.location, tea.attr, tea.type, substituteMacroParameters(er, s, map, tea.value, subst));
			} else if (tea.type == TemplateToken.STRING) {
				return tea;
			} else
				throw new UtilException("Cannot handle: " + tea);
		} else if (o instanceof UnresolvedVar) {
			String str = ((UnresolvedVar)o).var;
			if (subst.containsKey(str))
				return subst.get(str);
			return o;
		} else if (o instanceof UnresolvedOperator) {
			return o;
		} else if (o instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) o;
			List<Object> args = new ArrayList<Object>();
			for (Object o2 : ae.args)
				args.add(substituteMacroParameters(er, s, map, o2, subst));
			return new ApplyExpr(ae.location, substituteMacroParameters(er, s, map, ae.fn, subst), args);
		} else if (o instanceof CardReference) {
			// We don't have any parameters in this yet that could be macro parameters
		} else if (o instanceof TemplateCases) {
			TemplateCases tc = (TemplateCases)o;
			TemplateCases ret = new TemplateCases(tc.loc, substituteMacroParameters(er, s, map, tc.switchOn, subst));
			for (TemplateOr x : tc.cases)
				ret.addCase((TemplateOr) substituteMacroParameters(er, s, map, x, subst));
			return ret;
		} else if (o instanceof TemplateOr) {
			TemplateOr tor = (TemplateOr) o;
			TemplateOr ret = new TemplateOr(tor.location(), substituteMacroParameters(er, s, map, tor.cond, subst), unroll(er, s, map, tor.template, subst));
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

	private void doImplementation(State s, ErrorResult er, ScopeEntry se, Implements impl, List<Block> nested, String clz) {
		FunctionParser fp = new FunctionParser(new State(s.scope, s.withPkg(clz), s.kind));
		List<MCDWrapper> cases = new ArrayList<MCDWrapper>();
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
				cases.add(new MCDWrapper(b.nested, mcd));
			} else
				er.message(b, "cannot handle " + o.getClass());
		}

		ListMap<String, MCDWrapper> groups = new ListMap<String, MCDWrapper>();
		String cfn = null;
		int pnargs = 0;
		for (MCDWrapper q : cases) {
			MethodCaseDefn fcd = q.starter;
			String n = fcd.intro.name;
			if (cfn == null || !cfn.equals(n)) {
				cfn = n;
				pnargs = fcd.intro.args.size();
				if (groups.contains(cfn))
					er.message((Tokenizable)null, "split definition of function " + cfn);
			} else if (fcd.intro.args.size() != pnargs)
				er.message((Tokenizable)null, "inconsistent numbers of arguments in definitions of " + cfn);
			groups.add(cfn, q);
		}
		for (Entry<String, List<MCDWrapper>> x : groups.entrySet()) {
			MethodDefinition md = new MethodDefinition(x.getValue().get(0).starter.intro, new ArrayList<MethodCaseDefn>());
			impl.addMethod(md);
			for (MCDWrapper q : x.getValue()) {
				MethodCaseDefn mcd = new MethodCaseDefn(se, q.starter);
				md.cases.add(mcd);
				handleMessageMethods(er, mcd, q.nested);
			}
		}
	}

	private void defineEventMethods(ErrorResult er, State s, CardDefinition cd, List<ECDWrapper> events) {
		ListMap<String, ECDWrapper> groups = new ListMap<String, ECDWrapper>();
		String cfn = null;
		int pnargs = 0;
		for (ECDWrapper q : events) {
			EventCaseDefn ecd = q.starter;
			String n = ecd.intro.name;
			if (cfn == null || !cfn.equals(n)) {
				cfn = n;
				pnargs = ecd.intro.args.size();
				if (groups.contains(cfn))
					er.message((Tokenizable)null, "split definition of function " + cfn);
			} else if (ecd.intro.args.size() != pnargs)
				er.message((Tokenizable)null, "inconsistent numbers of arguments in definitions of " + cfn);
			groups.add(cfn, q);
		}
		for (Entry<String, List<ECDWrapper>> x : groups.entrySet()) {
			EventHandlerDefinition ehd = new EventHandlerDefinition(x.getValue().get(0).starter.intro, new ArrayList<EventCaseDefn>());
			ScopeEntry se = cd.innerScope().define(State.simpleName(x.getKey()), x.getKey(), ehd);
			for (ECDWrapper q : x.getValue()) {
				EventCaseDefn ecd = new EventCaseDefn(se, q.starter);
				ehd.cases.add(ecd);
				handleMessageMethods(er, ecd, q.nested);
			}
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

	private Block getNCNestedBlock(Block b, int i) {
		for (Block q : b.nested)
			if (!q.isComment() && i-- == 0)
				return q;
		return null;
	}

	private boolean hasNonCommentNestedLines(Block b) {
		for (Block q : b.nested)
			if (!q.isComment())
				return true;
		return false;
	}

	private void assertNoNonCommentNestedLines(ErrorResult er, Block b) {
		if (hasNonCommentNestedLines(b))
			er.message(b, "this line may not have nested declarations");
	}

	private void assertSomeNonCommentNestedLines(ErrorResult er, Block b) {
		if (!hasNonCommentNestedLines(b))
			er.message(b, "this line must have at least one nested declaration");
	}
}
