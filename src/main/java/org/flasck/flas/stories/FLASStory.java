package org.flasck.flas.stories;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.IfExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.PlatformSpec;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.template.TemplateIntro;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.FLASError;
import org.flasck.flas.errors.ScopeDefineException;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContainsScope;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.D3Intro;
import org.flasck.flas.parsedForm.D3PatternBlock;
import org.flasck.flas.parsedForm.D3Section;
import org.flasck.flas.parsedForm.D3Thing;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionClause;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.MessagesHandler;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMember;
import org.flasck.flas.parsedForm.PropertyDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateCardReference;
import org.flasck.flas.parsedForm.TemplateCases;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateFormatEvents;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TemplateOr;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
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
import org.flasck.flas.parser.TupleDeclarationParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.exceptions.UtilException;

public class FLASStory {
	public static class State {
		public final PackageName pkgName;
		public final Scope scope;
		public final HSIEForm.CodeType kind;

		public State(Scope scope, String pkg, HSIEForm.CodeType kind) {
			this.scope = scope;
			this.pkgName = new PackageName(pkg);
			this.kind = kind;
		}

		public String withPkg(String name) {
			if (pkgName == null || pkgName.simpleName() == null || pkgName.simpleName().length() == 0)
				return name;
			return pkgName.simpleName() +"." + name;
		}

		public static String simpleName(String key) {
			int idx = key.lastIndexOf(".");
			return key.substring(idx+1);
		}
	}

	public Object process(Scope top, List<Block> blocks) {
		ErrorResult er = new ErrorResult();
		process("ME", top, er, blocks, false);
		if (er.hasErrors())
			return er;
		return top;
	}

	public void process(String pkg, Scope sc, ErrorResult er, List<Block> blocks, boolean optimism) {
		State s = new State(sc, pkg, HSIEForm.CodeType.FUNCTION);
		doScope(er, s, blocks);
	}

