package org.pescuma.gitstats;

import java.util.Arrays;

class ByteArrayWrapper {
	
	private final byte[] data;
	
	public ByteArrayWrapper(byte[] data) {
		this.data = data;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(data);
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ByteArrayWrapper other = (ByteArrayWrapper) obj;
		if (!Arrays.equals(data, other.data))
			return false;
		return true;
	}
}