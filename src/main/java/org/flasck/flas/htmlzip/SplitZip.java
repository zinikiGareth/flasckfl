package org.flasck.flas.htmlzip;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SplitZip {
	public static void main(String[] argv) throws IOException {
		SplitZip sz = new SplitZip();
		sz.split(new StdoutSink(), new File("/Users/gareth/Downloads/demokratizatsiya.webflow.zip"));
	}
	
	public void split(Sink sink, File fromZip) throws IOException {
		System.out.println("Unzipping " + fromZip + " to " + sink + " at " + new Date());
		Splitter splitter = new Splitter(sink);
		ZipFile zf = null;
		try {
			zf = new ZipFile(fromZip);
			Enumeration<? extends ZipEntry> entries = zf.entries();
			while (entries.hasMoreElements()) {
				ZipEntry ze = entries.nextElement();
				if (ze.isDirectory())
					continue;
				splitter.extract(zf, ze);
			}
		} finally {
			if (zf != null)
				try { zf.close(); } catch (Exception ex) { ex.printStackTrace(); }
		}
	}
}
