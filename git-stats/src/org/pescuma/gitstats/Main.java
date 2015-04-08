package org.pescuma.gitstats;

import static java.lang.Math.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.pescuma.datatable.DataTable;
import org.pescuma.datatable.DataTable.Value;
import org.pescuma.datatable.DataTableSerialization;
import org.pescuma.datatable.MemoryDataTable;
import org.pescuma.gitstats.ColumnsOutput.Align;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

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
						if (normalizePath(file).startsWith(excluded))
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
			loaded = loaded.map(Consts.COL_AUTHOR, new Function<String, String>() {
				@Override
				public String apply(String author) {
					String alternateName = authorMappings.get(author);
					if (alternateName != null)
						return alternateName;
					else
						return author;
				}
			});
		}
		
		data.inc(loaded);
	}
	
	private static void outputStatsToCSV(DataTable data, String output) {
		System.out.println("Writing output to " + output);
		
		DataTableSerialization.saveAsCSV(data, new File(output), false);
		
		System.out.println();
	}
	
	private static void outputStatsToConsole(DataTable data, final Args args) {
		
		double totalLines = data.sum();
		
		System.out.println("Authors:");
		ColumnsOutput out = new ColumnsOutput();
		for (String author : sortedByLines(data, Consts.COL_AUTHOR)) {
			if (author.isEmpty())
				continue;
			
			DataTable authorData = data.filter(Consts.COL_AUTHOR, author);
			String[] months = getMonthRange(authorData);
			double authorLines = authorData.sum();
			
			out.appendColumn("   ").appendColumn(author).appendColumn(" : ")
					.appendColumn(Align.Right, "%.1f%%", percent(authorLines, totalLines))
					.appendColumn(" of the lines: ");
			
			appendLines(out, authorData, authorLines);
			
			out.appendColumn(" in ").appendColumn(authorData.getDistinct(Consts.COL_COMMIT).size())
					.appendColumn(" commits from ").appendColumn(months[0]).appendColumn(" to ")
					.appendColumn(months[1]);
			
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
				
				out.newLine();
			}
		}
		out.print(System.out);
		System.out.println();
		
		System.out.println("Months:");
		out = new ColumnsOutput();
		for (String month : data.getDistinct(Consts.COL_MONTH)) {
			if (month.isEmpty())
				continue;
			
			DataTable monthData = data.filter(Consts.COL_MONTH, month);
			
			out.appendColumn("   ").appendColumn(month).appendColumn(" : ");
			
			appendLines(out, monthData);
			
			out.appendColumn(" in ").appendColumn(monthData.getDistinct(Consts.COL_COMMIT).size())
					.appendColumn(" commits");
			
			out.newLine();
		}
		out.print(System.out);
		System.out.println();
		
		System.out.println("Languages:");
		out = new ColumnsOutput();
		for (String language : sortedByLines(data, Consts.COL_LANGUAGE)) {
			DataTable languageData = data.filter(Consts.COL_LANGUAGE, language);
			String[] months = getMonthRange(languageData);
			long unblamable = round(languageData.filter(Consts.COL_AUTHOR, "").sum());
			
			out.appendColumn("   ").appendColumn(language).appendColumn(" : ");
			appendLines(out, languageData);
			out.appendColumn(" in ").appendColumn(languageData.getDistinct(Consts.COL_COMMIT).size())
					.appendColumn(" commits from ").appendColumn(months[0]).appendColumn(" to ")
					.appendColumn(months[1]);
			
			if (unblamable > 0)
				out.appendColumn(" (").appendColumn((int) unblamable).appendColumn(" umblamable)");
			
			out.newLine();
		}
		out.print(System.out);
		System.out.println();
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
	
	private static double percent(double count, double total) {
		return count * 100 / total;
	}
	
	private static String[] getMonthRange(DataTable authorData) {
		List<String> result = new ArrayList<String>(authorData.getDistinct(Consts.COL_MONTH));
		result.remove("");
		if (result.size() < 1)
			return new String[] { "unknown", "unknown" };
		else
			return new String[] { result.get(0), result.get(result.size() - 1) };
	}
	
	private static List<String> sortedByLines(DataTable data, int col) {
		final Map<String, Value> authorAndLines = data.sumDistinct(col);
		
		List<String> sorted = new ArrayList<String>(authorAndLines.keySet());
		Collections.sort(sorted, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return (int) (authorAndLines.get(o2).value - authorAndLines.get(o1).value);
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
