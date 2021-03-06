package org.pescuma.gitstats;

import static java.lang.Math.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.pescuma.datatable.DataTable;
import org.pescuma.datatable.DataTable.Line;
import org.pescuma.datatable.DataTableSerialization;
import org.pescuma.datatable.MemoryDataTable;
import org.pescuma.datatable.func.Function2;
import org.pescuma.gitstats.ColumnsOutput.Align;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.io.Files;

public class Main {
	
	public static void main(String[] args) throws IOException, GitAPIException, InterruptedException {
		disableLogger();
		
		Args parsedArgs = new Args();
		CmdLineParser parser = new CmdLineParser(parsedArgs);
		try {
			
			parser.parseArgument(args);
			
		} catch (CmdLineException e) {
			System.out.println(e.getMessage());
			parser.printUsage(System.out);
			System.out.println();
			System.exit(-1);
		}
		
		if (parsedArgs.showHelp) {
			System.out.println("git stats [options...] arguments...");
			parser.printUsage(System.out);
			System.out.println();
			return;
		}
		
		System.exit(run(parsedArgs));
	}
	
	private static void disableLogger() {
		LogManager.getLogManager().reset();
	}
	
	private static int run(Args args) throws IOException, GitAPIException, InterruptedException {
		args.applyDefaults();
		
		DataTable data = new MemoryDataTable();
		
		for (File path : args.paths) {
			if (!path.exists())
				System.err.println("File/folder not found: " + path);
			
			else if (path.isFile() && path.getName().endsWith(".csv"))
				loadFromCSV(data, args, path);
			
			else
				RepositoryProcessor.process(data, args, path);
		}
		
		System.out.println();
		
		if (data.isEmpty()) {
			System.out.println("No data available");
			return -1;
		}
		
		for (String output : args.outputs) {
			if (Args.isConsole(output))
				outputStatsToConsole(data, args);
			
			else if (output.endsWith(".csv"))
				outputStatsToCSV(data, output);
			
			else if (output.endsWith(".htm") || output.endsWith(".html"))
				outputStatsToHTML(data, output);
			
			else
				System.out.println("Unknown output format: " + output);
		}
		
		return 0;
	}
	
	private static void loadFromCSV(DataTable data, final Args args, File file) {
		System.out.println("Loading " + file.getAbsolutePath() + "...");
		
		DataTable loaded = new MemoryDataTable();
		DataTableSerialization.loadFromCSV(loaded, file);
		
		if (!args.excludedPaths.isEmpty()) {
			final List<String> excludedPaths = preProcessExcludedPaths(args);
			loaded = loaded.filter(Consts.COL_FILE, new Predicate<String>() {
				@Override
				public boolean apply(String file) {
					for (String excluded : excludedPaths) {
						if (file.startsWith(excluded))
							return false;
					}
					return true;
				}
			});
		}
		
		if (!args.ignoredRevisions.isEmpty()) {
			loaded = loaded.filter(Consts.COL_COMMIT, new Predicate<String>() {
				@Override
				public boolean apply(String commit) {
					for (String rev : args.ignoredRevisions) {
						if (StringUtils.startsWithIgnoreCase(commit, rev))
							return false;
					}
					return true;
				}
			});
		}
		
		if (!args.authors.isEmpty()) {
			final Map<String, String> authorMappings = args.getAuthorMappings();
			loaded = loaded.mapColumn(Consts.COL_AUTHOR, new Function<String, String>() {
				@Override
				public String apply(String author) {
					String newAuthor = authorMappings.get(author);
					
					if (newAuthor != null)
						return newAuthor;
					else
						return author;
				}
			});
		}
		
		if (!args.languages.isEmpty()) {
			final Map<String, String> languageMappings = args.getLanguageMappings();
			loaded = loaded.mapColumn(Consts.COL_LANGUAGE, new Function2<String, String, Line>() {
				@Override
				public String apply(String language, Line line) {
					String extension = FilenameUtils.getExtension(line.getColumn(Consts.COL_FILE));
					String newLanguage = languageMappings.get(extension);
					
					if (newLanguage != null)
						return newLanguage;
					else
						return language;
				}
			});
		}
		
		data.inc(loaded);
	}
	
