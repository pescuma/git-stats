package org.pescuma.gitstats;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.blame.BlameGenerator;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.pescuma.datatable.DataTable;
import org.pescuma.datatable.MemoryDataTable;
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
		
		for (String file : files) {
			try {
				computeAuthors(data, file);
			} finally {
				progress.step();
			}
		}
		
		return data;
	}
	
	private void computeAuthors(DataTable data, String file) throws GitAPIException {
		
		String language = FilenameToLanguage.detectLanguage(file);
		
		SimpleFileParser parser = new SimpleFileParser(language);
		
		BlameResult blame = blame(file);
		
		RawText contents = blame.getResultContents();
		for (int i = 0; i < contents.size(); i++) {
			String line = contents.getString(i);
			String lineType = toLineTypeName(parser.feedNextLine(line));
			
			RevCommit commit = blame.getSourceCommit(i);
			if (commit == null) {
				data.inc(1, language, lineType, "", "", "", file);
				continue;
			}
			
			if (ignored.contains(commit.getId()))
				continue;
			
			int time = commit.getCommitTime();
			String month = new SimpleDateFormat("yyyy-MM").format(new Date(time * 1000L));
			
			String authorName = blame.getSourceAuthor(i).getName();
			if (authorName != null) {
				String alternateName = authorMappings.get(authorName);
				if (alternateName != null)
					authorName = alternateName;
			}
			
			data.inc(1, language, lineType, month, commit.getId().getName(), authorName, file);
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
	
	private BlameResult blame(String file) throws GitAPIException {
		BlameGenerator blame = new BlameGenerator(repository, file);
		try {
			
			blame.setTextComparator(RawTextComparator.WS_IGNORE_ALL);
			blame.setFollowFileRenames(true);
			blame.push(null, repository.resolve(Constants.HEAD));
			return blame.computeBlameResult();
			
		} catch (IOException e) {
			throw new JGitInternalException(e.getMessage(), e);
		} finally {
			blame.release();
		}
	}
}
