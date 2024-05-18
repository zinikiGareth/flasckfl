package org.flasck.flas.compiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.flasck.flas.Configuration;
import org.flasck.flas.parsedForm.assembly.ApplicationAssembly;
import org.flasck.flas.parsedForm.assembly.Assembly;
import org.flasck.flas.repository.AssemblyVisitor;
import org.flasck.jvm.assembly.CardInitializer;
import org.flasck.jvm.assembly.FLASAssembler;
import org.flasck.jvm.ziniki.ContentObject;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.utils.FileUtils;

public class CompilerAssembler implements AssemblyVisitor {
	private final Configuration config;
	private final FLASAssembler asm;
	private final File jsdir;
	private final File cssdir;
	private List<String> inits = new ArrayList<>();
	private List<String> css = new ArrayList<>();
	private List<String> js = new ArrayList<>();
	private List<ContentObject> temps = new ArrayList<>();

	public CompilerAssembler(Configuration config, FLASAssembler asm, File todir) {
		this.config = config;
		this.asm = asm;
		
		jsdir = new File(todir, "js");
		cssdir = new File(todir, "css");

		FileUtils.cleanDirectory(jsdir);
		FileUtils.assertDirectory(jsdir);
		FileUtils.cleanDirectory(cssdir);
		FileUtils.assertDirectory(cssdir);

	}

	@Override
	public void visitAssembly(Assembly a) {
	}

	@Override
	public void visitResource(String name, ZipInputStream zis) throws IOException {
	}

	@Override
	public void visitPackage(String pkg) {
		inits.add(pkg);
	}

	@Override
	public void uploadJar(ByteCodeEnvironment bce, String s) {
	}

	@Override
	public void includePackageFile(ContentObject co) {
		js.add(copyCOToAppFile("js", co));
	}

	private String copyCOToAppFile(String dir, ContentObject co) {
		String url = co.url();
		URI uri = URI.create(url);
		if (uri.getScheme().equals("file")) {
			File f = new File(uri.getPath());
			File to = new File(jsdir, f.getName());
			FileUtils.copyStreamToFile(co.asStream(), to);
			url = "/" + dir + "/" + f.getName();
		} else if (uri.getScheme().equals("memory")) {
			File to = new File(jsdir, co.key());
			FileUtils.copyStreamToFile(co.asStream(), to);
			url = "/" + dir + "/" + co.key();
		} else if (uri.getScheme().equals("https")) {
			File to = new File(jsdir, co.key());
			FileUtils.copyStreamToFile(co.asStream(), to);
			url = "/" + dir + "/" + co.key();
		} else {
			throw new NotImplementedException("how do I include package file " + url);
		}
		return url;
	}

	@Override
	public void visitCardTemplate(String cardName, InputStream is, long length) throws IOException {
		String s = FileUtils.readNStream(length, is);
		ContentObject co = new ContentObject() {
			@Override
			public String key() {
				return null;
			}

			@Override
			public String url() {
				return null;
			}
			
			@Override
			public long length() {
				return 0;
			}
			
			@Override
			public String writeUrl() {
				return null;
			}

			@Override
			public byte[] asByteArray() {
				return null;
			}

			@Override
			public InputStream asStream() {
				return null;
			}

			@Override
			public String asString() {
				return "    <template id='" + cardName + "'>\n" + s + "\n    </template>\n";
			}
		};
		temps.add(co);
	}

	@Override
	public void visitCSS(String name, ZipInputStream zis, long length) throws IOException {
		File to = new File(cssdir, name);
		FileUtils.copyStreamToFileWithoutClosing(zis, to);
		css.add(name);
	}

	@Override
	public void leaveAssembly(Assembly a) throws IOException {
		ApplicationAssembly aa = (ApplicationAssembly) a;
		asm.begin();
		asm.title(aa.getTitle());
		asm.afterTitle();
		for (ContentObject co : temps)
			asm.templates(co);
		asm.beginCss();
		for (String c : css)
			asm.css(config.inclPrefix + "/css/" + c);
		asm.endCss();
		if (config.inclPrefix != null && config.inclPrefix.length() > 0) {
			asm.beginImportMap();
			for (String j : js)
				asm.mapJavascript(j, config.inclPrefix + j);
			asm.endImportMap();
		}
		asm.beginJs();
		FLASCompiler.logger.info("assembly has " + js);
		for (String j : js)
			asm.javascript(config.inclPrefix + /* "js" + */j);
		asm.endJs();
		asm.endHead();
		asm.beginInit();
		asm.initializer(new CardInitializer() {
			@Override
			public Iterable<String> packages() {
				return inits;
			}

			@Override
			public String packageName() {
				return aa.name().uniqueName();
			}
		});
		asm.endInit();
		asm.end();
	}

	@Override
	public void traversalDone() throws Exception {
	}
}
