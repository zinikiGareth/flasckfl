package org.flasck.flas.compiler.assembler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipInputStream;

import org.flasck.flas.parsedForm.assembly.ApplicationAssembly;
import org.flasck.flas.parsedForm.assembly.Assembly;
import org.flasck.flas.repository.AssemblyLeaves;
import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.assembly.CardInitializer;
import org.flasck.jvm.ziniki.ContentObject;
import org.ziniki.deployment.fl.JVMApplicationAssembly;
import org.ziniki.deployment.fl.JVMCardInitializer;
import org.ziniki.deployment.fl.JVMPackageInfo;
import org.zinutils.utils.FileUtils;

public abstract class BuildApplicationAssembly extends AssemblyLeaves {
	private final ByteArrayOutputStream templates = new ByteArrayOutputStream();
	private final Map<String, ContentObject> assets = new TreeMap<>();
	private final PrintWriter tpw = new PrintWriter(templates);
	protected String aname;
	private JVMApplicationAssembly store;
	private List<ContentObject> jslibs = new ArrayList<>();;
	private List<ContentObject> cssfiles = new ArrayList<>();;
	private List<CardInitializer> inits = new ArrayList<>();
	protected final FLEvalContext cx;

	public BuildApplicationAssembly(FLEvalContext cx) {
		this.cx = cx;
	}

	@Override
	public void visitAssembly(ApplicationAssembly a) {
		aname = a.name().uniqueName();
		
		store = new JVMApplicationAssembly(cx);
		store.set("title", a.getTitle());
		store.set("assets", assets);
		
		ArrayList<Object> packages = new ArrayList<>();
		store.set("packages", packages);
		
		JVMPackageInfo pi = new JVMPackageInfo(cx);
		packages.add(pi);

		pi.set("javascript", jslibs);
		pi.set("css", cssfiles);
		pi.set("inits", inits);
		
		JVMCardInitializer init = new JVMCardInitializer(cx);
//		init.addPackage(a.name().container().uniqueName());
		init.mainCard(a.mainCard());
		inits.add(init);
	}
	
	@Override
	public void compiledPackageFile(File f) {
		ContentObject co = upload(f.getName(), f, "text/javascript; encoding=utf-8");
		jslibs.add(co);
	}

	@Override
	public void visitPackage(String pkg) {
		JVMCardInitializer init = (JVMCardInitializer) inits.get(0);
		init.addPackage(pkg);
	}
	
	@Override
	public void visitCardTemplate(String cardName, InputStream is, long length) throws IOException {
		tpw.println("    <template id='" + cardName + "'>");
		tpw.flush();
		FileUtils.copyStreamWithoutClosingEither(is, templates);
		tpw.println("\n    </template>");
		tpw.flush();
	}

	@Override
	public void visitCSS(String name, ZipInputStream zis, long length) throws IOException {
		byte[] bs = FileUtils.readAllStream(zis);
		ContentObject co = upload(name, new ByteArrayInputStream(bs), bs.length, false, "text/css");
		cssfiles.add(co);
		assets.put(name, co);
	}


	@Override
	public void visitResource(String name, ZipInputStream zis) throws IOException {
		byte[] bs = zis.readAllBytes();
		ContentObject co = upload(name, new ByteArrayInputStream(bs), bs.length, false, "application/octet-stream");
		assets.put(name, co);
	}

	@Override
	public void leaveAssembly(Assembly a) throws IOException {
		// TODO: this should only be done for ApplicationAssembly objects
		tpw.flush();
		byte[] bs = templates.toByteArray();
		if (bs.length > 0) {
			ContentObject co = upload("templates", new ByteArrayInputStream(bs), bs.length, false, "text/html");
			store.set("templates", co);
		}
		save(store);
		store = null;
		jslibs = null;
	}
	

	protected abstract ContentObject upload(String name, File f, String ctype);
	protected abstract ContentObject upload(String name, InputStream byteArrayInputStream, long length, boolean b, String string) throws IOException;
	protected abstract void save(org.flasck.jvm.assembly.ApplicationAssembly assembly);

	@Override
	public void traversalDone() throws Exception {
	}
}
