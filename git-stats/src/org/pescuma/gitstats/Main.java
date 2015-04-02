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

import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.pescuma.datatable.DataTable;
import org.pescuma.datatable.DataTable.Value;
import org.pescuma.datatable.DataTableSerialization;
import org.pescuma.datatable.MemoryDataTable;

public class Main {
	
	public static void main(String[] args) throws IOException, GitAPIException,
			InterruptedException {
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
			System.out.println("Processing " + path.getAbsolutePath());
			
			if (path.isFile() && path.getName().endsWith(".csv"))
				DataTableSerialization.loadFromCSV(data, path);
			else
				RepositoryProcessor.process(data, args, path);
		}
		
		System.out.println();
		
		for (String output : args.outputs) {
			if (Args.isConsole(output))
				outputStatsToConsole(data);
			
			else if (output.endsWith(".csv"))
				outputStatsToCSV(data, output);
			else
				System.out.println("Unknown output format: " + output);
		}
		
		return 0;
	}
	
	private static void outputStatsToCSV(DataTable data, String output) {
		System.out.println("Writing output to " + output);
		
		DataTableSerialization.saveAsCSV(data, new File(output), false);
		
		System.out.println();
	}
	
	private static void outputStatsToConsole(DataTable data) {
		double totalLines = data.sum();
		
		System.out.println("Authors:");
		for (String author : sortedByLines(data, Consts.COL_AUTHOR)) {
			if (author.isEmpty())
				continue;
			
			DataTable authorData = data.filter(Consts.COL_AUTHOR, author);
			String[] months = getMonthRange(authorData);
			double authorLines = authorData.sum();
			
			System.out
					.println(String
							.format("   %s : %.1f%% of the lines: %.0f lines (%.0f code, %.0f comment, %.0f empty) in %d commits from %s to %s", //
									author, //
									percent(authorLines, totalLines), //
									authorLines, //
									authorData.filter(Consts.COL_LINE_TYPE, Consts.CODE).sum(), //
									authorData.filter(Consts.COL_LINE_TYPE, Consts.COMMENT).sum(), //
									authorData.filter(Consts.COL_LINE_TYPE, Consts.EMPTY).sum(), //
									authorData.getDistinct(Consts.COL_COMMIT).size(), //
									months[0], //
									months[1]));
		}
		{
			DataTable unblamableData = data.filter(Consts.COL_AUTHOR, "");
			double unblamableLines = unblamableData.sum();
			if (unblamableLines > 0) {
				System.out
						.println(String
								.format("   Unblamable lines : %.1f%% of the lines: %.0f lines (%.0f code, %.0f comment, %.0f empty)",
										percent(unblamableLines, totalLines), //
										unblamableLines, //
										unblamableData.filter(Consts.COL_LINE_TYPE, Consts.CODE)
												.sum(), //
										unblamableData.filter(Consts.COL_LINE_TYPE, Consts.COMMENT)
												.sum(), //
										unblamableData.filter(Consts.COL_LINE_TYPE, Consts.EMPTY)
												.sum()));
			}
		}
		
		System.out.println();
		System.out.println("Months:");
		for (String month : data.getDistinct(Consts.COL_MONTH)) {
			if (month.isEmpty())
				continue;
			
			DataTable monthData = data.filter(Consts.COL_MONTH, month);
			double monthLines = monthData.sum();
			
			System.out.println(String.format(
					"   %s : %.0f lines (%.0f code, %.0f comment, %.0f empty) in %d commits", //
					month, //
					monthLines, //
					monthData.filter(Consts.COL_LINE_TYPE, Consts.CODE).sum(), //
					monthData.filter(Consts.COL_LINE_TYPE, Consts.COMMENT).sum(), //
					monthData.filter(Consts.COL_LINE_TYPE, Consts.EMPTY).sum(),//
					monthData.getDistinct(Consts.COL_COMMIT).size()));
		}
		
		System.out.println();
		System.out.println("Languages:");
		for (String language : sortedByLines(data, Consts.COL_LANGUAGE)) {
			DataTable languageData = data.filter(Consts.COL_LANGUAGE, language);
			String[] months = getMonthRange(languageData);
			double languageLines = languageData.sum();
			long unblamable = round(languageData.filter(Consts.COL_AUTHOR, "").sum());
			
			System.out.println(String.format(
					"   %s : %.0f lines (%.0f code, %.0f comment, %.0f empty) in %d commits, "
							+ "from %s to %s%s", //
					language, //
					languageLines, //
					languageData.filter(Consts.COL_LINE_TYPE, Consts.CODE).sum(), //
					languageData.filter(Consts.COL_LINE_TYPE, Consts.COMMENT).sum(), //
					languageData.filter(Consts.COL_LINE_TYPE, Consts.EMPTY).sum(), //
					languageData.getDistinct(Consts.COL_COMMIT).size(), //
					months[0], //
					months[1], //
					unblamable > 0 ? String.format("(%d umblamable)", unblamable) : ""));
		}
		
		System.out.println();
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
}