	private static void outputStatsToCSV(DataTable data, String output) {
		System.out.println("Writing CSV output to " + output);
		
		DataTableSerialization.saveAsCSV(data, new File(output), false);
		
		System.out.println();
	}
	
	private static void outputStatsToHTML(DataTable data, String output) throws IOException {
		System.out.println("Writing HTML output to " + output);
		
		StringBuilder lines = new StringBuilder();
		for (Line line : data.getLines()) {
			lines.append("        data.add(").append(line.getValue());
			for (String col : line.getColumns())
				lines.append(", '").append(col.replace("'", "\\'")).append("'");
			lines.append(");\n");
		}
		
		String html = readIndexHtml();
		html = html.replace("$$$date$$$", DateFormat.getDateTimeInstance().format(new Date()));
		html = html.replace("$$$version$$$", getVersion());
		html = html.replace("$$$data$$$", lines.toString());
		
		Files.write(html, new File(output), Charset.forName("UTF-8"));
		
		System.out.println();
	}
	
	private static String getVersion() {
		String version = Main.class.getPackage().getImplementationVersion();
		if (version == null)
			version = "devel";
		return version;
	}
	
	private static String readIndexHtml() throws IOException {
		InputStream in = Main.class.getResourceAsStream("/org/pescuma/gitstats/export/index.html");
		try {
			return IOUtils.toString(in, "UTF-8");
		} finally {
			in.close();
		}
	}
	
	private static void outputStatsToConsole(DataTable data, final Args args) {
		
		double totalLines = data.sum();
		
		System.out.println("Total:");
		ColumnsOutput out = new ColumnsOutput();
		{
			out.appendColumn("   ");
			appendLines(out, data);
			appendFiles(out, data);
			appendLanguages(out, data);
			appendCommits(out, data);
			appendAuthors(out, data);
			appendMonths(out, data);
			appendUnblamable(out, data);
		}
		out.print(System.out);
		System.out.println();
		
		System.out.println("Authors:");
		out = new ColumnsOutput();
		for (String author : sortByLines(data, Consts.COL_AUTHOR)) {
			if (author.isEmpty())
				continue;
			
			DataTable authorData = data.filter(Consts.COL_AUTHOR, author);
			double authorLines = authorData.sum();
			
			out.appendColumn("   ").appendColumn(author).appendColumn(" : ")
					.appendColumn(Align.Right, "%.1f%%", percent(authorLines, totalLines))
					.appendColumn(" of the lines: ");
			appendLines(out, authorData, authorLines);
			appendFiles(out, authorData);
			appendLanguages(out, authorData);
			appendCommits(out, authorData);
			appendMonths(out, authorData);
			
			out.newLine();
		}
		{
			DataTable unblamableData = data.filter(Consts.COL_AUTHOR, "");
			double unblamableLines = unblamableData.sum();
			if (unblamableLines > 0) {
				out.appendColumn("   ").appendColumn("Unblamable lines").appendColumn(" : ")
						.appendColumn(Align.Right, "%.1f%%", percent(unblamableLines, totalLines))
						.appendColumn(" of the lines: ");
				
				appendLines(out, unblamableData, unblamableLines);
				appendFiles(out, unblamableData);
				appendLanguages(out, unblamableData);
				
				out.newLine();
			}
		}
		out.print(System.out);
		System.out.println();
		
		System.out.print("Months: ");
		out = new ColumnsOutput();
		List<Double> perMonthLines = new ArrayList<Double>();
		for (String month : sortByText(data.getDistinct(Consts.COL_MONTH))) {
			if (month.isEmpty()) {
				perMonthLines.add(0d);
				continue;
			}
			
			DataTable monthData = data.filter(Consts.COL_MONTH, month);
			double monthLines = monthData.sum();
			
			perMonthLines.add(monthLines);
			
			out.appendColumn("   ").appendColumn(month).appendColumn(" : ");
			appendLines(out, monthData, monthLines);
			appendFiles(out, monthData);
			appendLanguages(out, monthData);
			appendCommits(out, monthData);
			appendAuthors(out, monthData);
			
			out.newLine();
		}
		System.out.println(Sparkline.getSparkline(perMonthLines));
		out.print(System.out);
		System.out.println();
		
		System.out.println("Languages:");
		out = new ColumnsOutput();
		for (String language : sortByLines(data, Consts.COL_LANGUAGE)) {
			DataTable languageData = data.filter(Consts.COL_LANGUAGE, language);
			
			out.appendColumn("   ").appendColumn(language).appendColumn(" : ");
			appendLines(out, languageData);
			appendFiles(out, languageData);
			appendCommits(out, languageData);
			appendAuthors(out, languageData);
			appendMonths(out, languageData);
			appendUnblamable(out, languageData);
			
			out.newLine();
		}
		out.print(System.out);
		System.out.println();
	}
	
