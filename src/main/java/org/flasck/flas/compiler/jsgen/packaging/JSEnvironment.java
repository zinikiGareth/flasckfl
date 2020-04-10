package org.flasck.flas.compiler.jsgen.packaging;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.creators.JSClass;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethod;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.jsgen.form.JSThis;
import org.flasck.flas.parsedForm.ContractDecl;
import org.zinutils.utils.FileUtils;

/** The idea here is to create a set of "package" files in memory with abstract constructs.
 * We then have a method to iterate over all of them and turn that into JS files in one go.
 * 
 * @author gareth
 */
public class JSEnvironment implements JSStorage {
	// The idea is that there is one file per package
	private final Map<String, JSFile> files = new TreeMap<String, JSFile>();
	private final File root;
	private final List<ContractDecl> contracts = new ArrayList<>();

	public JSEnvironment(File root) {
		this.root = root;
	}
	
	public Iterable<File> files() {
		return files.values().stream().map(jsf -> jsf.file()).collect(Collectors.toList());
	}

	@Override
	public void ensurePackageExists(String filePkg, String pkg) {
		if (filePkg.equals(pkg))
			return;
		if (!pkg.startsWith(filePkg))
			throw new RuntimeException(pkg + " is not in " + filePkg);
		getPackage(filePkg).ensurePackage(pkg);
	}

	@Override
	public JSClassCreator newClass(String pkg, String clz) {
		JSFile inpkg = getPackage(pkg);
		JSClass ret = new JSClass(clz);
		inpkg.addClass(ret);
		return ret;
	}
	
	@Override
	public JSMethodCreator newFunction(String pkg, String cxt, boolean isPrototype, String name) {
		JSFile inpkg = getPackage(pkg);
		JSMethod ret = new JSMethod(cxt, isPrototype, name);
		inpkg.addFunction(ret);
		return ret;
	}

	@Override
	public void methodList(SolidName name, List<FunctionName> methods) {
		JSFile inpkg = getPackage(name.container().uniqueName());
		inpkg.methodList(name, methods);
	}

	public JSFile getPackage(String pkg) {
		JSFile inpkg = files.get(pkg);
		if (inpkg == null) {
			File f = new File(root, pkg + ".js");
			inpkg = new JSFile(pkg, f);
			files.put(pkg, inpkg);
		}
		return inpkg;
	}

	@Override
	public void contract(ContractDecl cd) {
		contracts.add(cd);
	}

	@Override
	public void complete() {
		for (Entry<String, JSFile> p : files.entrySet()) {
			JSMethod ifn = new JSMethod(p.getKey(), false, "_init");
			ifn.argument("_cxt");
			for (ContractDecl cd : contracts)
				ifn.cxtMethod("registerContract", new JSString(cd.name().uniqueName()), ifn.newOf(cd.name()));
			p.getValue().addFunction(ifn);
		}
	}

	// debugMethod
	public void dumpAll(boolean b) {
		for (File f : files()) {
			System.out.println("JSFile " + f);
			FileUtils.cat(f);
		}
	}

	// untested
	public void writeAllTo(File jsDir) throws FileNotFoundException {
		FileUtils.assertDirectory(jsDir);
		for (JSFile jsf : files.values()) {
			jsf.write();
		}
	}
}
