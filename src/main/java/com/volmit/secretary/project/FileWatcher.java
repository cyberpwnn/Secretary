package com.volmit.secretary.project;

import java.io.File;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

public class FileWatcher
{
	private final FileAlterationMonitor monitor;

	public FileWatcher(File folder, Runnable somethingHappened) throws Exception
	{
		FileAlterationObserver o = new FileAlterationObserver(folder);
		o.addListener(new FileAlterationListener()
		{
			@Override
			public void onStop(FileAlterationObserver var1)
			{

			}

			@Override
			public void onStart(FileAlterationObserver var1)
			{

			}

			@Override
			public void onFileDelete(File var1)
			{
				somethingHappened.run();
			}

			@Override
			public void onFileCreate(File var1)
			{
				somethingHappened.run();
			}

			@Override
			public void onFileChange(File var1)
			{
				somethingHappened.run();
			}

			@Override
			public void onDirectoryDelete(File var1)
			{
				somethingHappened.run();
			}

			@Override
			public void onDirectoryCreate(File var1)
			{
				somethingHappened.run();
			}

			@Override
			public void onDirectoryChange(File var1)
			{
				somethingHappened.run();
			}
		});

		this.monitor = new FileAlterationMonitor(1000);
		monitor.addObserver(o);
		monitor.start();
	}

	public void close()
	{
		try
		{
			monitor.stop(2000);
		}

		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
