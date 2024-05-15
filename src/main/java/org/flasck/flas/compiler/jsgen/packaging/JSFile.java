package org.flasck.flas.compiler.jsgen.packaging;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.creators.JSClass;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethod;
import org.flasck.flas.compiler.templates.EventTargetZones;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.repository.Repository;
import org.flasck.jvm.ziniki.ContentObject;
import org.flasck.jvm.ziniki.MemoryContentObject;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.utils.FileUtils;

public class JSFile {
	private final Repository repository;
	private final NameOfThing pkg;
	private final boolean isTest;
	private final String key;
	private final Set<String> packages = new TreeSet<>();
	private final List<JSClass> classes = new ArrayList<>();
	private final List<JSMethod> functions = new ArrayList<>();
	private final List<MethodList> methodLists = new ArrayList<>();
	private final List<EventMap> eventMaps = new ArrayList<>();
	private final List<ApplRoutingTable> routes = new ArrayList<>();
	private final Set<String> exports = new TreeSet<>();
	private MemoryContentObject co;

	public JSFile(Repository repository, NameOfThing pkg, String name) {
		this.repository = repository;
		this.pkg = pkg;
		this.key = name;
		this.isTest = pkg.uniqueName().contains("_");
	}

	public String key() {
		return key;
	}

	public void ensurePackage(String nested) {
		packages.add(nested);
	}

	public void addClass(JSClass ret) {
		classes.add(ret);
	}

	public void addFunction(JSMethod jsMethod) {
		functions.add(jsMethod);
	}

	public void methodList(NameOfThing name, List<FunctionName> methods) {
		methodLists.add(new MethodList(name, methods));
	}

	public void eventMap(NameOfThing name, EventTargetZones etz) {
		eventMaps.add(new EventMap(name, etz));
	}

	public void applRouting(JSClassCreator clz, NameOfThing name, ApplicationRouting routes) {
		this.routes.add(new ApplRoutingTable(name, routes));
	}

	// untested
	public ContentObject generate(Collection<String> imports) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		IndentWriter iw = new IndentWriter(pw);
		writeTo(iw, imports);
		pw.close();
		
		this.co = new MemoryContentObject(key, sw.toString().getBytes());
		return co;
	}


	public void saveTo(File flimdir) {
		File f = new File(flimdir, this.co.key());
		FileUtils.copyStreamToFile(this.co.asStream(), f);
	}

	public boolean upload(JSUploader uploader) throws IOException {
		return uploader.uploadJs(this.co);
	}

	public void writeTo(IndentWriter iw, Collection<String> imports) {
		declarePackages(iw);
		iw.println(
			"import { IdempotentHandler } from \"/js/ziwsh.js\";"
		);
		iw.println(
			"import { Application, Assign, AssignCons, AssignItem, Debug, ResponseWithMessages, Send, UpdateDisplay, ClickEvent, ScrollTo, ContractStore, Entity, Image, Link, FLBuiltin, False, True, MakeHash, HashPair, Tuple, TypeOf, FLCard, FLObject, FLError, Nil, Cons, Crobag, CroEntry, SlideWindow, CrobagWindow, CrobagChangeEvent, CrobagWindowEvent, Random, Interval, Instant, Calendar } from \"/js/flasjs.js\";"
		);
		if (isTest) {
			iw.println(
				"import { BoundVar } from \"/js/flastest.js\";"
			);
		}
		for (String s : imports) {
			if (s.equals(pkg.uniqueName()))
				continue;
			PackageName pn = new PackageName(s);
			if (s.equals("root.package"))
				importRootPackage(iw, s, pn);
			else {				
				iw.println("import { " + pn.jsName() + " } from \"/js/" + s + ".js\";");
			}
		}
		ListMap<String, JSClass> deferred = new ListMap<>();
		for (JSClass c : classes) {
			// Handlers can be nested inside functions, so defer them ...
			if (c.name().container() instanceof FunctionName) {
				FunctionName fn = (FunctionName) c.name().container();
				if (!fn.baseName().startsWith("_"))
					throw new CantHappenException("was expecting a function case name");
				deferred.add(fn.container().jsName(), c);
			} else
				c.writeTo(exports, iw);
		}
		Set<NameOfThing> names = new HashSet<>();
		for (JSMethod f : functions) {
			declareContainingPackage(iw, f);
			f.write(iw, names, exports);
			if (deferred.contains(f.jsName())) {
				for (JSClass c : deferred.get(f.jsName()))
					c.writeTo(exports, iw);
			}
		}
		for (MethodList m : methodLists)
			m.write(iw);
		for (EventMap m : eventMaps)
			m.write(iw);
		for (ApplRoutingTable r : routes)
			r.write(iw);

		exportPackages(iw);
	}

	private void importRootPackage(IndentWriter iw, String s, PackageName pn) {
		iw.print("import { " + pn.jsName());
		for (NameOfThing n : repository.rootPackageNames()) {
			iw.print(", ");
			iw.print(n.jsName());
		}
		iw.println(" } from \"/js/" + s + ".js\";");
	}

	public void generate(ByteCodeEnvironment bce) {
//		declarePackages(iw);
		for (JSClass c : classes)
			c.generate(bce);
		for (JSMethod f : functions) {
			f.generate(bce, false);
		}
//		for (MethodList m : methodLists)
//			m.write(iw);
		for (EventMap m : eventMaps)
			m.generate(bce);
		for (ApplRoutingTable r : routes)
			r.generate(bce);
	}

	private void declareContainingPackage(IndentWriter iw, JSMethod f) {
		String full = f.getPackage() + "." + f.getName();
		int li = full.lastIndexOf('.');
		full = full.substring(0, li);
		for (JSClass clz : classes) {
			if (clz.clzname().equals(full))
				return;
		}
		if (packages.contains(full)) {
			declarePackage(iw, full);
			packages.remove(full);
		}
	}

	private void declarePackages(IndentWriter iw) {
		if (pkg == null || pkg.uniqueName().endsWith("_st"))
			return;
		declarePackage(iw, pkg.uniqueName());
		iw.println("");
	}

	private void declarePackage(IndentWriter iw, String full) {
		iw.print("var ");
		String exp = full.replace(".", "__");
		exports.add(exp);
		iw.print(exp);
		iw.println(" = {};");
	}

	private void exportPackages(IndentWriter iw) {
		if (exports.isEmpty())
			return;
		iw.println("");
		iw.print("export { ");
		String sep = "";
		for (String s : exports) {
			iw.print(sep);
			iw.print(s);
			sep = ", ";
		}
		iw.println(" };");
	}

	public List<JSClass> classes() {
		return classes;
	}

	public List<JSMethod> functions() {
		return functions;
	}

	public void asivm() {
		for (JSClass c : classes)
			System.out.println(c.asivm());
		for (JSMethod m : functions)
			System.out.println(m.asivm());
	}

	public ContentObject co() {
		return this.co;
	}
}
