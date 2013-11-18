package org.pescuma.gitstats;

import java.util.concurrent.atomic.AtomicInteger;

public class Progress {
	
	public final int threadCount;
	public final int total;
	public final AtomicInteger current = new AtomicInteger();
	
	public Progress(int total, int threadCount) {
		this.threadCount = threadCount;
		this.total = total;
	}
}
