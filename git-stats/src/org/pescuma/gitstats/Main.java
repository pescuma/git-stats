package org.pescuma.gitstats;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	
	public static void main(String[] args) throws IOException, GitAPIException {
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
	
	private static void run(Args args) throws IOException, GitAPIException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repository = builder //
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
		
		Set<RevCommit> commits = new HashSet<RevCommit>();
		Map<String, AuthorData> authors = new HashMap<String, AuthorData>();
		int unblamable = 0;
		
		int filesCount = files.size();
		for (int i = 0; i < filesCount; i++) {
			String file = files.get(i);
			System.out.println(String.format("Processing %d/%d %s ...", i + 1, filesCount, file));
			
			BlameResult blame = blame(repository, file);
			
			RawText contents = blame.getResultContents();
			for (int j = 0; j < contents.size(); j++) {
				RevCommit commit = blame.getSourceCommit(j);
				if (commit == null) {
					System.out.println("  Could not blame line " + (j + 1));
					unblamable++;
					continue;
				}
				
				commits.add(head);
				
				String line = contents.getString(j);
				
				String authorName = commit.getAuthorIdent().getName();
				
				AuthorData author = authors.get(authorName);
				if (author == null) {
					author = new AuthorData(authorName);
					authors.put(authorName, author);
				}
				
				if (line.trim().isEmpty())
					author.emptyLines++;
				else
					author.textLines++;
			}
		}
		
		List<AuthorData> sortedAuthors = new ArrayList<AuthorData>(authors.values());
		Collections.sort(sortedAuthors, new Comparator<AuthorData>() {
			@Override
			public int compare(AuthorData o1, AuthorData o2) {
				return o2.getTotalLines() - o1.getTotalLines();
			}
		});
		
		System.out.println();
		System.out.println("Authors:");
		for (AuthorData author : sortedAuthors)
			System.out.println(String.format("   %s : %d (%d code, %d empty)", author.name, author.getTotalLines(),
					author.textLines, author.emptyLines));
		if (unblamable > 0)
			System.out.println("   Unblamable lines : " + unblamable);
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
	
	private static class AuthorData {
		final String name;
		int textLines;
		int emptyLines;
		
		AuthorData(String name) {
			this.name = name;
		}
		
		int getTotalLines() {
			return textLines + emptyLines;
		}
	}
}
