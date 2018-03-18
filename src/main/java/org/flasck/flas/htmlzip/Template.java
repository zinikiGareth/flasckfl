package org.flasck.flas.htmlzip;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Template {
	public class Case {
		private final File tempFile;
		private final Set<String[]> conds;

		public Case(File tempFile, Set<String[]> conds) {
			this.tempFile = tempFile;
			this.conds = conds;
		}

		public void toString(StringBuilder sb) {
			sb.append("'templates/");
			sb.append(tempFile.getName());
			sb.append("' => [");
			String sep = "";
			for (String[] c : conds) {
				sb.append(sep);
				sep = ",";
				sb.append("['");
				sb.append(c[0]);
				sb.append("','");
				sb.append(c[1]);
				sb.append("']");
			}
			sb.append("]");
		}
	}

	private final String id;
	private final List<Case> cases = new ArrayList<>();

	public Template(String id) {
		this.id = id;
	}

	public File addCase(File templates, Set<String[]> conds) {
		int cnt = cases.size()+1;
		File tempFile = new File(templates, id + "-" + cnt + ".php");
		Case c = new Case(tempFile, conds);
		cases.add(c);

		return tempFile;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		String sep = "";
		for (Case c : cases) {
			sb.append(sep);
			sep = ",";
			c.toString(sb);
		}
		sb.append("]");
		return sb.toString();
	}
}
