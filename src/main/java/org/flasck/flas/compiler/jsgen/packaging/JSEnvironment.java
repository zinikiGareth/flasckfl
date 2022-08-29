package org.flasck.flas.compiler.jsgen.packaging;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.Configuration;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.creators.JSClass;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethod;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSLiteral;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.templates.EventTargetZones;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.jvm.ziniki.ContentObject;
import org.flasck.jvm.ziniki.FileContentObject;
import org.flasck.jvm.ziniki.PackageSources;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.graphs.DirectedAcyclicGraph;
import org.zinutils.graphs.Node;
import org.zinutils.graphs.NodeWalker;
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
	private final List<StructDefn> structs = new ArrayList<>();
	private final List<ContractDecl> contracts = new ArrayList<>();
	private final List<ObjectDefn> objects = new ArrayList<>();
	private final List<HandlerImplements> handlers = new ArrayList<>();
	private final DirectedAcyclicGraph<String> pkgdag;
	private final JSUploader uploader;
	private final Map<String, ContentObject> gencos = new TreeMap<>();
	private final List<File> localOnly = new ArrayList<>();
	private final List<SystemTest> systemTests = new ArrayList<>();

	public JSEnvironment(File root, DirectedAcyclicGraph<String> pkgs, JSUploader uploader) {
		this.root = root;
		this.pkgdag = pkgs;
		this.uploader = uploader;
	}
	
	public Iterable<File> files() {
		List<File> ret = new ArrayList<>();
		Iterable<String> pkgs = packages();
		for (String s : pkgs) {
			JSFile f = files.get(s);
			if (f != null)
				ret.add(f.file());
		}
		return ret;
	}

	@Override
	public File fileFor(String s) {
		JSFile r = files.get(s);
		if (r == null)
			return null;
		return r.file();
	}

	@Override
	public void ensurePackageExists(String filePkg, String pkg) {
		if (filePkg == null)
			filePkg = "root.package";
		if (pkg == null)
			pkg = "root.package";
		if (filePkg.equals(pkg))
			return;
		if (!pkg.startsWith(filePkg))
			throw new RuntimeException(pkg + " is not in " + filePkg);
		getPackage(filePkg).ensurePackage(pkg);
	}

	@Override
	public JSClassCreator newClass(String pkg, NameOfThing clz) {
		JSFile inpkg = getPackage(pkg);
		JSClass ret = new JSClass(this, clz);
		inpkg.addClass(ret);
		return ret;
	}
	
	@Override
	public JSClassCreator newUnitTest(UnitTestCase ut) {
		JSFile inpkg = getPackage(ut.name.container().jsName());
		JSClass ret = new JSClass(this, ut.name);
		inpkg.addClass(ret);
		return ret;
	}

	@Override
	public JSClassCreator newSystemTest(SystemTest st) {
		systemTests.add(st);
		JSFile inpkg = getPackage(st.name().packageName().jsName() + "._st");
		JSClass ret = new JSClass(this, st.name());
		inpkg.addClass(ret);
		return ret;
	}
	
	@Override
	public Iterable<SystemTest> systemTests() {
		return systemTests;
	}

	@Override
	public JSMethodCreator newFunction(NameOfThing fnName, String pkg, NameOfThing cxt, boolean isPrototype, String name) {
		JSFile inpkg = getPackage(pkg);
		JSMethod ret = new JSMethod(this, fnName, cxt, isPrototype, name);
		inpkg.addFunction(ret);
		return ret;
	}

	@Override
	public void methodList(NameOfThing name, List<FunctionName> methods) {
		JSFile inpkg = getPackage(name.packageName().uniqueName());
		inpkg.methodList(name, methods);
	}

	@Override
	public void eventMap(NameOfThing name, EventTargetZones eventMethods) {
		JSFile inpkg = getPackage(name.packageName().uniqueName());
		inpkg.eventMap(name, eventMethods);
	}

	@Override
	public void applRouting(JSClassCreator clz, NameOfThing name, ApplicationRouting routes) {
		JSFile inpkg = getPackage(name.packageName().uniqueName());
		inpkg.applRouting(clz, name, routes);
	}

	public JSFile getPackage(String pkg) {
		if (pkg == null)
			pkg = "root.package";
		JSFile inpkg = files.get(pkg);
		if (inpkg == null) {
			File f = new File(root, pkg + ".js");
			inpkg = new JSFile(pkg, f);
			files.put(pkg, inpkg);
		}
		return inpkg;
	}

	@Override
	public void struct(StructDefn s) {
		structs.add(s);
	}

	@Override
	public void contract(ContractDecl cd) {
		contracts.add(cd);
	}

	@Override
	public void object(ObjectDefn cd) {
		objects.add(cd);
	}

	@Override
	public void handler(HandlerImplements hi) {
		handlers.add(hi);
	}

	@Override
	public void complete() {
		for (Entry<String, JSFile> p : files.entrySet()) {
			String pkg = p.getKey();
			if (pkg.contains("._ut_") || pkg.contains("_st_"))
				continue;
			JSMethod ifn = new JSMethod(this, null, new PackageName(pkg), false, "_init");
			ifn.noJVM();
			ifn.argument("_cxt");
			for (ContractDecl cd : contracts)
				ifn.cxtMethod("registerContract", new JSString(cd.name().uniqueName()), ifn.newOf(cd.name()));
			for (ObjectDefn od : objects)
				ifn.cxtMethod("registerObject", new JSString(od.name().uniqueName()), ifn.literal(od.name().uniqueName()));
			for (HandlerImplements hi : handlers)
				ifn.cxtMethod("registerStruct", new JSString(hi.name().uniqueName()), ifn.literal(hi.name().jsName()));
			for (StructDefn hi : structs)
				ifn.cxtMethod("registerStruct", new JSString(hi.name().uniqueName()), ifn.literal(hi.name().jsName()));
			ifn.ifTrue(new JSLiteral(pkg + "._builtin_init")).trueCase().callMethod("void", null, pkg + "._builtin_init");
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
			File tof = jsf.write(jsDir);
			if (uploader != null) {
				ContentObject co = uploader.uploadJs(tof);
				if (co != null)
					gencos.put(jsf.file().getName(), co);
				else
					localOnly.add(tof);
			}
		}
	}

	public void generate(ByteCodeEnvironment bce) {
		for (JSFile jsf : files.values()) {
			jsf.generate(bce);
		}
	}

	public Iterable<String> packages() {
		LinkedHashSet<String> ret = new LinkedHashSet<>();
		if (pkgdag.hasNode("root.package"))
			ret.add("root.package");
		for (String s : files.keySet()) {
			pkgdag.ensure(s);
			pkgdag.postOrderFrom(new NodeWalker<String>() {
				@Override
				public void present(Node<String> node) {
					ret.add(node.getEntry());
				}
			}, s);
		}
		return ret;
	}

	@Override
	public Iterable<ContentObject> jsIncludes(Configuration config, String testDirJS) {
		List<ContentObject> ret = new ArrayList<>();
		if (config.flascklibDir != null) {
			figureJSFilesOnDisk(ret, config, testDirJS);
		} else if (config.flascklibCPV != null) {
			figureJSFilesFromContentStore(ret, config, testDirJS);
		}
		return ret;
	}

	private void figureJSFilesOnDisk(List<ContentObject> ret, Configuration config, String testDirJS) {
		List<String> inlib = new ArrayList<>();
		addFrom(ret, testDirJS, inlib, new File(config.flascklibDir, "main"));
		addFrom(ret, testDirJS, inlib, new File(config.flascklibDir, "test"));
		for (File mld : config.modules) {
			addFrom(ret, testDirJS, inlib, mld);
		}

		for (String s : packages()) {
			File f = fileFor(s);
			if (f != null) {
				includeFile(ret, testDirJS, f);
				inlib.add(f.getName());
			} else {
				for (File q : config.readFlims) {
					File i = new File(q, s + ".js");
					if (i.exists()) {
						if (!inlib.contains(i.getName())) {
							includeFile(ret, testDirJS, i);
							inlib.add(i.getName());
						}
					}
				}
				for (File q : config.includeFrom) {
					for (File i : FileUtils.findFilesMatching(q, s + ".js")) {
						if (!inlib.contains(i.getName())) {
							includeFile(ret, testDirJS, i);
							inlib.add(i.getName());
						}
					}
				}
			}
		}
	}

	private void addFrom(List<ContentObject> ret, String testDirJS, List<String> inlib, File from) {
		List<File> library = FileUtils.findFilesMatching(from, "*");
		for (File f : library) {
			includeFile(ret, testDirJS, f);
			inlib.add(f.getName());
		}
	}

	private void figureJSFilesFromContentStore(List<ContentObject> ret, Configuration config, String testDir) {
		for (ContentObject co : config.flascklibCPV.mainjs()) {
			ret.add(co);
		}
		for (ContentObject co : config.flascklibCPV.livejs()) {
			ret.add(co);
		}
		for (ContentObject co : config.flascklibCPV.testjs()) {
			ret.add(co);
		}
		if (config.moduleCOs != null) {
			for (PackageSources d : config.moduleCOs) {
				for (ContentObject co : d.mainjs())
					ret.add(co);
				for (ContentObject co : d.livejs())
					ret.add(co);
				for (ContentObject co : d.testjs())
					ret.add(co);
			}
		}
		if (config.dependencies != null) {
			for (PackageSources d : config.dependencies) {
				for (ContentObject co : d.mainjs())
					ret.add(co);
				for (ContentObject co : d.livejs())
					ret.add(co);
				for (ContentObject co : d.testjs())
					ret.add(co);
			}
		}
		for (ContentObject co : gencos.values()) {
			ret.add(co);
		}
		for (File f : localOnly) {
			includeFile(ret, testDir, f);
		}
	}

	private void includeFile(List<ContentObject> ret, String testDir, File f) {
		if (!f.isAbsolute())
			f = new File(new File(System.getProperty("user.dir")), f.getPath());
		ret.add(new FileContentObject(f));
		if (testDir != null && f.isFile()) {
			FileUtils.copy(f, new File(testDir, f.getName()));
		}
	}
	
	public void asivm() {
		for (JSFile f : files.values()) {
			f.asivm();
		}
	}
	
	@Override
	public String toString() {
		return "JSEnv[" + files + "]";
	}
}
