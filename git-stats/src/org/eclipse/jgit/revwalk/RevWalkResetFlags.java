package org.eclipse.jgit.revwalk;

import org.eclipse.jgit.lib.Repository;

public class RevWalkResetFlags extends RevWalk {
	
	public RevWalkResetFlags(Repository repo) {
		super(repo);
	}
	
	@Override
	protected void reset(int retainFlags) {
		if (retainFlags == 0)
			retainFlags = PARSED;
		
		super.reset(retainFlags);
		
		final int clearFlags = ~retainFlags;
		
		for (RevObject obj : objects) {
			if (!(obj instanceof RevCommit))
				continue;
			
			RevCommit c = (RevCommit) obj;
			
			if ((c.flags & clearFlags) == 0)
				continue;
			c.flags &= retainFlags;
			c.reset();
		}
	}
}
