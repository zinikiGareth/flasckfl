package org.flasck.flas.compiler.jsgen.packaging;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
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
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.Repository;
import org.flasck.jvm.ziniki.ContentObject;
import org.flasck.jvm.ziniki.FileContentObject;
import org.flasck.jvm.ziniki.PackageSources;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.InvalidUsageException;
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
	private final Repository repository;
	// The idea is that there is one file per package
	private final Map<String, JSFile> files = new TreeMap<String, JSFile>();
	private final List<StructDefn> structs = new ArrayList<>();
	private final List<ContractDecl> contracts = new ArrayList<>();
	private final List<ObjectDefn> objects = new ArrayList<>();
	private final List<HandlerImplements> handlers = new ArrayList<>();
	private final DirectedAcyclicGraph<String> pkgdag;
	private final Map<String, ContentObject> gencos = new TreeMap<>();
	private final List<ContentObject> localOnly = new ArrayList<>();
	private final List<UnitTestCase> unitTests = new ArrayList<>();
	private final List<SystemTest> systemTests = new ArrayList<>();
	private final Configuration config;
	private final ErrorReporter errors;

	public JSEnvironment(Configuration config, ErrorReporter errors, Repository repository, DirectedAcyclicGraph<String> pkgs) {
		this.config = config;
		this.errors = errors;
		this.repository = repository;
		this.pkgdag = pkgs;
	}
	
	public Iterable<ContentObject> files() {
		List<ContentObject> ret = new ArrayList<>();
		Iterable<String> pkgs = packageStrings();
		for (String s : pkgs) {
			JSFile f = files.get(s);
			if (f != null)
				ret.add(f.co());
		}
		return ret;
	}

	@Override
	public ContentObject fileFor(String s) {
		JSFile r = files.get(s);
		if (r == null)
			return null;
		return r.co();
	}

	@Override
	public void ensurePackageExists(NameOfThing filePkg, String pkg) {
		if (filePkg == null || filePkg.uniqueName() == null)
			filePkg = new PackageName("root.package");
		if (pkg == null)
			pkg = "root.package";
		if (filePkg.jsName().equals(pkg))
			return;
		if (!pkg.startsWith(filePkg.uniqueName()) && !pkg.startsWith(filePkg.jsName()))
			throw new RuntimeException(pkg + " is not in " + filePkg);
		getPackage(filePkg).ensurePackage(pkg);
	}

	@Override
	public JSClassCreator newClass(PackageName pkg, NameOfThing clz) {
		JSFile inpkg = getPackage(pkg);
		JSClass ret = new JSClass(this, clz);
		inpkg.addClass(ret);
		return ret;
	}
	
	@Override
	public JSClassCreator newUnitTest(UnitTestCase ut) {
		unitTests.add(ut);
		JSFile inpkg = getPackage(ut.name.container());
		JSClass ret = new JSClass(this, ut.name);
		inpkg.addClass(ret);
		return ret;
	}

	@Override
	public JSClassCreator newSystemTest(SystemTest st) {
		systemTests.add(st);
		JSFile inpkg = getPackage(st.name());
		JSClass ret = new JSClass(this, st.name());
		inpkg.addClass(ret);
		return ret;
	}
	
	@Override
	public Iterable<SystemTest> systemTests() {
		return systemTests;
	}

	@Override
	public Iterable<UnitTestCase> unitTests() {
		return unitTests;
	}

	@Override
	public JSMethodCreator newFunction(NameOfThing fnName, PackageName pkg, NameOfThing cxt, boolean isPrototype, String name) {
		JSFile inpkg = getPackage(pkg);
		JSMethod ret = new JSMethod(this, fnName, cxt, isPrototype, name);
		inpkg.addFunction(ret);
		return ret;
	}

	@Override
	public void methodList(NameOfThing name, List<FunctionName> methods) {
		JSFile inpkg = getPackage(name.packageName());
		inpkg.methodList(name, methods);
	}

	@Override
	public void eventMap(NameOfThing name, EventTargetZones eventMethods) {
		JSFile inpkg = getPackage(name.packageName());
		inpkg.eventMap(name, eventMethods);
	}

	@Override
	public void applRouting(JSClassCreator clz, NameOfThing name, ApplicationRouting routes) {
		JSFile inpkg = getPackage(name.packageName());
		inpkg.applRouting(clz, name, routes);
	}

	public JSFile getPackage(NameOfThing pkg) {
		if (pkg == null || pkg.uniqueName() == null)
			pkg = new PackageName("root.package");
		else if (pkg.uniqueName().contains("__"))
			throw new CantHappenException("there should not be a __ in the package name: " + pkg);
		JSFile inpkg = files.get(pkg.uniqueName());
		if (inpkg == null) {
			String name = pkg.uniqueName() + ".js";
			inpkg = new JSFile(repository, pkg, name);
			files.put(pkg.uniqueName(), inpkg);
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
			PackageName pp = new PackageName(pkg);
			JSMethod ifn = new JSMethod(this, null, pp, false, "_init");
			ifn.noJVM();
			ifn.argument("_cxt");
			for (ContractDecl cd : contracts)
				ifn.cxtMethod("registerContract", new JSString(cd.name().uniqueName()), ifn.newOf(cd.name()));
			for (ObjectDefn od : objects)
				ifn.cxtMethod("registerObject", new JSString(od.name().uniqueName()), ifn.literal(od.name().jsName()));
			for (HandlerImplements hi : handlers)
				ifn.cxtMethod("registerStruct", new JSString(hi.name().uniqueName()), ifn.literal(hi.name().jsName()));
			for (StructDefn hi : structs)
				ifn.cxtMethod("registerStruct", new JSString(hi.name().uniqueName()), ifn.literal(hi.name().jsName()));
			ifn.ifTrue(new JSLiteral(pp.jsName() + "._builtin_init")).trueCase().callMethod("void", null, pp.jsName() + "._builtin_init");
			p.getValue().addFunction(ifn);
		}
	}

	// debugMethod
	public void dumpAll(boolean b) {
		for (ContentObject f : files()) {
			System.out.println("JSFile " + f.key());
			try (InputStream cos = f.asStream()) {
				FileUtils.copyStreamWithoutClosingEither(cos, System.out);
			} catch (IOException ex) {
				System.err.println("could not dump " + f.key());
			}
		}
	}

	// untested
	public void generateCOs() throws IOException {
		Collection<String> imports = fileImports();
		for (JSFile jsf : files.values()) {
			ContentObject co = jsf.generate(config, imports);
			gencos.put(jsf.key(), co);
		}
	}
	
	public void saveCOsTo(File flimdir) {
		FileUtils.assertDirectory(flimdir);
		for (JSFile jsf : files.values()) {
			jsf.saveTo(flimdir);
		}
	}

	public void upload(JSUploader uploader) throws IOException {
		for (JSFile jsf : files.values()) {
			ContentObject co = jsf.upload(uploader);
			if (co != null)
				gencos.put(jsf.key(), co);
			else
				localOnly.add(jsf.co());
		}
	}

	private Collection<String> fileImports() {
		Collection<String> wantedPkgs = packageStrings();
		LinkedHashSet<String> ret = new LinkedHashSet<>();
		for (String s : files.keySet()) {
			if (s.contains("_ut") || s.contains("_st"))
				continue;
			ret.add(s);
		}
		for (String s : repository.flimPackages()) {
			if (s.contains("_ut") || s.contains("_st"))
				continue;
			if (!wantedPkgs.contains(s))
				continue;
			ret.add(s);
		}
		this.additionalModulePackages(ret);
		return ret;
	}

	public void generate(ByteCodeEnvironment bce) {
		for (JSFile jsf : files.values()) {
			jsf.generate(bce);
		}
	}

	public Collection<PackageName> packageNames() {
		Iterable<String> strings = packageStrings();
		LinkedHashSet<PackageName> ret = new LinkedHashSet<>();
		for (String s : strings) {
			ret.add(new PackageName(s));
		}
		return ret;
	}

	public Collection<String> packageStrings() {
		Collection<String> flims = repository.flimPackages();
		LinkedHashSet<String> ret = new LinkedHashSet<>();
		if (pkgdag.hasNode("root.package"))
			ret.add("root.package");
		for (String s : files.keySet()) {
			pkgdag.ensure(s);
			pkgdag.postOrderFrom(new NodeWalker<String>() {
				@Override
				public void present(Node<String> node) {
					String pkg = node.getEntry();
					if (files.containsKey(pkg) || flims.contains(pkg))
						ret.add(pkg);
				}
			}, s);
		}

		additionalModulePackages(ret);
		return ret;
	}

	private void additionalModulePackages(LinkedHashSet<String> ret) {
		for (String m : config.modules) {
			File mdir = new File(config.moduleDir, m);
			if (!mdir.isDirectory())
				continue;
			File pf = new File(mdir, "packages");
			if (pf.canRead()) {
				for (String s : FileUtils.readFileAsLines(pf)) {
					ret.add(s);
				}
			}
		}
	}

	@Override
	public Iterable<ContentObject> jsIncludes(String mockOrLive) {
		List<ContentObject> ret = new ArrayList<>();
		if (config.flascklibDir != null) {
			figureJSFilesOnDisk(ret, config, mockOrLive);
		} else if (config.flascklibCPV != null) {
			figureJSFilesFromContentStore(ret, config, mockOrLive);
		}
		return ret;
	}

	private void figureJSFilesOnDisk(List<ContentObject> ret, Configuration config, String mockOrLive) {
		List<String> inlib = new ArrayList<>();
		
		File fljs = new File(config.flascklibDir, "js"); 
		addFrom(ret, inlib, new File(fljs, "core"));
		addFrom(ret, inlib, new File(fljs, mockOrLive));
		
		for (String mld : config.modules) {
			addModule(ret, config.moduleDir, inlib, mld, mockOrLive);
		}

		Iterable<String> pkgs = packageStrings();
		
		for (String s : pkgs) {
			if (mockOrLive.equals("live") && (s.contains("_ut_") || s.contains("_st_")))
				continue;
			ContentObject co = fileFor(s);
			if (co != null) {
				ret.add(co);
				inlib.add(co.key());
			} else {
				boolean added = false;
				File inc = new File(config.writeFlim, s + ".js");
				if (inc.exists()) {
					if (!inlib.contains(inc.getName())) {
						ret.add(new FileContentObject(inc));
						inlib.add(inc.getName());
						added = true;
					}
				}
				for (File q : config.includeFrom) {
					for (File i : FileUtils.findFilesMatching(q, s + ".js")) {
						if (!inlib.contains(i.getName())) {
							ret.add(new FileContentObject(i));
							inlib.add(i.getName());
							added = true;
						}
					}
				}
				if (!added) {
					errors.message((InputPosition)null, "no files could be added for package " + s);
				}
			}
		}
	}

	private void addModule(List<ContentObject> ret, File moduleDir, List<String> inlib, String m, String mockOrLive) {
		File f = new File(moduleDir, m);
		if (!f.isDirectory()) {
			errors.message((InputPosition)null, "there is no module " + m + " defined in " + moduleDir);
		}
		File mjs = new File(f, "js");
		addFrom(ret, inlib, new File(mjs, "core"));
		File importer = new File(mjs, mockOrLive);
		if (!importer.isDirectory()) {
			errors.message((InputPosition)null, "there is no dir " + mockOrLive + " in module " + m + " defined in " + moduleDir);
		}
		addFrom(ret, inlib, importer);
	}
	
	private void addFrom(List<ContentObject> ret, List<String> inlib, File from) {
		if (!from.isDirectory())
			return;
		List<File> library = FileUtils.findFilesMatching(from, "*");
		for (File f : library) {
			ret.add(new FileContentObject(f));
			inlib.add(f.getName());
		}
	}

	private void figureJSFilesFromContentStore(List<ContentObject> ret, Configuration config, String mockOrLive) {
		for (ContentObject co : config.flascklibCPV.corejs()) {
			ret.add(co);
		}
		if (mockOrLive.equals("live")) {
			for (ContentObject co : config.flascklibCPV.livejs()) {
				ret.add(co);
			}
		}
		if (mockOrLive.equals("mock")) {
			for (ContentObject co : config.flascklibCPV.mockjs()) {
				ret.add(co);
			}
		}
		if (config.moduleCOs != null) {
			for (PackageSources d : config.moduleCOs) {
				for (ContentObject co : d.corejs())
					ret.add(co);
				if (mockOrLive.equals("live")) {
					for (ContentObject co : d.livejs())
						ret.add(co);
				}
				if (mockOrLive.equals("mock")) {	
					for (ContentObject co : d.mockjs())
						ret.add(co);
				}
			}
		}
		if (config.dependencies != null) {
			for (PackageSources d : config.dependencies) {
				for (ContentObject co : d.corejs())
					ret.add(co);
				if (mockOrLive.equals("live")) {
					for (ContentObject co : d.livejs())
						ret.add(co);
				}
				if (mockOrLive.equals("mock")) {
					for (ContentObject co : d.mockjs())
						ret.add(co);
				}
			}
		}
		for (ContentObject co : gencos.values()) {
			ret.add(co);
		}
		for (ContentObject co : localOnly) {
			ret.add(co);
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
