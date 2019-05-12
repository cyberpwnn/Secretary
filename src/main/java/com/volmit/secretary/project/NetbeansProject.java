package com.volmit.secretary.project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.volmit.secretary.services.PluginSVC;
import com.volmit.secretary.util.nmp.FrameType;
import com.volmit.secretary.util.nmp.J;
import com.volmit.secretary.util.nmp.NMP;
import com.volmit.volume.bukkit.U;
import com.volmit.volume.bukkit.task.S;
import com.volmit.volume.bukkit.util.text.C;
import com.volmit.volume.math.M;
import java.nio.CharBuffer;

public class NetbeansProject extends Thread implements IProject
{
	private File rootDirectory;
	private File watching;
	private String name;
	private String status;
	private MonitorMode monitor;
	private FileWatcher watcher;
	private long lastModification;
	private long lastBuild;
	private int modifiedFiles;
	private boolean req;
	private volatile boolean rebuild;
	private volatile boolean ready;

	public NetbeansProject(File rootDirectory) throws Exception
	{
		this.rootDirectory = rootDirectory;
		lastModification = M.ms();
		lastBuild = M.ms();
		status = "Idle";
		modifiedFiles = 0;
		ready = true;
		req = false;
		scanProjectMetadata();
		setName(getProjectName() + "'s Secretary");
	}

	@Override
	public void run()
	{
		try
		{
			open();
		}

		catch(Exception e1)
		{
			e1.printStackTrace();
			return;
		}

		while(!interrupted())
		{
			try
			{
				Thread.sleep(750);

				if(M.ms() - lastBuild > 2000 && rebuild && ready)
				{
					rebuild = false;

					if(!req && getMonitor().equals(MonitorMode.TARGET))
					{
						status = "Injecting";
						Thread.sleep(1500);
						J.s(() -> install());
						ready = true;
						continue;
					}

					req = false;

					try
					{
						status = "Rebuilding";
						rebuildProject();
					}

					catch(Throwable e)
					{
						status = "Monitoring " + watching.getName();
						ready = true;
					}
				}
			}

			catch(InterruptedException e)
			{
				break;
			}
		}
	}

	@Override
	public void scanProjectMetadata() throws Exception
	{
		File pom = new File(rootDirectory, "build.xml");

		if(!pom.exists())
		{
			throw new FileNotFoundException("Cannot find the build.xml");
		}
		// This is really hacky, but it works ;)
		FileReader r = new FileReader(pom);
		char[] cb = new char[9000];
		int last = r.read(cb);
		r.close();
		String d = String.copyValueOf(cb, 0, last);
		int i = d.indexOf("<project name=\"") + "<project name=\"".length();
		int j = i < 20 ? -1 : d.indexOf("\"", i);
		if(j != -1) {
			name = d.substring(i, j);
		}
		
		monitor = MonitorMode.TARGET;

		watching = getMonitor().equals(MonitorMode.TARGET) ? getTargetDirectory() : getSrcDirectory();
		status = "Monitoring " + watching.getName();
	}

	@Override
	public File getRootDirectory()
	{
		return rootDirectory;
	}

	public void setRootDirectory(File rootDirectory)
	{
		this.rootDirectory = rootDirectory;
	}

	@Override
	public String getVersion()
	{
		return "";
	}

	@Override
	public String getGroupId()
	{
		return name;
	}

	@Override
	public String getArtifactId()
	{
		return name;
	}

	@Override
	public String getProjectName()
	{
		return name;
	}

	@Override
	public MonitorMode getMonitor()
	{
		return monitor;
	}

	public void setMonitor(MonitorMode monitor)
	{
		this.monitor = monitor;
	}

	public void log(String s)
	{
		String tag = C.DARK_GRAY + "[" + C.YELLOW + "Secretary" + C.DARK_GRAY + "]: " + C.GRAY;

		new S()
		{
			@Override
			public void run()
			{
				for(Player i : Bukkit.getOnlinePlayers())
				{
					i.sendMessage(tag + s);
					NMP.MESSAGE.advance(i, new ItemStack(Material.NETHER_STAR), C.GRAY + s, FrameType.GOAL);

					if(s.toLowerCase().contains("injected"))
					{
						i.playSound(i.getLocation(), MSound.EYE_DEATH.bukkitSound(), 0.65f, 1.21f);
					}

					if(s.toLowerCase().contains("building "))
					{
						i.playSound(i.getLocation(), MSound.FRAME_FILL.bukkitSound(), 0.65f, 1.21f);
					}

					if(s.toLowerCase().contains("failed"))
					{
						i.playSound(i.getLocation(), MSound.ECHEST_OPEN.bukkitSound(), 0.65f, 0.21f);
					}
				}

				Bukkit.getConsoleSender().sendMessage(tag + s);
			}
		};
	}

	@Override
	public long getLastModification()
	{
		return lastModification;
	}

	@Override
	public File getTargetDirectory()
	{
		return new File(getRootDirectory(), "dist");
	}

	@Override
	public File getSrcDirectory()
	{
		return new File(getRootDirectory(), "src");
	}

	@Override
	public String getStatus()
	{
		return status;
	}

	@Override
	public long getLastBuild()
	{
		return lastBuild;
	}

	public int getModifiedFiles()
	{
		return modifiedFiles;
	}

	@Override
	public boolean hasSecretary()
	{
		return true;
	}

	@Override
	public String toString()
	{
		return getGroupId() + ":" + getArtifactId() + " (" + getProjectName() + ")";
	}

	@Override
	public void open() throws Exception
	{
		System.out.println("Watcher started");
		watcher = new FileWatcher(getWatchedDirectory(), getMonitor().equals(MonitorMode.TARGET), () -> requestRebuild());
	}

	private void requestRebuild()
	{
		if(ready)
		{
			rebuild = true;
		}
	}

	private void rebuildProject()
	{
		ready = false;
		log("Building " + getProjectName());
		lastBuild = M.ms();
		String failure = "Install";

		try
		{
			J.s(() -> install());
		}

		catch(Throwable e)
		{
			log("Failure on " + getProjectName() + " (" + failure + ")");
			e.printStackTrace();
		}
		ready = true;
	}

	private void install()
	{
		try
		{
			U.getService(PluginSVC.class).reinstall(this, getArtifact());
			log("Injected changes for " + getProjectName());
		}

		catch(Throwable e)
		{
			log("Failed to inject " + getProjectName());
			e.printStackTrace();
		}

		ready = true;
		status = "Monitoring " + watching.getName();
	}

	@Override
	public File getArtifact()
	{
		return new File(getTargetDirectory(), getProjectName() + ".jar");
	}

	@Override
	public void close()
	{
		watcher.close();
	}

	@Override
	public File getWatchedDirectory()
	{
		return watching;
	}

	@Override
	public String getRunCommand()
	{
		return null;
	}

	@Override
	public void rebuild()
	{
		req = true;
		requestRebuild();
	}
}
