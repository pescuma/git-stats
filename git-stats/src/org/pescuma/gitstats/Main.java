package org.pescuma.gitstats;

import static java.lang.Math.round;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pescuma.datatable.DataTable;
import org.pescuma.datatable.DataTable.Value;
import org.pescuma.datatable.MemoryDataTable;
import org.pescuma.gitstats.blame.BlameGenerator;
import org.pescuma.gitstats.blame.BlameResult;
import org.pescuma.gitstats.threads.ParallelLists;

public class Main {
	
	private static final int COL_LANGUAGE = 0;
	private static final int COL_LINE_TYPE = 1;
	private static final int COL_MONTH = 2;
	private static final int COL_COMMIT = 3;
	private static final int COL_AUTHOR = 4;
	
	private static final String EMPTY = "Empty";
	private static final String CODE = "Code";
	
	public static void main(String[] args) throws IOException, GitAPIException,
			InterruptedException {
		Args parsedArgs = new Args();
		CmdLineParser parser = new CmdLineParser(parsedArgs);
		try {
			
			parser.parseArgument(args);
			
		} catch (CmdLineException e) {
			System.out.println(e.getMessage());
			parser.printUsage(System.out);
			return;
		}
		
		parsedArgs.applyDefaults();
		
		run(parsedArgs);
	}
	
	private static class Args {
		
		@Argument(required = false, usage = "Path with git repository")
		public File path;
		
		@Option(name = "-t", aliases = { "--threads" }, usage = "Number of threads (by default it creates one thread per processor)")
		public int threads;
		
		void applyDefaults() {
			if (path == null)
				path = new File(".");
			
			try {
				path = path.getCanonicalFile();
			} catch (IOException e) {
				path = path.getAbsoluteFile();
			}
			
			if (threads < 1) {
				threads = Runtime.getRuntime().availableProcessors();
				if (threads >= 4)
					// Some room for breathing
					threads--;
			}
		}
	}
	
	private static void run(Args args) throws IOException, GitAPIException, InterruptedException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		final Repository repository = builder //
				.readEnvironment() // scan environment GIT_* variables
				.findGitDir(args.path) // scan up the file system tree
				.build();
		
		RevWalk walk = new RevWalk(repository);
		RevCommit head = walk.parseCommit(repository.resolve(Constants.HEAD));
		
		TreeWalk tree = new TreeWalk(repository);
		tree.addTree(head.getTree());
		tree.setRecursive(true);
		
		List<String> files = new ArrayList<String>();
		while (tree.next()) {
			String file = tree.getPathString();
			if (!FilenameToLanguage.isKnownFileType(file)) {
				// System.out.println("Skipping " + file + " ...");
				continue;
			}
			files.add(file);
		}
		
		final List<DataTable> tables = Collections.synchronizedList(new ArrayList<DataTable>());
		final Progress progress = new Progress(files.size());
		
		new ParallelLists(args.threads).splitInThreads(files, new ParallelLists.Callback<String>() {
			@Override
			public void run(Iterable<String> files) throws Exception {
				tables.add(computeAuthors(repository, files, progress));
			}
		});
		
		DataTable data = new MemoryDataTable();
		for (DataTable d : tables)
			data.inc(d);
		
		progress.finish();
		
