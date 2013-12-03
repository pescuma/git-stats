package org.pescuma.gitstats;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Progress {
	
	private final long start;
	private final int total;
	private final AtomicInteger current = new AtomicInteger();
	private final AtomicLong lastUpdate = new AtomicLong();
	
	public Progress(int total) {
		this.total = total;
		start = System.currentTimeMillis();
	}
	
	public void step() {
		int c = current.incrementAndGet();
		
		long last = lastUpdate.get();
		long now = System.currentTimeMillis();
		
		if (last + 1000 >= now)
			return;
		
		if (!lastUpdate.compareAndSet(last, now))
			return;
		
		output(c, now);
	}
	
	private void output(int c, long now) {
		long dt = now - start;
		double percent = c * 100. / total;
		double avg = dt / (double) c;
		double eta = avg * (total - c);
		
		System.out.print(String.format("\rProcessing files... %.0f %% (%d of %d) | Elapsed %d s | ETA %.0f s", percent,
				c, total, dt / 1000, eta / 1000));
	}
	
	public void finish() {
		output(current.get(), System.currentTimeMillis());
		
		System.out.print("\r" + spaces(79) + "\r");
	}
	
	private String spaces(int count) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < count; i++)
			result.append(' ');
		return result.toString();
	}
}
