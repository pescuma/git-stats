package org.pescuma.gitstats;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.blame.BlameGenerator;
import org.eclipse.jgit.blame.BlameResult;
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

public class Main {
	
	public static void main(String[] args) throws IOException, GitAPIException, InterruptedException {
		Args parsedArgs = new Args();
		CmdLineParser parser = new CmdLineParser(parsedArgs);
		try {
			
			parser.parseArgument(args);
			
		} catch (CmdLineException e) {
			System.out.println(e.getMessage());
			parser.printUsage(System.out);
		}
		
		parsedArgs.applyDefaults();
		
		run(parsedArgs);
	}
	
	private static class Args {
		
		@Argument(required = false, usage = "Path with git repository")
		public File path;
		
		void applyDefaults() {
			if (path == null)
				path = new File(".");
			
			try {
				path = path.getCanonicalFile();
			} catch (IOException e) {
				path = path.getAbsoluteFile();
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
				System.out.println("Skipping " + file + " ...");
				continue;
			}
			files.add(file);
		}
		
		final Authors authors = new Authors();
		final Set<RevCommit> commits = new HashSet<RevCommit>();
		final AtomicInteger unblamable = new AtomicInteger();
		
		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		final int filesCount = files.size();
		for (int i = 0; i < filesCount; i++) {
			final int index = i;
			final String file = files.get(i);
			
			exec.submit(new Runnable() {
				@Override
				public void run() {
					System.out.println(String.format("Processing %d/%d %s ...", index + 1, filesCount, file));
					
					BlameResult blame;
					try {
						blame = blame(repository, file);
					} catch (GitAPIException e) {
						e.printStackTrace();
						return;
					}
					
					RawText contents = blame.getResultContents();
					for (int j = 0; j < contents.size(); j++) {
						RevCommit commit = blame.getSourceCommit(j);
						if (commit == null) {
							System.out.println("  Could not blame " + file + " : " + (j + 1));
							unblamable.incrementAndGet();
							continue;
						}
						
						commits.add(commit);
						
						String line = contents.getString(j);
						
						String authorName = commit.getAuthorIdent().getName();
						
						Author author = authors.get(authorName);
						
						if (line.trim().isEmpty())
							author.incEmptyLines();
						else
							author.incTextLines();
					}
				}
			});
		}
		
		exec.shutdown();
		exec.awaitTermination(1000, TimeUnit.DAYS);
		
		List<Author> sortedAuthors = new ArrayList<Author>(authors.values());
		Collections.sort(sortedAuthors, new Comparator<Author>() {
			@Override
			public int compare(Author o1, Author o2) {
				return o2.getTotalLines() - o1.getTotalLines();
			}
		});
		
		System.out.println();
		System.out.println("Authors:");
		for (Author author : sortedAuthors)
			System.out.println(String.format("   %s : %d (%d code, %d empty)", author.name, author.getTotalLines(),
					author.getTextLines(), author.getEmptyLines()));
		if (unblamable.get() > 0)
			System.out.println("   Unblamable lines : " + unblamable.get());
	}
	
	private static BlameResult blame(Repository repository, String file) throws GitAPIException {
		BlameGenerator gen = new BlameGenerator(repository, file);
		try {
			
			gen.setTextComparator(RawTextComparator.WS_IGNORE_ALL);
			gen.setFollowFileRenames(true);
			gen.push(null, repository.resolve(Constants.HEAD));
			return gen.computeBlameResult();
			
		} catch (IOException e) {
			throw new JGitInternalException(e.getMessage(), e);
		} finally {
			gen.release();
		}
	}
	
	public static class Authors {
		private final ConcurrentMap<String, Author> authors = new ConcurrentHashMap<String, Author>();
		
		public Author get(String authorName) {
			Author author = authors.get(authorName);
			if (author == null) {
				author = new Author(authorName);
				Author other = authors.putIfAbsent(authorName, author);
				if (other != null)
					author = other;
			}
			return author;
		}
		
		public Collection<Author> values() {
			return authors.values();
		}
	}
	
	public static class Author {
		private final String name;
		private final AtomicInteger textLines = new AtomicInteger();
		private final AtomicInteger emptyLines = new AtomicInteger();
		
		public Author(String name) {
			this.name = name;
		}
		
		public void incTextLines() {
			textLines.incrementAndGet();
		}
		
		public void incEmptyLines() {
			emptyLines.incrementAndGet();
		}
		
		public int getTextLines() {
			return textLines.get();
		}
		
		public int getEmptyLines() {
			return emptyLines.get();
		}
		
		public int getTotalLines() {
			return textLines.get() + emptyLines.get();
		}
	}
}
