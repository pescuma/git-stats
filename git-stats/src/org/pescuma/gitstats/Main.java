package org.pescuma.gitstats;

import static java.lang.Math.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
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
import org.pescuma.gitstats.SimpleFileParser.LineType;
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
	private static final String COMMENT = "Comment";
	
	private static final String IGNORED = "Ignored";
	
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
		
		@Option(name = "-i", aliases = { "--ignore-rev" }, usage = "Revision to ignore (can be used multiple times)")
		public List<String> ignoredRevisions = new ArrayList<String>();
		
		@Option(name = "-a", aliases = { "--author" }, usage = "Authors mapping, in the format loginname=Joe User (can be used multiple times)")
		public List<String> authors = new ArrayList<String>();
		
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
		
		Set<ObjectId> ignored = preProcessIgnored(args, repository);
		Map<String, String> authorMappings = preProcessMappings(args);
		
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
				tables.add(new AuthorsProcessor(repository, ignored, authorMappings)
						.computeAuthors(files, progress));
			}
		});
		
		DataTable data = new MemoryDataTable();
		for (DataTable d : tables)
			data.inc(d);
		
		progress.finish();
		
		outputStats(data);
	}
	
	private static Map<String, String> preProcessMappings(Args args) {
		Map<String, String> authorMappings = new HashMap<String, String>();
		
		for (String am : args.authors) {
			int pos = am.indexOf('=');
			authorMappings.put(am.substring(0, pos).trim(), am.substring(pos + 1).trim());
		}
		
		return authorMappings;
	}
	
	private static Set<ObjectId> preProcessIgnored(Args args, final Repository repository)
			throws GitAPIException, IOException {
		Set<ObjectId> ignored = new HashSet<ObjectId>();
		
		for (String id : args.ignoredRevisions) {
			ObjectId rid = repository.resolve(id);
			if (rid == null)
				System.out.println("Could not find revision " + id);
			ignored.add(rid);
		}
		
		return ignored;
	}
	
	private static class AuthorsProcessor {
		
		private final Repository repository;
		private final Set<ObjectId> ignored;
		private final Map<String, String> authorMappings;
		
		public AuthorsProcessor(Repository repository, Set<ObjectId> ignored,
				Map<String, String> authorMappings) {
			this.repository = repository;
			this.authorMappings = authorMappings;
			this.ignored = ignored;
		}
		
		public DataTable computeAuthors(Iterable<String> files, Progress progress)
				throws GitAPIException {
			DataTable data = new MemoryDataTable();
			
			RevWalk revWalk = new RevWalk(repository);
			
			for (String file : files) {
				try {
					computeAuthors(data, revWalk, file);
				} finally {
					progress.step();
				}
			}
			
			return data;
		}
		
		private void computeAuthors(DataTable data, RevWalk revWalk, String file)
				throws GitAPIException {
			
			SimpleFileParser parser = new SimpleFileParser(file);
			String language = FilenameToLanguage.detectLanguage(file);
			
			BlameResult blame = blame(file, revWalk);
			
			RawText contents = blame.getResultContents();
			for (int i = 0; i < contents.size(); i++) {
				String line = contents.getString(i);
				String lineType = toLineTypeName(parser.feedNextLine(line));
				
				RevCommit commit = blame.getSourceCommit(i);
				if (commit == null) {
					data.inc(1, language, lineType);
					continue;
				}
				
				int time = commit.getCommitTime();
				String month = new SimpleDateFormat("yyyy-MM").format(new Date(time * 1000L));
				
				if (ignored.contains(commit.getId())) {
					data.inc(1, language, lineType, month, commit.getId().getName(), IGNORED);
					continue;
				}
				
				String authorName = blame.getSourceAuthor(i).getName();
				if (authorName != null) {
					String alternateName = authorMappings.get(authorName);
					if (alternateName != null)
						authorName = alternateName;
				}
				
				data.inc(1, language, lineType, month, commit.getId().getName(), authorName);
			}
		}
		
		private static String toLineTypeName(LineType lineType) {
			switch (lineType) {
				case Empty:
					return EMPTY;
				case Code:
					return CODE;
				case Comment:
					return COMMENT;
				default:
					throw new IllegalArgumentException();
			}
		}
		
		private BlameResult blame(String file, RevWalk revWalk) throws GitAPIException {
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
							.format("   %s : %.1f%% of the lines: %.0f lines (%.0f code, %.0f comment, %.0f empty) in %d commits from %s to %s", //
									author, //
									percent(authorLines, totalLines), //
									authorLines, //
									authorData.filter(COL_LINE_TYPE, CODE).sum(), //
									authorData.filter(COL_LINE_TYPE, COMMENT).sum(), //
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
								.format("   Unblamable lines : %.1f%% of the lines: %.0f lines (%.0f code, %.0f comment, %.0f empty)",
										percent(unblamableLines, totalLines), //
										unblamableLines, //
										unblamableData.filter(COL_LINE_TYPE, CODE).sum(), //
										unblamableData.filter(COL_LINE_TYPE, COMMENT).sum(), //
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
					"   %s : %.0f lines (%.0f code, %.0f comment, %.0f empty) in %d commits", //
					month, //
					monthLines, //
					monthData.filter(COL_LINE_TYPE, CODE).sum(), //
					monthData.filter(COL_LINE_TYPE, COMMENT).sum(), //
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
					"   %s : %.0f lines (%.0f code, %.0f comment, %.0f empty) in %d commits, "
							+ "from %s to %s%s", //
					language, //
					languageLines, //
					languageData.filter(COL_LINE_TYPE, CODE).sum(), //
					languageData.filter(COL_LINE_TYPE, COMMENT).sum(), //
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
