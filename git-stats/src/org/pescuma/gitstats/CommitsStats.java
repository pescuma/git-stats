package org.pescuma.gitstats;

import static java.lang.Math.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.jgit.revwalk.RevCommit;

public class CommitsStats {
	
	private final ConcurrentMap<ByteArrayWrapper, ByteArrayWrapper> commits = new ConcurrentHashMap<ByteArrayWrapper, ByteArrayWrapper>();
	public final ConcurrentMap<String, DateInfo> months = new ConcurrentHashMap<String, DateInfo>();
	public int firstCommit = Integer.MAX_VALUE;
	public int lastCommit = 0;
	
	public void add(RevCommit commit, boolean emptyLine) throws IOException {
		ByteArrayWrapper ba = new ByteArrayWrapper(getId(commit));
		
		int time = commit.getCommitTime();
		Date date = toDate(time);
		String month = new SimpleDateFormat("yyyy-MM").format(date);
		
		DateInfo info = getInfo(month);
		
		if (commits.putIfAbsent(ba, ba) == null) {
			// First time this commit is seen
			synchronized (this) {
				firstCommit = min(firstCommit, time);
				lastCommit = max(lastCommit, time);
				info.addCommit(commit);
			}
		}
		
		info.addLine(commit, emptyLine);
	}
	
	private Date toDate(int time) {
		return new Date(time * 1000L);
	}
	
	private DateInfo getInfo(String month) {
		DateInfo info = months.get(month);
		if (info == null) {
			DateInfo tmp = new DateInfo(month);
			
			info = months.putIfAbsent(month, tmp);
			
			if (info == null)
				info = tmp;
		}
		return info;
	}
	
	private static byte[] getId(RevCommit commit) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		commit.getId().copyRawTo(buffer);
		return buffer.toByteArray();
	}
	
	public int getCommitCount() {
		return commits.size();
	}
	
	public Date getFirstDate() {
		return toDate(firstCommit);
	}
	
	public Date getLastDate() {
		return toDate(lastCommit);
	}
}