	private Object doScope(ErrorResult er, State s, List<Block> blocks) {
		if (blocks.isEmpty())
			return null;

		Scope ret = s.scope;
		for (Block b : blocks) {
			if (b.isComment())
				continue;
			
			Object o = new MultiParser(s, IntroParser.class, FunctionParser.class, TupleDeclarationParser.class).parse(b);
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
					String caseName = ret.caseName(fcd.intro.name);
					fcd.provideCaseName(caseName);
					ret.define(State.simpleName(fcd.functionName()), fcd.functionName(), fcd);
					if (!b.nested.isEmpty()) {
						doScope(er, new State(fcd.innerScope(), caseName, s.kind), b.nested);
					}
				} else if (o instanceof FunctionIntro) {
					FunctionIntro fi = (FunctionIntro) o;
					String caseName = ret.caseName(fi.name);
					Object[] arr = doCompoundFunction(er, b, fi);
					if (arr == null)
						continue;
					Block lastBlock = (Block) arr[1];
					FunctionCaseDefn fcd = new FunctionCaseDefn(fi.location, s.kind, fi.name, fi.args, arr[0]);
					fcd.provideCaseName(caseName);
					ret.define(State.simpleName(fcd.functionName()), fcd.functionName(), fcd);
					if (!lastBlock.nested.isEmpty()) {
						doScope(er, new State(fcd.innerScope(), caseName, s.kind), lastBlock.nested);
					}
				} else if (o instanceof MethodCaseDefn) {
					MethodCaseDefn mcd = (MethodCaseDefn)o;
					String caseName = ret.caseName(mcd.methodName());
					mcd.provideCaseName(caseName);
					ret.define(State.simpleName(mcd.methodName()), mcd.methodName(), mcd);
					addMethodMessages(er, mcd.messages, b.nested);
				} else if (o instanceof TupleAssignment) {
					TupleAssignment ta = (TupleAssignment) o;
					int k=0;
					for (LocatedName x : ta.vars) {
						ret.define(x.text, s.withPkg(x.text), new TupleMember(x.location, ta, k++));
					}
					// I don't think we need to do anything here, but if recursion is called for, we probably have a scope
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
					ret.define(State.simpleName(hi.hiName), hi.hiName, hi);
					doImplementation(s, er, hi, b.nested, State.simpleName(hi.hiName));
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
			expr = new IfExpr((Locatable) c.guard, c.expr, expr);
		return new Object[] { expr, lastBlock };
	}

	public void addMethodMessages(ErrorResult er, List<MethodMessage> messages, List<Block> nested) {
		MethodMessageParser mmp = new MethodMessageParser();
		for (Block b : nested) {
			assertNoNonCommentNestedLines(er, b);
			Object ibo = mmp.tryParsing(new Tokenizable(b.line));
			if (ibo == null)
				er.message(b, "expected method message");
			else if (ibo instanceof ErrorResult)
				er.merge((ErrorResult) ibo);
			else if (!(ibo instanceof MethodMessage))
				er.message(b, "expected method message");
			else
				messages.add((MethodMessage) ibo);
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
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Tokenizable tkz = new Tokenizable(b);
			InputPosition posn = tkz.realinfo();
			Object om = omp.tryParsing(tkz);
			if (om instanceof ErrorResult)
				er.merge((ErrorResult) om);
			else if ("state".equals(om))
				doObjectState(er, s, posn, sd, b.nested);
			else if (om instanceof ObjectMember) {
				ObjectMember omm = (ObjectMember) om;
				switch (omm.type) {
				case ObjectMember.CTOR: {
					if (omm.what instanceof FunctionIntro)
						throw new UtilException("Should work, but not implemented: see other FunctionIntro cases in FLASStory and doCompoundFunction, but I think everything is broken");
					else if (omm.what instanceof FunctionCaseDefn) {
						FunctionCaseDefn fcd = (FunctionCaseDefn) omm.what;
						String caseName = sd.innerScope().caseName(fcd.intro.name);
						fcd.provideCaseName(caseName);
						sd.innerScope().define(State.simpleName(fcd.functionName()), fcd.functionName(), fcd);
						if (!b.nested.isEmpty()) {
							doScope(er, new State(fcd.innerScope(), caseName, s.kind), b.nested);
						}
					}
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
		Scope inner = cd.innerScope();
		int cs = 0;
		int ss = 0;
		Set<LocatedToken> frTemplates = new TreeSet<LocatedToken>();
		for (Block b : components) {
			if (b.isComment())
				continue;
			Tokenizable tkz = new Tokenizable(b);
			InputPosition posn = tkz.realinfo();
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
					doCardState(er, s, posn.copySetEnd(tkz.at()), cd, b.nested);
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
				if (cd.templates.isEmpty()) {
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
					for (Template t : cd.templates)
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
					cd.templates.add(new Template(intro.kw, intro.location, intro.name, intro.args, t));
			} else if (o instanceof D3Intro) {
				D3Intro d3 = (D3Intro) o;
				List<D3PatternBlock> lines = new ArrayList<D3PatternBlock>();
				assertSomeNonCommentNestedLines(er, b);
				doD3Pattern(er, b.nested, lines);
				if (!er.hasErrors())
					cd.d3s.add(new D3Thing(d3, lines));
			} else if (o instanceof ContractImplements) {
				cd.addContractImplementation((ContractImplements)o);
				doImplementation(s, er, (Implements)o, b.nested, "_C" + cs++);
			} else if (o instanceof ContractService) {
				cd.addContractService((ContractService)o);
				doImplementation(s, er, (Implements)o, b.nested, "_S" + ss++);
			} else if (o instanceof HandlerImplements) {
				HandlerImplements hi = (HandlerImplements)o;
				cd.addHandlerImplementation(hi);
				doImplementation(s, er, hi, b.nested, State.simpleName(hi.hiName));
			} else if (o instanceof FunctionCaseDefn) {
				FunctionCaseDefn fcd = (FunctionCaseDefn) o;
				inner.define(State.simpleName(fcd.functionName()), fcd.functionName(), fcd);
				String caseName = inner.caseName(fcd.intro.name);
				fcd.provideCaseName(caseName);
			} else if (o instanceof FunctionIntro) {
				// TODO: this code has never been tested in anger
				// It was cut-and-paste from the Scope version
				// It may not quite "fit" here
				FunctionIntro fi = (FunctionIntro) o;
				Object[] arr = doCompoundFunction(er, b, fi);
				if (arr == null)
					continue;
				Block lastBlock = (Block) arr[1];
				FunctionCaseDefn fcd = new FunctionCaseDefn(fi.location, s.kind, fi.name, fi.args, arr[0]);
				String caseName = inner.caseName(fcd.intro.name);
				fcd.provideCaseName(caseName);
				inner.define(State.simpleName(fcd.functionName()), fcd.functionName(), fcd);
				if (!lastBlock.nested.isEmpty()) {
					Scope is = ((ContainsScope)o).innerScope();
					doScope(er, new State(is, fcd.caseName(), s.kind), lastBlock.nested);
				}
			} else if (o instanceof MethodCaseDefn) {
				MethodCaseDefn mcd = (MethodCaseDefn) o;
				inner.define(State.simpleName(mcd.methodName()), mcd.methodName(), mcd);
				addMethodMessages(er, mcd.messages, b.nested);
			} else if (o instanceof EventCaseDefn) {
				EventCaseDefn ecd = (EventCaseDefn) o;
				inner.define(State.simpleName(ecd.methodName()), ecd.methodName(), ecd);
				String caseName = inner.caseName(ecd.methodName());
				ecd.provideCaseName(caseName);
				addMethodMessages(er, ecd.messages, b.nested);
			} else if (o instanceof ContractDecl) {
				er.message(((ContractDecl)o).location(), "cannot embed contract declarations in a card");
			} else
				throw new UtilException("Cannot handle " + o.getClass());
		}
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

	private void doCardState(ErrorResult er, State s, InputPosition kwp, CardDefinition cd, List<Block> nested) {
		if (cd.state != null)
			er.message((Block)null, "duplicate state definition in card");
		StateDefinition os = cd.state = new StateDefinition(kwp);
		doState(er, new FieldParser(FieldParser.CARD), os, nested);
	}

	private void doObjectState(ErrorResult er, State s, InputPosition kwp, ObjectDefn od, List<Block> nested) {
		if (od.state != null)
			er.message((Block)null, "duplicate state definition in card");
		StateDefinition os = od.state = new StateDefinition(kwp);
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
		} else if (tl instanceof TemplateCardReference) {
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
				asList.template = new TemplateDiv(td.kw, td.customTagLoc, "li", td.customTagVarLoc, null, td.attrs, td.formats);
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
	
	private void doD3Section(ErrorResult er, List<Block> nested, List<D3Section> sections) {
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
				for (D3Section os : sections) {
					if (os.name.equals(s.name)) {
						er.message(b, "cannot have duplicate sections of name " + s.name);
						break;
					}
				}
				sections.add(s);
				if (s.name.equals("enter"))
					doD3Methods(er, b.nested, s.actions);
				else if (s.name.equals("layout"))
					doD3Layout(er, b.nested, s.properties);
				else
					er.message(b, s.name + " is not a valid d3 section name");
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

	private void doD3Layout(ErrorResult er, List<Block> nested, List<PropertyDefn> properties) {
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
				for (PropertyDefn other : properties)
					if (other.name.equals(prop.name)) {
						er.message(b, "cannot specify property " + prop.name +" more than once");
						break;
					}
				properties.add(prop);
			}
		}
	}

	private void doImplementation(State s, ErrorResult er, Implements impl, List<Block> nested, String clz) {
		FunctionParser fp = new FunctionParser(new State(s.scope, s.withPkg(clz), s.kind));
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Object o = fp.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult) o);
			else if (o instanceof FunctionIntro) {
				MethodCaseDefn mcd = new MethodCaseDefn((FunctionIntro)o);
				mcd.provideCaseName(mcd.methodName());
				impl.addMethod(mcd);
				handleMessageMethods(er, mcd, b.nested);
			} else
				er.message(b, "cannot handle " + o.getClass());
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