	private static void appendLanguages(ColumnsOutput out, DataTable data) {
		out.appendColumn(" in ").appendColumn(data.getDistinct(Consts.COL_LANGUAGE).size()).appendColumn(" languages");
	}
	
	private static void appendFiles(ColumnsOutput out, DataTable data) {
		out.appendColumn(" in ").appendColumn(data.getDistinct(Consts.COL_FILE).size()).appendColumn(" files");
	}
	
	private static void appendLines(ColumnsOutput out, DataTable data) {
		appendLines(out, data, data.sum());
	}
	
	private static void appendLines(ColumnsOutput out, DataTable data, double total) {
		out.appendColumn((int) total).appendColumn(" lines (")
				.appendColumn((int) data.filter(Consts.COL_LINE_TYPE, Consts.CODE).sum()).appendColumn(" code, ")
				.appendColumn((int) data.filter(Consts.COL_LINE_TYPE, Consts.COMMENT).sum()).appendColumn(" comment, ")
				.appendColumn((int) data.filter(Consts.COL_LINE_TYPE, Consts.EMPTY).sum()).appendColumn(" empty)");
	}
	
	private static void appendCommits(ColumnsOutput out, DataTable data) {
		out.appendColumn(" in ").appendColumn(data.getDistinct(Consts.COL_COMMIT).size()).appendColumn(" commits");
	}
	
	private static void appendAuthors(ColumnsOutput out, DataTable data) {
		out.appendColumn(" by ").appendColumn(data.getDistinct(Consts.COL_AUTHOR).size()).appendColumn(" authors");
	}
	
	private static void appendMonths(ColumnsOutput out, DataTable data) {
		String[] months = getMonthRange(data);
		
		out.appendColumn(" from ").appendColumn(months[0]).appendColumn(" to ").appendColumn(months[1]);
	}
	
	private static void appendUnblamable(ColumnsOutput out, DataTable data) {
		long unblamable = round(data.filter(Consts.COL_AUTHOR, "").sum());
		if (unblamable > 0)
			out.appendColumn(" (").appendColumn((int) unblamable).appendColumn(" umblamable)");
	}
	
	private static double percent(double count, double total) {
		return count * 100 / total;
	}
	
	private static String[] getMonthRange(DataTable authorData) {
		List<String> result = new ArrayList<String>(authorData.getDistinct(Consts.COL_MONTH));
		result.remove("");
		Collections.sort(result);
		
		if (result.size() < 1)
			return new String[] { "unknown", "unknown" };
		else
			return new String[] { result.get(0), result.get(result.size() - 1) };
	}
	
	private static List<String> sortByText(Collection<String> data) {
		List<String> result = new ArrayList<String>(data);
		Collections.sort(result);
		return result;
	}
	
	private static List<String> sortByLines(DataTable data, int col) {
		final DataTable authorAndLines = data.groupBy(col);
		
		List<String> sorted = new ArrayList<String>(authorAndLines.getColumn(0));
		Collections.sort(sorted, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return (int) (authorAndLines.get(o2) - authorAndLines.get(o1));
			}
		});
		return sorted;
	}
	
	private static List<String> preProcessExcludedPaths(Args args) {
		List<String> result = new ArrayList<String>();
		
		for (String path : args.excludedPaths)
			result.add(normalizePath(path));
		
		return result;
	}
	
	private static String normalizePath(String path) {
		String result = path.replace('\\', '/');
		
		if (result.startsWith("/"))
			result = result.substring(1);
		
		if (!result.endsWith("/"))
			result += "/";
		
		return result;
	}
	
}
