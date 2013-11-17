package org.pescuma.gitstats;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
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
import org.pescuma.gitstats.blame.BlameGenerator;
import org.pescuma.gitstats.blame.BlameResult;

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
				// System.out.println("Skipping " + file + " ...");
				continue;
			}
			files.add(file);
		}
		
		final Authors authors = new Authors();
		// final Set<byte[]> commits = new ConcurrentSkipListSet<byte[]>();
		final AtomicInteger unblamable = new AtomicInteger();
		final AtomicInteger current = new AtomicInteger();
		final int total = files.size();
		
		final int threadCount = Runtime.getRuntime().availableProcessors();
		
		final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>(files);
		
		Thread[] threads = new Thread[threadCount];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread() {
				@Override
				public void run() {
					computeAuthors(repository, new Iterable<String>() {
						@Override
						public Iterator<String> iterator() {
							return new Iterator<String>() {
								String next;
								
								@Override
								public boolean hasNext() {
									if (next == null)
										next = queue.poll();
									
									return next != null;
								}
								
								@Override
								public String next() {
									try {
										return next;
									} finally {
										next = null;
									}
								}
								
								@Override
								public void remove() {
								}
							};
						}
					}, authors, unblamable, current, total, threadCount);
				}
			};
		}
		
		for (int i = 0; i < threads.length; i++)
			threads[i].start();
		
		for (int i = 0; i < threads.length; i++)
			threads[i].join();
		
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
	
	private static void computeAuthors(final Repository repository, Iterable<String> files, final Authors authors,
			final AtomicInteger unblamable, AtomicInteger current, int total, int threadCount) {
		RevWalk revWalk = new RevWalkResetFlags(repository);
		TreeWalk treeWalk = new TreeWalk(repository);
		
		for (String file : files) {
			long dt = System.nanoTime();
			try {
				BlameResult blame;
				try {
					blame = blame(repository, file, revWalk, treeWalk);
				} catch (GitAPIException e) {
					e.printStackTrace();
					return;
				}
				
				RawText contents = blame.getResultContents();
				for (int j = 0; j < contents.size(); j++) {
					RevCommit commit = blame.getSourceCommit(j);
					if (commit == null) {
						// System.out.println("  Could not blame " + file + " : " + (j + 1));
						unblamable.incrementAndGet();
						continue;
					}
					
					// try {
					//
					// ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					// commit.getId().copyRawTo(buffer);
					// byte[] id = buffer.toByteArray();
					// commits.add(id);
					//
					// } catch (IOException e) {
					// e.printStackTrace();
					// }
					
					String line = contents.getString(j);
					
					String authorName = commit.getAuthorIdent().getName();
					
					Author author = authors.get(authorName);
					
					if (line.trim().isEmpty())
						author.incEmptyLines();
					else
						author.incTextLines();
				}
			} finally {
				dt = System.nanoTime() - dt;
				long ms = dt / 1000000;
				
				long s = sum.addAndGet(ms);
				int c = count.incrementAndGet();
				
				double avg = s / (double) c;
				double eta = (avg * total);
				
				int i = current.incrementAndGet();
				
				System.out.println(String.format(
						"%4d / %4d : In %4d ms -> avg %4.0f ms (so far %3d s | et %3.0f s | ett %3.0f s)", i, total,
						ms, avg, s / 1000, eta / 1000, eta / threadCount / 1000));
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
