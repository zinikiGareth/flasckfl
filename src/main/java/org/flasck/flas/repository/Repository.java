package org.flasck.flas.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.DuplicateNameException;
import org.flasck.flas.compiler.StateNameException;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.assembly.Assembly;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.splitter.CardData;
import org.ziniki.splitter.ConcreteMetaData;
import org.ziniki.splitter.HTMLData;
import org.ziniki.splitter.NoMetaDataException;
import org.ziniki.splitter.SplitMetaData;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class Repository implements TopLevelDefinitionConsumer, RepositoryReader {
	private static final Logger logger = LoggerFactory.getLogger("Repository");
	final Map<String, RepositoryEntry> dict = new TreeMap<>();
	private final ConcreteMetaData webs = new ConcreteMetaData();
	private final Map<URI, List<String>> uriDefines = new TreeMap<>();
	private final Map<String, List<String>> flimDefines = new TreeMap<>();
	private List<String> currentDefines;
	
	public Repository() {
	}

	public void parsing(URI uri) {
		// Remove anything previously defined by compiling this file
		List<String> curr = uriDefines.get(uri);
		if (curr == null) {
			currentDefines = new ArrayList<>();
			uriDefines.put(uri, currentDefines);
		} else {
			while (!curr.isEmpty()) {
				dict.remove(curr.remove(0));
			}
			currentDefines = curr;
		}
		
		// Remove anything defined by FLIM
		String fn = uri.getPath();
		File f = new File(fn);
		String pkg = f.getParentFile().getName(); // the package name associated with a file
		curr = flimDefines.get(pkg);
		if (curr != null) {
			while (!curr.isEmpty()) {
				dict.remove(curr.remove(0));
			}
			flimDefines.remove(pkg);
		}
	}

	public void readingFLIM(String pkg) {
		List<String> curr = flimDefines.get(pkg);
		if (curr == null) {
			currentDefines = new ArrayList<>();
			flimDefines.put(pkg, currentDefines);
		} else {
			while (!curr.isEmpty()) {
				dict.remove(curr.remove(0));
			}
			currentDefines = curr;
		}
	}

	public void selectPackage(String pkg) {
		List<String> curr = flimDefines.get(pkg);
		if (curr == null) { // I don't really think this can happen
			currentDefines = new ArrayList<>();
			flimDefines.put(pkg, currentDefines);
		} else {
			currentDefines = curr;
		}
	}

	public void done() {
		this.currentDefines = null;
	}
	
	@Override
	public void functionDefn(ErrorReporter errors, FunctionDefinition func) {
		addEntry(errors, func.name(), func);
	}

	@Override
	public void tupleDefn(ErrorReporter errors, List<LocatedName> vars, FunctionName exprFnName, FunctionName pkgName, Expr expr) {
		TupleAssignment ta = new TupleAssignment(vars, exprFnName, pkgName, expr);
		NameOfThing pkg = pkgName.inContext;
		int k=0;
		for (LocatedName x : vars) {
			FunctionName tn = FunctionName.function(x.location, pkg, x.text);
			TupleMember tm = new TupleMember(x.location, ta, k++, tn);
			addEntry(errors, tn, tm);
			ta.addMember(tm);
		}
		try {
			addEntry(null, exprFnName, ta);
		} catch (DuplicateNameException | StateNameException ex) {
			// if this is thrown, it is because vars[0] is a duplicate
			// that (should) have already flagged an error above
		}
	}

	@Override
	public void argument(ErrorReporter errors, VarPattern parm) {
		addEntry(errors, parm.name(), parm);
	}

	@Override
	public void argument(ErrorReporter errors, TypedPattern parm) {
		addEntry(errors, parm.name(), parm);
	}

	@Override
	public void newHandler(ErrorReporter errors, HandlerImplements hi) {
		addEntry(errors, hi.handlerName, hi);
	}

	@Override
	public void newAgent(ErrorReporter errors, AgentDefinition decl) {
		addEntry(errors, decl.cardName(), decl);
	}

	@Override
	public void newCard(ErrorReporter errors, CardDefinition decl) {
		addEntry(errors, decl.cardName(), decl);
	}

	@Override
	public void newService(ErrorReporter errors, ServiceDefinition svc) {
		addEntry(errors, svc.cardName(), svc);
	}

	@Override
	public void newStandaloneMethod(ErrorReporter errors, StandaloneMethod meth) {
		addEntry(errors, meth.name(), meth);
	}

	@Override
	public void newObjectMethod(ErrorReporter errors, ObjectActionHandler om) {
		addEntry(errors, om.name(), om);
	}

	@Override
	public void newRequiredContract(ErrorReporter errors, RequiresContract rc) {
		addEntry(errors, rc.varName(), rc);
	}
	
	@Override
	public void newProvidesServiceWithName(ErrorReporter errors, Provides cs) {
		addEntry(errors, cs.varName(), cs);
	}

	@Override
	public void newContractImpl(ErrorReporter errors, ImplementsContract ci) {
		addEntry(errors, ci.name(), ci);
	}
	
	@Override
	public void newObjectContract(ErrorReporter errors, ObjectContract oc) {
		addEntry(errors, oc.varName(), oc);
	}
	
	@Override
	public void newStruct(ErrorReporter errors, StructDefn sd) {
		addEntry(errors, sd.name(), sd);
		for (PolyType p : sd.polys())
			addEntry(errors, p.name(), p);
	}

	@Override
	public void newStructField(ErrorReporter errors, StructField sf) {
		addEntry(errors, sf.name(), sf);
	}

	@Override
	public void newUnion(ErrorReporter errors, UnionTypeDefn ud) {
		addEntry(errors, ud.name(), ud);
		for (PolyType p : ud.polys())
			addEntry(errors, p.name(), p);
	}

	@Override
	public void newContract(ErrorReporter errors, ContractDecl decl) {
		addEntry(errors, decl.name(), decl);
	}

	@Override
	public void newContractMethod(ErrorReporter errors, ContractMethodDecl decl) {
		addEntry(errors, decl.name(), decl);
	}

	@Override
	public void newObject(ErrorReporter errors, ObjectDefn od) {
		addEntry(errors, od.name(), od);
		for (PolyType p : od.polys())
			addEntry(errors, p.name(), p);
	}

	@Override
	public void newObjectAccessor(ErrorReporter errors, ObjectAccessor oa) {
		addEntry(errors, oa.name(), oa);
	}

	public void unitTestPackage(ErrorReporter errors, UnitTestPackage pkg) {
		addEntry(errors, pkg.name(), pkg);
	}

	public void systemTest(ErrorReporter errors, SystemTest st) {
		addEntry(errors, st.name(), st);
	}

	@Override
	public void newTestData(ErrorReporter errors, UnitDataDeclaration data) {
		addEntry(errors, data.name(), data);
	}

	@Override
	public void newIntroduction(ErrorReporter errors, IntroduceVar var) {
		addEntry(errors, var.name(), var);
	}

	@Override
	public void newTemplate(ErrorReporter errors, Template template) {
		addEntry(errors, template.name(), template);
	}
	
	@Override
	public void polytype(ErrorReporter errors, PolyType pt) {
		addEntry(errors, pt.name(), pt);
	}

	@Override
	public void addEntry(ErrorReporter errors, final NameOfThing name, final RepositoryEntry entry) {
		String un = name.uniqueName();
		logger.debug("trying to add " + un + " of type " + entry.getClass());
		if (!checkNoStateConflicts(errors, name, entry)) {
			return;
		}
		if (dict.containsKey(un)) {
			if (errors != null) {
				errors.message(entry.location(), un + " is defined multiple times: " + dict.get(un).location());
				return;
			} else
				throw new DuplicateNameException(name);
		}
		dict.put(un, entry);
		if (currentDefines != null) {
			logger.debug("adding " + un + " to current defines: " + currentDefines.size());
			currentDefines.add(un);
		}
	}

	private boolean checkNoStateConflicts(ErrorReporter errors, NameOfThing name, RepositoryEntry entry) {
		if (entry instanceof StructField) {
			VarName vn = (VarName) name;
			String cname = vn.container().uniqueName() + ".";
			boolean conflicts = false;
			for (Entry<String, RepositoryEntry> e : dict.entrySet()) {
				RepositoryEntry val = e.getValue();
				if (e.getKey().startsWith(cname)) {
					NameOfThing rn = e.getValue().name();
					String base;
					if (rn instanceof FunctionName)
						base = ((FunctionName)rn).name;
					else if (rn instanceof VarName)
						base = ((VarName)rn).var;
					else
						continue;
					if (base.equals(vn.var)) {
						if (errors == null)
							throw new NotImplementedException("we should be passed errors in this case - figure it out");
						else {
							InputPosition loc = val.location();
							if (val instanceof TypedPattern) {
								loc = ((TypedPattern) val).var.loc;
							}
							errors.message(loc, "cannot use " + base + " here as it conflicts with state member at " + vn.loc);
						}
						conflicts = true;
					}
				}
			}
			return !conflicts;
		} else if (name instanceof FunctionName || name instanceof VarName) {
			String base;
			if (name instanceof FunctionName) {
				base = ((FunctionName)name).name;
				if (name.container() != null && name.container() instanceof CSName) {
					// It is perfectly acceptable to have an implementation "share" a name with local names
					// because they are not really here but inside "_C0" or whatever.
					// And they are not "used" locally
					return true;
				}
			} else if (name instanceof VarName)
				base = ((VarName)name).var;
			else
				throw new NotImplementedException("cannot extract base from " + name);
			NameOfThing n1 = name;
			while (n1 != null && !(n1 instanceof PackageName)) {
				if (n1 instanceof SolidName || n1 instanceof CardName) {
					RepositoryEntry other = dict.get(n1.uniqueName());
					if (other instanceof StateHolder) {
						StateDefinition state = ((StateHolder)other).state();
						if (state != null && state.hasMember(base)) {
							if (errors == null)
								throw new StateNameException(state.findField(base).name());
							else
								errors.message(entry.location(), "cannot use " + base + " here as it conflicts with state member at " + state.findField(base).loc);
							return false;
						}
					}
					break;
				}
				n1 = n1.container();
			}
			return true;
		} else
			return true;
	}

	@Override
	public void replaceDefinition(HandlerLambda hl) {
		if (!dict.containsKey(hl.name().uniqueName()))
			throw new NotImplementedException(hl.name().uniqueName() + " was not defined");
		dict.put(hl.name().uniqueName(), hl);
	}

	public void dumpTo(File dumpRepo) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(dumpRepo);
		dumpTo(pw);
		pw.close();
	}

	public void dumpTo(PrintWriter pw) {
		for (Entry<String, RepositoryEntry> x : dict.entrySet()) {
			pw.print(x.getKey() + " = ");
			x.getValue().dumpTo(pw);
		}
		for (HTMLData e : webs.htmls()) {
			pw.println("have webdata " + e.uri() + ": " + e.cards());
		}
		pw.flush();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends RepositoryEntry> T get(String string) {
		return (T)dict.get(string);
	}

	@Override
	public RepositoryEntry findNested(ErrorReporter errors, InputPosition loc, String scope, String tn) {
		RepositoryEntry have = null;
		for (Entry<String, RepositoryEntry> e : dict.entrySet()) {
			String key = e.getKey();
			if (key.startsWith(scope + ".") && e.getValue().name().baseName().equals(tn)) {
				if (have == null)
					have = e.getValue();
				else
					errors.message(loc, "ambiguous reference to " + tn + " inside " + scope);
			}
			
		}
		return have;
	}

	@Override
	public Type findUnionWith(ErrorReporter errors, InputPosition pos, Set<Type> ms, boolean needAll) {
		if (ms.isEmpty())
			throw new NotImplementedException();
		Set<Type> collect = new HashSet<Type>();
		Set<PolyType> polys = new HashSet<PolyType>();
		for (Type t : ms) {
			if (t instanceof Primitive && ((Primitive)t).willAcceptAll(ms)) {
				// If t is a 'special' union (eg Any), and is only unified with one other type,
				// then consider that what we are talking about here is a case of a more-specific type being passed
				// to a function expecting a general type.
				
				// Of course, there may be other more subtle cases in here, so be careful.  We need to be precise about
				// what the roles are.
				if (ms.size() == 2 && !needAll) {
					HashSet<Type> foo = new HashSet<>(ms);
					foo.remove(t);
					return foo.iterator().next();
				}
				return t;
			} else if (t == LoadBuiltins.error)
				continue;
			else if (t instanceof PolyType)
				polys.add((PolyType) t);
			else
				collect.add(t);
		}
		if (collect.isEmpty()) {
			if (polys.isEmpty())
				return LoadBuiltins.any;
			else if (polys.size() == 1)
				return polys.iterator().next();
			else {
				// We have been asked to unify two distinct polymorphic vars
				// Since they should only be resolved to polymorphic vars AFTER unification, something has gone wrong in that we didn't see they were the same
				throw new CantHappenException("multiple distinct polys in union: " + polys);
			}
		} else if (collect.size() == 1)
			return collect.iterator().next();
		List<Type> matching = new ArrayList<>();
		for (RepositoryEntry k : dict.values()) {
			if (k instanceof UnionTypeDefn) {
				UnionTypeDefn utd = (UnionTypeDefn) k;
				Type union = utd.matches(errors, pos, this, collect, needAll);
				if (union != null)
					matching.add(union);
			}
		}
		if (matching.isEmpty()) {
			return null;
		} else if (matching.size() == 1)
			return matching.get(0);
		else {
			// if multiple match, accept a uniquely "smallest" one
			int min = Integer.MAX_VALUE;
			Type ret = null;
			boolean ambig = false;
			for (Type t : matching) {
				int nc = ncases(t);
				if (nc < min) {
					min = nc;
					ret = t;
				} else if (nc == min) {
					ambig = true;
				}
			}
			if (!ambig) {
				return ret;
			}
			TreeSet<String> tyes = new TreeSet<String>();
			for (Type ty : ms)
				tyes.add(ty.signature());
			TreeSet<String> us = new TreeSet<String>();
			for (Type ty : matching)
				us.add(ty.signature());
			errors.message(pos, "multiple unions match " + tyes + ": " + us);
			return null;
		}
	}

	private int ncases(Type t) {
		if (t instanceof PolyInstance)
			t = ((PolyInstance)t).struct();
		return ((UnionTypeDefn)t).cases.size();
	}

	@Override
	public List<UnionTypeDefn> unionsContaining(StructDefn sd) {
		List<UnionTypeDefn> ret = new ArrayList<UnionTypeDefn>();
		for (RepositoryEntry k : dict.values()) {
			if (k instanceof UnionTypeDefn) {
				UnionTypeDefn utd = (UnionTypeDefn) k;
				if (utd.hasCase(sd))
					ret.add(utd);
			}
		}
		return ret;
	}

	@Override
	public void traverse(RepositoryVisitor visitor) {
		Traverser t = new Traverser(visitor);
		t.doTraversal(this);
	}

	public void traverseWithImplementedMethods(RepositoryVisitor visitor) {
 		Traverser t = new Traverser(visitor);
 		t.withImplementedMethods();
		t.doTraversal(this);
	}

	public void traverseLifted(RepositoryVisitor visitor) {
 		Traverser t = new Traverser(visitor);
		t.withNestedPatterns();
		t.doTraversal(this);
	}

	@Override
	public void traverseInGroups(RepositoryVisitor visitor, FunctionGroups groups) {
 		Traverser t = new Traverser(visitor);
		t.withNestedPatterns();
		t.withFunctionsInDependencyGroups(groups);
		t.withEventSources();
		t.doTraversal(this);
	}

	@Override
	public void traverseWithMemberFields(RepositoryVisitor visitor) {
 		Traverser t = new Traverser(visitor);
		t.withMemberFields();
		t.doTraversal(this);
	}

	@Override
	public void traverseWithHSI(HSIVisitor v) {
		Traverser t = new Traverser(v).withHSI().withNestedPatterns();
		t.doTraversal(this);
	}

	public void traverseAssemblies(Configuration config, ErrorReporter errors, JSEnvironment jse, ByteCodeEnvironment bce, AssemblyVisitor v) {
		AssemblyTraverser t = new AssemblyTraverser(config, errors, jse, bce, v);
		t.doTraversal(this);
	}

	public void traverseAssembly(Configuration config, ErrorReporter errors, JSEnvironment jse, ByteCodeEnvironment bce, AssemblyVisitor v, Assembly asm) {
		AssemblyTraverser t = new AssemblyTraverser(config, errors, jse, bce, v);
		t.traverse(this, asm);
	}

	public ConcreteMetaData ensureWebData() {
		return webs;
	}

	@Override
	public CardData findWeb(String baseName) {
		for (HTMLData web : webs.htmls()) {
			try {
				logger.info("looking for " + baseName + " in " + web.uri());
				return web.forCard(baseName);
			} catch (NoMetaDataException ex) {
				; // it wasn't there ...
				logger.info("... not there");
			}
		}
		logger.error("could not find " + baseName + " in " + webs.htmls());
		return null;
	}

	public SplitMetaData allWebs() {
		return webs;
	}

	public void clean() {
		for (RepositoryEntry e : dict.values())
			e.clean();
	}

	@Override
	public void dump() {
		for (RepositoryEntry e : dict.values())
			System.out.println(e.name().uniqueName() + " => " + e);
	}

	public List<NameOfThing> rootPackageNames() {
		List<NameOfThing> rpns = new ArrayList<>();
		for (RepositoryEntry e : dict.values()) {
			NameOfThing name = e.name();
			if (name.container() instanceof PackageName) {
				PackageName pn = (PackageName) name.container();
				if (pn.baseName() == null && !pn.isBuiltin())
					rpns.add(name);
			}
		}
		return rpns;
	}

	public List<Assembly> getAssemblies() {
		List<Assembly> ret = new ArrayList<Assembly>();
		for (RepositoryEntry d : dict.values()) {
			if (d instanceof Assembly)
				ret.add((Assembly) d);
		}
		return ret ;
	}
	
	public Collection<String> flimPackages() {
		return flimDefines.keySet();
	}
}
