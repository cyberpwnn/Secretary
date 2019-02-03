package com.volmit.secretary.project;

@FunctionalInterface
public interface WatcherResult
{
	public void onReturned(int filesModified, boolean overflow, boolean interrupted, boolean modified);
}
