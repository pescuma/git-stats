package org.pescuma.gitstats;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class Args {
	
	@Option(name = "--help", aliases = { "-h" }, help = true, hidden = true)
	public boolean showHelp = false;
	
	@Argument(required = false, usage = "Path(s) with git repository or a previously saved file. If not expecified, it uses the current directory.")
	public List<File> paths = new ArrayList<File>();
	
	@Option(name = "--threads", aliases = { "-t" }, usage = "Number of threads (by default it creates one thread per processor)")
	public int threads;
	
	@Option(name = "--ignore-rev", aliases = { "-i" }, usage = "Revision to ignore (can be used multiple times)")
	public List<String> ignoredRevisions = new ArrayList<String>();
	
	@Option(name = "--author", aliases = { "-a" }, usage = "Authors mapping, in the format loginname=Joe User (can be used multiple times)")
	public List<String> authors = new ArrayList<String>();
	
	@Option(name = "--output", aliases = { "-o" }, usage = "How to show output. It can be console or a file name. The format is based on its extension. Supported extensions: csv (can be used multiple times)")
	public List<String> outputs = new ArrayList<String>();
	
	@Option(name = "--exclude-path", aliases = { "-ep" }, usage = "Exclude from process all files inside this path (can be used multiple times)")
	public List<String> excludedPaths = new ArrayList<String>();
	
	@Option(name = "--add-language", usage = "Add a language, based on a file extension, in the format ext=Language (can be used multiple times)")
	public List<String> languages = new ArrayList<String>();
	
	void applyDefaults() {
		if (paths.isEmpty())
			paths.add(new File("."));
		
		for (int i = 0; i < paths.size(); i++)
			paths.set(i, getCanonical(paths.get(i)));
		
		if (outputs.isEmpty())
			outputs.add("console");
		
		for (int i = 0; i < outputs.size(); i++) {
			String output = outputs.get(i);
			if (!isConsole(output))
				outputs.set(i, getCanonical(outputs.get(i)));
		}
		
		if (threads < 1) {
			threads = Runtime.getRuntime().availableProcessors();
			if (threads >= 4)
				// Some room for breathing
				threads--;
		}
	}
	
	public Map<String, String> getAuthorMappings() {
		return split(authors);
	}
	
	public Map<String, String> getLanguageMappings() {
		return split(languages);
	}
	
	private Map<String, String> split(List<String> lines) {
		Map<String, String> result = new HashMap<String, String>();
		
		for (String am : lines) {
			int pos = am.indexOf('=');
			result.put(am.substring(0, pos).trim(), am.substring(pos + 1).trim());
		}
		
		return result;
	}
	
	public static File getCanonical(File file) {
		try {
			return file.getCanonicalFile();
		} catch (IOException e) {
			return file.getAbsoluteFile();
		}
	}
	
	public static String getCanonical(String file) {
		try {
			return new File(file).getCanonicalPath();
		} catch (IOException e) {
			return new File(file).getAbsolutePath();
		}
	}
	
	public static boolean isConsole(String output) {
		return output.equalsIgnoreCase("console");
	}
	
}
