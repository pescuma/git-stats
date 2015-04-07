package org.pescuma.gitstats;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import org.pescuma.datatable.DataTable;
import org.pescuma.datatable.MemoryDataTable;
import org.pescuma.gitstats.blame.BlameGenerator;
import org.pescuma.gitstats.blame.BlameResult;
import org.pescuma.programminglanguagedetector.FilenameToLanguage;
import org.pescuma.programminglanguagedetector.SimpleFileParser;
import org.pescuma.programminglanguagedetector.SimpleFileParser.LineType;

public class AuthorsProcessor {
	
	private final Repository repository;
	private final Set<ObjectId> ignored;
	private final Map<String, String> authorMappings;
	
	public AuthorsProcessor(Repository repository, Set<ObjectId> ignored, Map<String, String> authorMappings) {
		this.repository = repository;
		this.authorMappings = authorMappings;
		this.ignored = ignored;
	}
	
	public DataTable computeAuthors(Iterable<String> files, Progress progress) throws GitAPIException {
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
	
	private void computeAuthors(DataTable data, RevWalk revWalk, String file) throws GitAPIException {
		
		String language = FilenameToLanguage.detectLanguage(file);
		
		SimpleFileParser parser = new SimpleFileParser(language);
		
		BlameResult blame = blame(file, revWalk);
		
		RawText contents = blame.getResultContents();
		for (int i = 0; i < contents.size(); i++) {
			String line = contents.getString(i);
			String lineType = toLineTypeName(parser.feedNextLine(line));
			
			RevCommit commit = blame.getSourceCommit(i);
			if (commit == null) {
				data.inc(1, language, file, lineType);
				continue;
			}
			
			int time = commit.getCommitTime();
			String month = new SimpleDateFormat("yyyy-MM").format(new Date(time * 1000L));
			
			if (ignored.contains(commit.getId())) {
				data.inc(1, language, file, lineType, month, commit.getId().getName(), Consts.IGNORED);
				continue;
			}
			
			String authorName = blame.getSourceAuthor(i).getName();
			if (authorName != null) {
				String alternateName = authorMappings.get(authorName);
				if (alternateName != null)
					authorName = alternateName;
			}
			
			data.inc(1, language, file, lineType, month, commit.getId().getName(), authorName);
		}
	}
	
	private static String toLineTypeName(LineType lineType) {
		switch (lineType) {
			case Empty:
				return Consts.EMPTY;
			case Code:
				return Consts.CODE;
			case Comment:
				return Consts.COMMENT;
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
