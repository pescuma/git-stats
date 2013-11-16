package org.pescuma.gitstats;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
		final Set<byte[]> commits = new HashSet<byte[]>();
		final AtomicInteger unblamable = new AtomicInteger();
		
		RevWalk revWalk = new RevWalkResetFlags(repository);
		TreeWalk treeWalk = new TreeWalk(repository);
		
		final int filesCount = files.size();
		for (int i = 0; i < filesCount; i++) {
			final int index = i;
			final String file = files.get(i);
			
			System.out.print(String.format("Processing %d/%d ...  ", index + 1, filesCount, file));
			
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
				
				try {
					
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					commit.getId().copyRawTo(buffer);
					byte[] id = buffer.toByteArray();
					commits.add(id);
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				String line = contents.getString(j);
				
				String authorName = commit.getAuthorIdent().getName();
				
				Author author = authors.get(authorName);
				
				if (line.trim().isEmpty())
					author.incEmptyLines();
				else
					author.incTextLines();
			}
		}
		
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
	
	static long sum;
	static int count;
	
	private static BlameResult blame(Repository repository, String file, RevWalk revWalk, TreeWalk treeWalk)
			throws GitAPIException {
		revWalk.reset();
		treeWalk.reset();
		
		long dt = System.nanoTime();
		
		BlameGenerator gen = new BlameGenerator(repository, file, revWalk, treeWalk);
		try {
			
			gen.setTextComparator(RawTextComparator.WS_IGNORE_ALL);
			gen.setFollowFileRenames(true);
			gen.push(null, repository.resolve(Constants.HEAD));
			return gen.computeBlameResult();
			
		} catch (IOException e) {
			throw new JGitInternalException(e.getMessage(), e);
		} finally {
			gen.dispose();
			
			dt = System.nanoTime() - dt;
			long ms = dt / 1000000;
			
			sum += ms;
			count++;
			
			System.out.println("In " + ms + " ms -> avg " + (sum / count) + " ms");
		}
	}
}
