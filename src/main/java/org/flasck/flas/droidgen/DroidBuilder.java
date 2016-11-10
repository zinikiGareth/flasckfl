package org.flasck.flas.droidgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.w3c.css.sac.Condition;
import org.w3c.css.sac.ConditionalSelector;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SelectorList;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.exceptions.UtilException;
import org.zinutils.parser.TokenizedLine;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.StringComparator;

import com.gmmapowell.quickbuild.app.BuildOutput;
import com.gmmapowell.quickbuild.app.QuickBuild;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildExecutor;
import com.gmmapowell.quickbuild.build.android.AdbInstallCommand;
import com.gmmapowell.quickbuild.build.android.AdbStartCommand;
import com.gmmapowell.quickbuild.build.android.AndroidCommand;
import com.gmmapowell.quickbuild.build.android.AndroidNature;
import com.gmmapowell.quickbuild.build.android.AndroidRestrictJNICommand;
import com.gmmapowell.quickbuild.build.android.AndroidUseLibraryCommand;
import com.gmmapowell.quickbuild.build.java.JavaNature;
import com.gmmapowell.quickbuild.build.maven.MavenLibraryCommand;
import com.gmmapowell.quickbuild.build.maven.RepoCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.config.LibsCommand;
import com.steadystate.css.dom.CSSStyleRuleImpl;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import com.steadystate.css.parser.selectors.ClassConditionImpl;

public class DroidBuilder {
	public final File androidDir;
	final ByteCodeEnvironment bce;
	final File qbcdir;
	private String launchCard;
	final List<File> libs = new ArrayList<File>();
	final List<String> maven = new ArrayList<String>();
	private String useJack = "";
	private List<String> jnis;
	final List<PackageInfo> packages = new ArrayList<PackageInfo>();
	private Map<String, DroidStyle> cssClasses = new TreeMap<>(new StringComparator());
	private boolean reallyBuild = true;

	public DroidBuilder(File androidDir, ByteCodeEnvironment bce) {
		this.androidDir = androidDir;
		this.bce = bce;
		qbcdir = new File(androidDir, "qbout/classes");
	}

	public void dontBuild() {
		reallyBuild = false;
	}

	public void init() {
		if (androidDir == null)
			return;
		if (!androidDir.exists()) {
			// create a directory structure to put things in
			FileUtils.assertDirectory(androidDir);
			FileUtils.assertDirectory(new File(androidDir, "src"));
			FileUtils.assertDirectory(new File(androidDir, "src/main"));
			FileUtils.assertDirectory(new File(androidDir, "src/main/java"));
			FileUtils.assertDirectory(new File(androidDir, "src/android"));
			FileUtils.assertDirectory(new File(androidDir, "src/android/assets"));
			FileUtils.assertDirectory(new File(androidDir, "src/android/assets/css"));
			FileUtils.assertDirectory(new File(androidDir, "src/android/gen"));
			FileUtils.assertDirectory(new File(androidDir, "src/android/lib"));
			FileUtils.assertDirectory(new File(androidDir, "src/android/res"));
			FileUtils.assertDirectory(new File(androidDir, "src/android/rawapk"));
			FileUtils.assertDirectory(new File(androidDir, "src/android/rawapk/lib"));
			FileUtils.assertDirectory(new File(androidDir, "qbout"));
			FileUtils.assertDirectory(new File(androidDir, "qbout/jill"));
			FileUtils.assertDirectory(new File(androidDir, "qbout/dex"));
			FileUtils.assertDirectory(new File(androidDir, "libs"));
		}
		// Right ... now make this work from the aar ...
//		FileUtils.copyRecursive(new File("/Users/gareth/user/Personal/Projects/Android/qb/libs/lib"), new File(androidDir, "src/android/rawapk/lib"));
		
		
		FileUtils.assertDirectory(qbcdir);
		FileUtils.cleanDirectory(qbcdir);
		// HACK ALERT! This is to pick up the "support library" for FlasckAndroid, which should probably be in a well-known JAR
//		{ // bigger hack - copy as source
//			if (!androidDir.getPath().startsWith("/tmp"))
//				throw new UtilException("You don't want this hack - it will delete your source code");
//			FileUtils.cleanDirectory(new File(androidDir, "src/main/java"));
//			FileUtils.copyRecursive(new File("/Users/gareth/user/Personal/Projects/Android/HelloAndroid/src/main/java"), new File(androidDir, "src/main/java"));
//		}
//		cmd.addToJRR(new File("/Users/gareth/user/Personal/Projects/Android/HelloAndroid/qbout/classes"));
		FileUtils.copyRecursive(new File("/Users/gareth/user/Personal/Projects/Android/HelloAndroid/qbout/classes", "org"), new File(qbcdir, "org"));
		FileUtils.copyRecursive(new File("/Users/gareth/user/Personal/Projects/Android/HelloAndroid", "src/android/assets"), new File(androidDir, "src/android/assets"));
		FileUtils.copyRecursive(new File("/Users/gareth/Ziniki/Code/Tools/QuickBuild/qbout/classes/", "com/gmmapowell/quickbuild/annotations/android/"), new File(qbcdir, "com/gmmapowell/quickbuild/annotations/android/"));
	}
	
