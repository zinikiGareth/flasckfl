package org.flasck.flas.compiler.jsgen.packaging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.compiler.jsgen.creators.JSClass;
import org.flasck.flas.compiler.jsgen.creators.JSMethod;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSFile {
	private final String pkg;
	private final File file;
	private final Set<String> packages = new TreeSet<>();
	private final List<JSClass> classes = new ArrayList<>();
	private final List<JSMethod> functions = new ArrayList<>();

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

	// untested
	public void write() throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(file);
		IndentWriter iw = new IndentWriter(pw);
		writeTo(iw);
		pw.close();
	}

	public void writeTo(IndentWriter iw) {
		declarePackages(iw);
		for (JSClass c : classes)
			c.writeTo(iw);
		for (JSMethod f : functions) {
			declareContainingPackage(iw, f);
			f.write(iw);
		}
	}

	private void declareContainingPackage(IndentWriter iw, JSMethod f) {
		String full = f.getPackage()+"."+f.getName();
		int li = full.lastIndexOf('.');
		full = full.substring(0, li);
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
}