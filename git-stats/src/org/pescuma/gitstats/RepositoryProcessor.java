package org.pescuma.gitstats;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.pescuma.datatable.DataTable;
import org.pescuma.gitstats.threads.ParallelLists;
import org.pescuma.programminglanguagedetector.FilenameToLanguage;

public class RepositoryProcessor {
	
	public static void process(DataTable data, Args args, File path) throws IOException, GitAPIException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		
		final Repository repository;
		
		try {
			repository = builder //
					.readEnvironment() // scan environment GIT_* variables
					.findGitDir(path) // scan up the file system tree
					.build();
			
		} catch (RuntimeException e) {
			System.out.println(path.getAbsolutePath() + " is not a git repository");
			return;
		}
		
		System.out.println("Processing " + repository.getWorkTree().getAbsolutePath() + " ...");
		
		RevWalk walk = new RevWalk(repository);
		RevCommit head = walk.parseCommit(repository.resolve(Constants.HEAD));
		
		final Set<ObjectId> ignored = preProcessIgnored(args, repository);
		final Map<String, String> authorMappings = args.getAuthorMappings();
		final Map<String, String> languageMappings = args.getLanguageMappings();
		List<String> excludedPaths = preProcessExcludedPaths(args);
		
		TreeWalk tree = new TreeWalk(repository);
		tree.addTree(head.getTree());
		tree.setRecursive(true);
		
		List<String> files = new ArrayList<String>();
		while (tree.next()) {
			String file = tree.getPathString();
			
			if (isInExcludedPath(repository, file, excludedPaths))
				continue;
			
			if (!FilenameToLanguage.isKnownFileType(file))
				continue;
			
			files.add(file);
		}
		
		final List<DataTable> tables = Collections.synchronizedList(new ArrayList<DataTable>());
		final Progress progress = new Progress(files.size());
		
		new ParallelLists(args.threads).splitInThreads(files, new ParallelLists.Callback<String>() {
			@Override
			public void run(Iterable<String> files) throws Exception {
				tables.add(new AuthorsProcessor(repository, ignored, authorMappings, languageMappings).computeAuthors(
						files, progress));
			}
		});
		
		for (DataTable d : tables)
			data.inc(d);
		
		progress.finish();
	}
	
	private static List<String> preProcessExcludedPaths(Args args) {
		List<String> result = new ArrayList<String>();
		
		for (String path : args.excludedPaths) {
			String canonical = Args.getCanonical(path);
			
			if (!canonical.endsWith(File.separator))
				canonical += File.separator;
			
			result.add(canonical);
		}
		
		return result;
	}
	
	private static boolean isInExcludedPath(final Repository repository, String file, List<String> excludePaths)
			throws IOException {
		if (excludePaths.isEmpty())
			return false;
		
		String absolute = new File(repository.getWorkTree(), file).getAbsolutePath();
		for (String excluded : excludePaths) {
			if (FilenameUtils.directoryContains(excluded, absolute))
				return true;
		}
		
		return false;
	}
	
	private static Set<ObjectId> preProcessIgnored(Args args, final Repository repository) throws GitAPIException,
			IOException {
		Set<ObjectId> ignored = new HashSet<ObjectId>();
		
		for (String id : args.ignoredRevisions) {
			ObjectId rid = repository.resolve(id);
			if (rid == null)
				System.out.println("Could not find revision " + id);
			ignored.add(rid);
		}
		
		return ignored;
	}
}