	public void cleanFirst() {
		FileUtils.cleanDirectory(androidDir);
		androidDir.delete();
		init();
	}

	public void useJack() {
		useJack = "--jack ";
		// Hack I put in for useJack mode ...
//		FileUtils.copyStreamToFile(new ByteArrayInputStream("class Foo {}".getBytes()), new File("/tmp/chaddyAndroid/src/main/java/Foo.java"));
	}
	
	public void restrictJni(String string) {
		if (jnis == null)
			jnis = new ArrayList<String>();
		jnis.add(string);
	}

	public void setLaunchCard(String launchCard) {
		if (launchCard.contains("/"))
			this.launchCard = launchCard;
		else
			this.launchCard = launchCard.substring(0, launchCard.lastIndexOf(".")) + "/" + launchCard;
	}

	public void useLib(String name) {
		File ul = new File(name);
		if (!ul.canRead())
			throw new UtilException("Cannot read " + ul);
		if (ul.isDirectory()) {
			for (File f : ul.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".jar");
				}
			})) {
				libs.add(f);
			}
		} else if (name.endsWith(".jar") || name.endsWith(".aar"))
			libs.add(ul);
		else
			throw new UtilException("Cannot interpret " + ul + " as a directory, jar or aar");
	}

	public void useMaven(String id) {
		maven.add(id);
	}
	
	public void useCSS(String dir) {
		File f = new File(dir);
		if (!f.canRead())
			throw new UtilException("Cannot copy CSS " + dir + " as it does not exist");
		File cssdir = new File(androidDir, "src/android/assets/css");
		if (f.isDirectory()) {
			for (File q : FileUtils.findFilesMatching(f, "*.css"))
				importCSS(q);
			for (File q : f.listFiles()) {
				if (q.isDirectory())
					FileUtils.copyRecursive(q, cssdir);
				else
					FileUtils.copy(q, new File(cssdir, q.getName()));
			}
		} else
			throw new UtilException("Handle standalone css spec");
	}

	private void importCSS(File q) {
		InputStreamReader isr = null;
		System.out.println("Parsing file " + q);
		try {
			isr = new InputStreamReader(new FileInputStream(q), "UTF-8");
			InputSource is = new InputSource(isr);
			CSSOMParser parser = new CSSOMParser(new SACParserCSS3());
			CSSStyleSheet sheet = parser.parseStyleSheet(is, null, null);
			CSSRuleList rules = sheet.getCssRules();
			for (int i = 0; i < rules.getLength(); i++) {
				CSSRule rule = rules.item(i);
				switch (rule.getType()) {
				case CSSRule.STYLE_RULE: {
					CSSStyleRule sr = (CSSStyleRule) rule;
					SelectorList selectors = ((CSSStyleRuleImpl) sr).getSelectors();
					DroidStyle ds = parseActualStyle(sr.getStyle());
					if (ds == null)
						continue;
					for (int j=0;j<selectors.getLength();j++) {
						Selector item = selectors.item(j);
						switch (item.getSelectorType()) {
						case Selector.SAC_CONDITIONAL_SELECTOR:
							ConditionalSelector cs = (ConditionalSelector) item;
							Condition condition = cs.getCondition();
							switch (condition.getConditionType()) {
							case Condition.SAC_CLASS_CONDITION:
								ClassConditionImpl cci = (ClassConditionImpl) condition;
								cssClasses.put(cci.getValue(), ds);
//								System.out.println("Cond: " + cci.getLocalName() + " " + cci.getValue());
//								if (cci.getLocalName() != null)
//									System.out.println("LocalName = " + cci.getLocalName());
								break;
							default:
								System.out.println("Can't handle condition type " + condition.getConditionType() + " for " + condition);
								break;
							}
							break;
						default:
							System.out.println("Can't handle selector type: " + item.getSelectorType() + " for " + item);
							break;
						}
					}
					break;
				}
				case CSSRule.MEDIA_RULE:
//					System.out.println("Media: " + rule);
					break;
				default:
					System.out.println("Don't handle rule type " + rule.getType() + " for " + rule);
					break;
				}
			}
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		} finally {
			if (isr != null)
				try { isr.close(); } catch (Exception ex) {}
		}
	}

	private DroidStyle parseActualStyle(CSSStyleDeclaration style) {
//		System.out.println("actual style >>> " + style);
		DroidStyle ret = new DroidStyle();
		String val = style.getPropertyValue("x-flasck-type");
		if (val.length() > 0)
			ret.set("x-flasck-type", val);
		
		getMeasurement(ret, style, "height");
		getMeasurement(ret, style, "width");
		if (ret.isEmpty())
			return null;
		return ret;
	}

	protected void getMeasurement(DroidStyle ret, CSSStyleDeclaration style, String prop) {
		String val = style.getPropertyValue(prop);
		if (val.length() > 0)
			ret.set(prop, val); // probably should parse it in some way ...
	}

	public void usePackage(String desc) {
		int eq = desc.indexOf("=");
		if (eq == -1)
			throw new UtilException("Invalid package descriptor: " + desc + " must be local=ziniki:version");
		int colon = desc.indexOf(":", eq);
		if (colon == -1)
			throw new UtilException("Invalid package descriptor: " + desc + " must be local=ziniki:version");
		packages.add(new PackageInfo(desc.substring(0, eq), desc.substring(eq+1, colon), Integer.parseInt(desc.substring(colon+1))));
	}

	public void build() {
		if (!reallyBuild)
			return;
		
		for (Entry<String, DroidStyle> s: cssClasses.entrySet()) {
			System.out.println("Class " + s.getKey() + " of type " + s.getValue().getFlasck("type") + " has height " + s.getValue().get("height"));
		}

		// THIS IS A HACK WHICH IS HERE DELIBERATELY TO STOP BUILDS HAPPENING
//		if (this.androidDir != null)
//			return;
		// REMOVE IT IF YOU WANT ANYTHING TO EVER WORK!!!
		
		
		// there are a number of possibilities here:
		// just build and deploy "in memory"
		// build from a QB file (by name)
		// defer the building
		// for now, just do the "easy and obvious thing", i.e. build this app
		ConfigFactory cf = new ConfigFactory();
		BuildOutput outlog = new BuildOutput(false);
		Config config = new Config(cf, outlog, androidDir, "xx", null);
		/*JavaNature jn = */cf.getNature(config, JavaNature.class);
//		jn.addLib(new File("/Users/gareth/user/Personal/Projects/Android/qb/libs"), new ArrayList<ExcludeCommand>());
		cf.getNature(config, AndroidNature.class);
		QuickBuild.readHomeConfig(config, null);
		RepoCommand rc = new RepoCommand(new TokenizedLine(0, "repo http://files.couchbase.com/maven2"));
		config.addChild(rc);
//		LibsCommand lc = new LibsCommand(new TokenizedLine(0, "libs /Users/gareth/user/Personal/Projects/Android/qb/libs"));
//		config.addChild(lc);
		int setupLineNo = 100;
		int lineNo = 200;
		TokenizedLine toks = new TokenizedLine(lineNo++, "android " + useJack + androidDir.getName());
		AndroidCommand cmd = new AndroidCommand(toks);
		if (jnis != null) {
			StringBuilder jniCmd = new StringBuilder("jni");
			for (String s : jnis)
				jniCmd.append(" " + s);
			cmd.addChild(new AndroidRestrictJNICommand(new TokenizedLine(lineNo++, jniCmd.toString())));
		}
			
		for (String mvn : maven) {
			MavenLibraryCommand mlc = new MavenLibraryCommand(new TokenizedLine(setupLineNo++, "maven " + mvn));
			config.addChild(mlc);
			cmd.addChild(new AndroidUseLibraryCommand(new TokenizedLine(lineNo++, "use " + mvn)));
		}
		for (File f : libs) {
			LibsCommand lc = new LibsCommand(new TokenizedLine(setupLineNo++, "libs " + f.getPath()));
			config.addChild(lc);
//			jn.addLib(f, new ArrayList<ExcludeCommand>());
			cmd.addChild(new AndroidUseLibraryCommand(new TokenizedLine(lineNo++, "use " + f.getPath())));
		}
		config.addChild(cmd);
		String apkName = "qbout/" + androidDir.getName() + ".apk";
		AdbInstallCommand install = new AdbInstallCommand(new TokenizedLine(lineNo++, "adbinstall " + androidDir.getName() + " " + apkName));
		config.addChild(install);
		if (launchCard != null) {
			AdbStartCommand start = new AdbStartCommand(new TokenizedLine(lineNo++, "adbstart " + apkName + " " + launchCard));
			config.addChild(start);
		}
		
		config.done();
		cf.done();

		ArrayList<String> sdf = new ArrayList<String>();
//		sdf.add("Manifest");
//		sdf.add("Dex");
//		sdf.add("Compil");
//		sdf.add("Jack");
//		sdf.add("apk");
//		sdf.add("manbuild");
		BuildContext cxt = new BuildContext(config, cf, outlog, true, true, false, sdf, sdf, false, null, null, false, true, false);
		cxt.configure();

		new BuildExecutor(cxt, false).doBuild();

	}
}