		outputStats(data);
	}
	
	private static DataTable computeAuthors(Repository repository, Iterable<String> files,
			Progress progress) throws Exception {
		DataTable data = new MemoryDataTable();
		
		RevWalk revWalk = new RevWalk(repository);
		
		for (String file : files) {
			try {
				computeAuthors(data, repository, revWalk, file);
			} finally {
				progress.step();
			}
		}
		
		return data;
	}
	
	private static void computeAuthors(DataTable data, Repository repository, RevWalk revWalk,
			String file) throws GitAPIException {
		
		String language = FilenameToLanguage.detectLanguage(file);
		
		BlameResult blame = blame(repository, file, revWalk);
		
		RawText contents = blame.getResultContents();
		for (int i = 0; i < contents.size(); i++) {
			String line = contents.getString(i);
			boolean isEmptyLine = line.trim().isEmpty();
			
			RevCommit commit = blame.getSourceCommit(i);
			if (commit == null) {
				// System.out.println("  Could not blame " + file + " : " + (j + 1));
				data.inc(1, language, isEmptyLine ? EMPTY : CODE);
				continue;
			}
			
			int time = commit.getCommitTime();
			String month = new SimpleDateFormat("yyyy-MM").format(new Date(time * 1000L));
			String authorName = blame.getSourceAuthor(i).getName();
			
			data.inc(1, language, isEmptyLine ? EMPTY : CODE, month, commit.getId().getName(),
					authorName);
		}
	}
	
	private static BlameResult blame(Repository repository, String file, RevWalk revWalk)
			throws GitAPIException {
		revWalk.reset();
		
		BlameGenerator gen = new BlameGenerator(repository, file, revWalk, null);
		try {
			revWalk.markStart(revWalk.lookupCommit(repository.resolve(Constants.HEAD)));
			
			gen.setTextComparator(RawTextComparator.WS_IGNORE_ALL);
			gen.setFollowFileRenames(true);
			gen.push(null, repository.resolve(Constants.HEAD));
			return gen.computeBlameResult();
			
		} catch (IOException e) {
			throw new JGitInternalException(e.getMessage(), e);
		} finally {
			gen.dispose();
		}
	}
	
	private static void outputStats(DataTable data) {
		double totalLines = data.sum();
		
		System.out.println();
		System.out.println("Authors:");
		for (String author : sortedByLines(data, COL_AUTHOR)) {
			if (author.isEmpty())
				continue;
			
			DataTable authorData = data.filter(COL_AUTHOR, author);
			String[] months = getMonthRange(authorData);
			double authorLines = authorData.sum();
			
			System.out
					.println(String
							.format("   %s : %.1f%% of the lines: %.0f lines (%.0f code, %.0f empty) in %d commits from %s to %s", //
									author, //
									percent(authorLines, totalLines), //
									authorLines, //
									authorData.filter(COL_LINE_TYPE, CODE).sum(), //
									authorData.filter(COL_LINE_TYPE, EMPTY).sum(), //
									authorData.getDistinct(COL_COMMIT).size(), //
									months[0], //
									months[1]));
		}
		{
			DataTable unblamableData = data.filter(COL_AUTHOR, "");
			double unblamableLines = unblamableData.sum();
			if (unblamableLines > 0) {
				System.out
						.println(String
								.format("   Unblamable lines : %.1f%% of the lines: %.0f lines (%.0f code, %.0f empty)",
										percent(unblamableLines, totalLines), //
										unblamableLines, //
										unblamableData.filter(COL_LINE_TYPE, CODE).sum(), //
										unblamableData.filter(COL_LINE_TYPE, EMPTY).sum()));
			}
		}
		
		System.out.println();
		System.out.println("Months:");
		for (String month : data.getDistinct(COL_MONTH)) {
			if (month.isEmpty())
				continue;
			
			DataTable monthData = data.filter(COL_MONTH, month);
			double monthLines = monthData.sum();
			
			System.out.println(String.format(
					"   %s : %.0f lines (%.0f code, %.0f empty) in %d commits", //
					month, //
					monthLines, //
					monthData.filter(COL_LINE_TYPE, CODE).sum(), //
					monthData.filter(COL_LINE_TYPE, EMPTY).sum(),//
					monthData.getDistinct(COL_COMMIT).size()));
		}
		
		System.out.println();
		System.out.println("Languages:");
		for (String language : sortedByLines(data, COL_LANGUAGE)) {
			DataTable languageData = data.filter(COL_LANGUAGE, language);
			String[] months = getMonthRange(languageData);
			double languageLines = languageData.sum();
			long unblamable = round(languageData.filter(COL_AUTHOR, "").sum());
			
			System.out.println(String.format(
					"   %s : %.0f lines (%.0f code, %.0f empty) in %d commits, "
							+ "from %s to %s%s", //
					language, //
					languageLines, //
					languageData.filter(COL_LINE_TYPE, CODE).sum(), //
					languageData.filter(COL_LINE_TYPE, EMPTY).sum(), //
					languageData.getDistinct(COL_COMMIT).size(), //
					months[0], //
					months[1], //
					unblamable > 0 ? String.format("(%d umblamable)", unblamable) : ""));
		}
	}
	
	private static double percent(double count, double total) {
		return count * 100 / total;
	}
	
	private static String[] getMonthRange(DataTable authorData) {
		List<String> result = new ArrayList<String>(authorData.getDistinct(COL_MONTH));
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
