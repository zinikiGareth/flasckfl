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
import org.zinutils.utils.FileUtils;

public class CompilerAssembler implements AssemblyVisitor {
	private final Configuration config;
	private final FLASAssembler asm;
	private final File jsdir;
	private List<String> inits = new ArrayList<>();
	private List<String> css = new ArrayList<>();
	private List<String> js = new ArrayList<>();
	private List<ContentObject> temps = new ArrayList<>();

	public CompilerAssembler(Configuration config, FLASAssembler asm, File todir) {
		this.config = config;
		this.asm = asm;
		
		jsdir = new File(todir, "js");
		FileUtils.cleanDirectory(jsdir);
		FileUtils.assertDirectory(jsdir);

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
		String url = co.url();
//				TODO: copy files if they are file;//
		URI uri = URI.create(url);
		if (uri.getScheme().equals("file")) {
			File f = new File(uri.getPath());
			File to = new File(jsdir, f.getName());
			FileUtils.copyStreamToFile(co.asStream(), to);
			url = "/js/" + f.getName();
			/*
			url= remap.get(url);
			if (url == null)
				throw new CantHappenException("should remap all files now");
			/*
				url = tmp;
			else if (url.startsWith("file://" + config.jsDir().getPath())) {
				url = url.substring(7 + config.jsDir().getPath().length());
				url = url.replaceAll("^/*", "");
				FileUtils.copyStreamToFile(co.asStream(), new File(outdir, url));
				url = config.inclPrefix + "js/" + url;
			}
			*/
		}
		js.add(url);
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
			asm.css(config.inclPrefix + "css/" + c);
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
	
	/*
	private void copyJSLib(Map<File, File> reloc, File userDir, File pf, File jsdir, File libroot) {
		List<File> library = FileUtils.findFilesMatching(libroot, "*.js");
		for (File f : library) {
			File to = new File(jsdir, f.getName());
			FileUtils.copy(f, to);
			reloc.put(absWith(userDir, f), to);
		}
	}

	private File absWith(File userDir, File f) {
		if (f.isAbsolute())
			return f;
		return new File(userDir, f.getPath());
	}
	*/
}
