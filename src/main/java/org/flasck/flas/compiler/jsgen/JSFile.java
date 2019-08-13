package org.flasck.flas.compiler.jsgen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSFile {
	private final String pkg;
	private final File file;
	private final List<JSClass> classes = new ArrayList<>();
	private final List<JSMethod> functions = new ArrayList<>();

	public JSFile(String pkg, File file) {
		this.pkg = pkg;
		this.file = file;
	}

	public File file() {
		return file;
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
		for (JSMethod f : functions)
			f.write(iw);
	}

	private void declarePackages(IndentWriter iw) {
		if (pkg == null)
			return;
		String[] pkgs = pkg.split("\\.");
		String enclosing = "";
		for (String s : pkgs) {
			iw.print("if (!");
			iw.print(enclosing);
			iw.print(s);
			iw.print(") ");
			iw.print(enclosing);
			iw.print(s);
			iw.println(" = {};");
			enclosing = enclosing + s + ".";
		}
	}
}
