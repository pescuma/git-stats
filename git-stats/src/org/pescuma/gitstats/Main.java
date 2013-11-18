package org.pescuma.gitstats;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkResetFlags;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pescuma.gitstats.blame.BlameGenerator;
import org.pescuma.gitstats.blame.BlameResult;
import org.pescuma.gitstats.threads.ParallelLists;

public class Main {
	
	public static void main(String[] args) throws IOException, GitAPIException, InterruptedException {
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
		
		final Result result = new Result();
		final Progress progress = new Progress(files.size(), args.threads);
		
		ParallelLists parallel = new ParallelLists(args.threads);
		
		parallel.splitInThreads(files, new ParallelLists.Callback<String>() {
			@Override
			public void run(Iterable<String> files) throws Exception {
				computeAuthors(repository, files, result, progress);
			}
		});
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
		
		System.out.println();
		System.out.println("Authors:");
		for (Author author : sortedAuthors(result))
			System.out.println(String.format("   %s : %d commits, %d lines (%d code, %d empty), from %s to %s",
					author.getName(), author.getCommits().getCommitCount(), author.getTotalLines(),
					author.getTextLines(), author.getEmptyLines(),
					dateFormat.format(author.getCommits().getFirstDate()),
					dateFormat.format(author.getCommits().getLastDate())));
		if (result.unblamable.getTotalLines() > 0)
			System.out.println(String.format("   Unblamable lines : %d lines (%d code, %d empty)",
					result.unblamable.getTotalLines(), result.unblamable.getTextLines(),
					result.unblamable.getEmptyLines()));
		
		System.out.println();
		System.out.println("Months:");
		for (DateInfo month : sortedMonths(result))
			System.out.println(String.format("   %s : %d commits, %d lines (%d code, %d empty)", month.getName(),
					month.getCommitCount(), month.getTotalLines(), month.getTextLines(), month.getEmptyLines()));
	}
	
	private static List<Author> sortedAuthors(final Result result) {
		List<Author> sorted = new ArrayList<Author>(result.authors.values());
		Collections.sort(sorted, new Comparator<Author>() {
			@Override
			public int compare(Author o1, Author o2) {
				return o2.getTotalLines() - o1.getTotalLines();
			}
		});
		return sorted;
	}
	
	private static List<DateInfo> sortedMonths(final Result result) {
		List<DateInfo> sorted = new ArrayList<DateInfo>(result.commits.months.values());
		Collections.sort(sorted, new Comparator<DateInfo>() {
			@Override
			public int compare(DateInfo o1, DateInfo o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		return sorted;
	}
	
	private static void computeAuthors(Repository repository, Iterable<String> files, Result result, Progress progress)
			throws Exception {
		RevWalk revWalk = new RevWalkResetFlags(repository);
		TreeWalk treeWalk = new TreeWalk(repository);
		
		for (String file : files) {
			long dt = System.nanoTime();
			try {
				BlameResult blame = blame(repository, file, revWalk, treeWalk);
				
				RawText contents = blame.getResultContents();
				for (int i = 0; i < contents.size(); i++) {
					String line = contents.getString(i);
					boolean isEmptyLine = line.trim().isEmpty();
					
					RevCommit commit = blame.getSourceCommit(i);
					if (commit == null) {
						// System.out.println("  Could not blame " + file + " : " + (j + 1));
						result.unblamable.add(null, isEmptyLine);
						continue;
					}
					
					result.commits.add(commit, isEmptyLine);
					
					String authorName = blame.getSourceAuthor(i).getName();
					Author author = result.authors.get(authorName);
					
					author.add(commit, isEmptyLine);
				}
			} finally {
				dt = System.nanoTime() - dt;
				long ms = dt / 1000000;
				
				long s = sum.addAndGet(ms);
				int c = count.incrementAndGet();
				
				double avg = s / (double) c;
				double eta = (avg * progress.total);
				
				int i = progress.current.incrementAndGet();
				
				System.out.println(String.format(
						"%4d / %4d : In %4d ms -> avg %4.0f ms (so far %3d s | et %3.0f s | ett %3.0f s)", i,
						progress.total, ms, avg, s / 1000, eta / 1000, eta / progress.threadCount / 1000));
			}
		}
	}
	
	static AtomicLong sum = new AtomicLong();
	static AtomicInteger count = new AtomicInteger();
	
	private static BlameResult blame(Repository repository, String file, RevWalk revWalk, TreeWalk treeWalk)
			throws GitAPIException {
		revWalk.reset();
		treeWalk.reset();
		
		BlameGenerator gen = new BlameGenerator(repository, file, revWalk, null);
		try {
			
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
