package com.volmit.secretary.project;

import java.io.File;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

public class FileWatcher
{
	private final FileAlterationMonitor monitor;

	public FileWatcher(File folder, boolean jarsOnly, boolean pomOnly, Runnable somethingHappened) throws Exception
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
			public void onFileDelete(File f)
			{
				if(jarsOnly)
				{
					if(f.isFile() && f.getName().endsWith(".jar"))
					{
						somethingHappened.run();
					}
				}

				else if(pomOnly)
				{
					if(f.isFile() && f.getName().equals("pom.xml"))
					{
						somethingHappened.run();
					}
				}

				else
				{
					somethingHappened.run();
				}
			}

			@Override
			public void onFileCreate(File f)
			{
				if(jarsOnly)
				{
					if(f.isFile() && f.getName().endsWith(".jar"))
					{
						somethingHappened.run();
					}
				}

				else
				{
					somethingHappened.run();
				}
			}

			@Override
			public void onFileChange(File f)
			{
				if(jarsOnly)
				{
					if(f.isFile() && f.getName().endsWith(".jar"))
					{
						somethingHappened.run();
					}
				}

				else
				{
					somethingHappened.run();
				}
			}

			@Override
			public void onDirectoryDelete(File var1)
			{

			}

			@Override
			public void onDirectoryCreate(File var1)
			{

			}

			@Override
			public void onDirectoryChange(File f)
			{
				if(jarsOnly)
				{
					return;
				}

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
