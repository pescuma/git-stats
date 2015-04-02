package org.pescuma.gitstats;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.pescuma.datatable.DataTable;
import org.pescuma.gitstats.Main.Args;
import org.pescuma.gitstats.threads.ParallelLists;

public class RepositoryProcessor {
	
	public void process(DataTable data, Args args, File path) throws IOException, GitAPIException {
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
		
		RevWalk walk = new RevWalk(repository);
		RevCommit head = walk.parseCommit(repository.resolve(Constants.HEAD));
		
		final Set<ObjectId> ignored = preProcessIgnored(args, repository);
		final Map<String, String> authorMappings = preProcessMappings(args);
		
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
		
		for (DataTable d : tables)
			data.inc(d);
		
		progress.finish();
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
}
