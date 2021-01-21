package org.flasck.flas.compiler.jsgen.packaging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JSClass;
import org.flasck.flas.compiler.jsgen.creators.JSMethod;
import org.flasck.flas.compiler.templates.EventTargetZones;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.CantHappenException;

public class JSFile {
	private final String pkg;
	private final File file;
	private final Set<String> packages = new TreeSet<>();
	private final List<JSClass> classes = new ArrayList<>();
	private final List<JSMethod> functions = new ArrayList<>();
	private final List<MethodList> methodLists = new ArrayList<>();
	private final List<EventMap> eventMaps = new ArrayList<>();

	public JSFile(String pkg, File file) {
		this.pkg = pkg;
		this.file = file;
	}

	public File file() {
		return file;
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

	// untested
	public File write(File jsDir) throws FileNotFoundException {
		File f = new File(jsDir, file.getName());
		PrintWriter pw = new PrintWriter(f);
		IndentWriter iw = new IndentWriter(pw);
		writeTo(iw);
		pw.close();
		return f;
	}

	public void writeTo(IndentWriter iw) {
		declarePackages(iw);
		ListMap<String, JSClass> deferred = new ListMap<>();
		for (JSClass c : classes) {
			// Handlers can be nested inside functions, so defer them ...
			if (c.name().container() instanceof FunctionName) {
				FunctionName fn = (FunctionName) c.name().container();
				if (!fn.baseName().startsWith("_"))
					throw new CantHappenException("was expecting a function case name");
				deferred.add(fn.container().jsName(), c);
			} else
				c.writeTo(iw);
		}
		Set<NameOfThing> names = new HashSet<>();
		for (JSMethod f : functions) {
			declareContainingPackage(iw, f);
			f.write(iw, names);
			if (deferred.contains(f.jsName())) {
				for (JSClass c : deferred.get(f.jsName()))
					c.writeTo(iw);
			}
		}
		for (MethodList m : methodLists)
			m.write(iw);
		for (EventMap m : eventMaps)
			m.write(iw);
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
	}

	private void declareContainingPackage(IndentWriter iw, JSMethod f) {
		String full = f.getPackage()+"."+f.getName();
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
		if (pkg == null)
			return;
		String[] pkgs = pkg.split("\\.");
		String enclosing = "";
		for (String s : pkgs) {
			String full = enclosing + s;
			declarePackage(iw, full);
			enclosing = enclosing + s + ".";
		}
	}

	private void declarePackage(IndentWriter iw, String full) {
		iw.print("if (typeof(");
		iw.print(full);
		iw.print(") === 'undefined') ");
		iw.print(full);
		iw.println(" = {};");
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
}
