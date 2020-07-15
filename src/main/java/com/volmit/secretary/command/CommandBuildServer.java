package com.volmit.secretary.command;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import com.volmit.secretary.project.Download;
import com.volmit.secretary.project.DownloadMonitor;
import com.volmit.secretary.project.DownloadState;
import com.volmit.secretary.util.StreamSucker;
import com.volmit.volume.bukkit.command.PawnCommand;
import com.volmit.volume.bukkit.command.VolumeSender;

public class CommandBuildServer extends PawnCommand
{
	public CommandBuildServer()
	{
		super("server", "s");
	}

	@Override
	public boolean handle(VolumeSender sender, String[] args)
	{
		if(args.length != 1)
		{
			sender.sendMessage("/build server <MC VERSION | LATEST>");
			return true;
		}

		File f = new File("secretary/buildtools");
		f.mkdirs();
		File ff = new File("secretary/spigot-" + args[0]);
		ff.mkdirs();

		try
		{
			new Download(new DownloadMonitor()
			{

				@Override
				public void onDownloadUpdateProgress(Download download, long bytes, long totalBytes, double percentComplete)
				{

				}

				@Override
				public void onDownloadStateChanged(Download download, DownloadState from, DownloadState to)
				{

				}

				@Override
				public void onDownloadStarted(Download download)
				{
					sender.sendMessage("Updating BuildTools");
				}

				@Override
				public void onDownloadFinished(Download download)
				{
					sender.sendMessage("Updated BuildTools");
					ProcessBuilder pb = new ProcessBuilder("java", "-jar", download.getFile().getAbsolutePath(), "--rev", args[0], "--output-dir", ff.getAbsolutePath());
					try
					{
						sender.sendMessage("Building Spigot " + args[0] + "...");
						pb.directory(f);
						Process p = pb.start();

						StreamSucker sl = new StreamSucker(p.getInputStream(), (l) ->
						{
							sender.sendMessage("[Build]: " + l);
						});
						sl.start();
						int px = p.waitFor();
						sender.sendMessage("Build finished with code " + px);
					}

					catch(IOException e)
					{
						e.printStackTrace();
					}

					catch(InterruptedException e)
					{
						e.printStackTrace();
					}
				}

				@Override
				public void onDownloadFailed(Download download)
				{

				}
			}, new URL("https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar"), new File(f, "BuildTools.jar"), 8192).start();
		}

		catch(IOException e)
		{

		}

		return true;
	}
}
