package org.pescuma.gitstats;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Result {
	private final ConcurrentMap<ByteArrayWrapper, ByteArrayWrapper> commits = new ConcurrentHashMap<ByteArrayWrapper, ByteArrayWrapper>();
	public final Authors authors = new Authors();
	public final Author unblamable = new Author("Unblamable");
	
	public void addCommit(byte[] id) {
		ByteArrayWrapper ba = new ByteArrayWrapper(id);
		commits.putIfAbsent(ba, ba);
	}
}
