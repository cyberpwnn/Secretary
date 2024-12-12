package com.volmit.secretary.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.volmit.secretary.Secretary;
import com.volmit.secretary.services.PluginSVC;
import com.volmit.secretary.util.nmp.J;
import com.volmit.volume.bukkit.U;
import com.volmit.volume.bukkit.task.S;
import com.volmit.volume.bukkit.util.text.C;
import com.volmit.volume.lang.collections.GList;
import com.volmit.volume.lang.format.F;

import ninja.bytecode.shuriken.bench.PrecisionStopwatch;
import ninja.bytecode.shuriken.execution.Looper;
import ninja.bytecode.shuriken.math.M;
import ninja.bytecode.shuriken.reaction.O;

public class MavenProject extends Thread implements IProject
{
	private File rootDirectory;
	private File watching;
	private String version;
	private String groupId;
	private String artifactId;
	private String name;
	private String runcommand;
	private String status;
	private MonitorMode monitor;
	private FileWatcher watcher;
	private long lastModification;
	private long lastBuild;
	private int modifiedFiles;
	private boolean hasSecretary;
	private boolean req;
	private volatile boolean rebuild;
	private volatile boolean ready;

	public MavenProject(File rootDirectory) throws Exception
	{
		this.rootDirectory = rootDirectory;
		lastModification = M.ms();
		lastBuild = M.ms();
		status = "Idle";
		modifiedFiles = 0;
		hasSecretary = false;
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
				Thread.sleep(250);

				if(M.ms() - lastBuild > 500 && rebuild && ready)
				{
					rebuild = false;

					if(!req && getMonitor().equals(MonitorMode.TARGET))
					{
						status = "Injecting";
						Thread.sleep(250);
						J.s(() -> install());
						ready = true;
						return;
					}

					req = false;

					try
					{
						status = "Rebuilding";
						rebuildProject();
						rebuild = false;
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
		System.out.print("Scanning " + rootDirectory.getPath());
		File pom = new File(rootDirectory, "pom.xml");

		if(!pom.exists())
		{
			throw new FileNotFoundException("Cannot find the pom.xml");
		}

		MavenXpp3Reader reader = new MavenXpp3Reader();
		reader.setAddDefaultEntities(false);
		Model model = reader.read(new FileInputStream(getRootDirectory().getAbsolutePath() + "/pom.xml"));

		version = model.getVersion();
		artifactId = model.getArtifactId();
		groupId = model.getArtifactId();
		name = model.getName();
		Properties props = model.getProperties();
		monitor = MonitorMode.TARGET;

		try
		{
			monitor = MonitorMode.valueOf(props.getProperty("secretary.monitor").toUpperCase());
			hasSecretary = true;
		}

		catch(Throwable e)
		{

		}

		try
		{
			runcommand = props.getProperty("secretary.build", "package");

			if(!runcommand.equals("package"))
			{
				hasSecretary = true;
			}
		}

		catch(Throwable e)
		{

		}

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
		return version;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	@Override
	public String getGroupId()
	{
		return groupId;
	}

	public void setGroupId(String groupId)
	{
		this.groupId = groupId;
	}

	@Override
	public String getArtifactId()
	{
		return artifactId;
	}

	public void setArtifactId(String artifactId)
	{
		this.artifactId = artifactId;
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
				}

				if(s.toLowerCase().contains("injected"))
				{
					Secretary.play("success");
				}

				if(s.toLowerCase().contains("building "))
				{
					Secretary.play("started");
				}

				if(s.toLowerCase().contains("failed") || s.toLowerCase().contains("failure"))
				{
					Secretary.play("failed");
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
		return new File(getRootDirectory(), "target");
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
		return hasSecretary;
	}

	@Override
	public String toString()
	{
		return getGroupId() + ":" + getArtifactId() + ":" + getVersion() + " (" + getProjectName() + ")";
	}

	@Override
	public void open() throws Exception
	{
		watcher = new FileWatcher(getWatchedDirectory(), getMonitor().equals(MonitorMode.TARGET), false, () -> requestRebuild());
	}

	private void requestRebuild()
	{
		rebuild = true;
	}

	private void rebuildProject()
	{
		ready = false;
		log("Building " + getProjectName());
		lastBuild = M.ms();
		String failure = "";

		try
		{
			failure = "Failed to read pom.xml correctly.";
			verifyMetadata();
			status = "Rebuilding";
			failure = "Failed to start maven build with command " + runcommand;
			PrecisionStopwatch px = new PrecisionStopwatch();
			px.begin();

			if(buildProject())
			{
				log("Build Successful on " + getProjectName() + " in " + F.time(px.getMilliseconds(), 2));
				px.end();
				status = "Injecting";
				rebuild = false;
				J.s(() -> install());
			}

			else
			{
				status = "Monitoring " + watching.getName();
				ready = true;
				rebuild = false;
				log("Build Failure on " + getProjectName() + ". Took " + F.time(px.getMilliseconds(), 2));
				return;
			}
		}

		catch(Throwable e)
		{
			log("Failure on " + getProjectName() + " (" + failure + ")");
			e.printStackTrace();
			ready = true;
		}
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
		return new File(getTargetDirectory(), getProjectName() + "-" + getVersion() + ".jar");
	}

	private boolean buildProject() throws IOException, InterruptedException
	{
		return buildProject(true);
	}

	private boolean buildProject(boolean usePath) throws IOException, InterruptedException
	{
		rebuild = false;

		GList<String> pars = new GList<>();
		pars.add("cmd");
		pars.add("/c");
		pars.add(usePath ? "mvn" : (Secretary.vpi.getDataFile("caches", "maven", "apache-maven-3.6.2", "bin", "mvn").getAbsolutePath()));
		pars.add(getRunCommand().split("\\Q \\E"));
		ProcessBuilder pb = new ProcessBuilder(pars.toArray(new String[pars.size()]));
		pb.directory(getRootDirectory());
		Process p = pb.start();
		O<Boolean> cancelled = new O<Boolean>().set(false);
		Looper l = new Looper()
		{
			@Override
			protected long loop()
			{
				if(rebuild)
				{
					Secretary.play("restart");
					log("Build Cancelled. New Changes!");
					p.destroyForcibly();
					cancelled.set(true);

					return -1;
				}

				return 150;
			}
		};
		l.start();

		new StreamGobbler(p.getInputStream(), (log) -> System.out.println(name + ": " + log));
		new StreamGobbler(p.getErrorStream(), (log) -> log("Build Error: " + log));

		while(true)
		{
			try
			{
				if(p.waitFor(250, TimeUnit.MILLISECONDS))
				{
					if(cancelled.get())
					{
						return buildProject(usePath);
					}

					break;
				}
			}

			catch(InterruptedException e)
			{

			}
		}
		l.interrupt();

		return p.exitValue() == 0;
	}

	private void verifyMetadata() throws Exception
	{
		int mon = getMonitor().ordinal();
		scanProjectMetadata();

		if(mon != getMonitor().ordinal())
		{
			resetWatcher();
		}
	}

	private void resetWatcher()
	{
		close();

		try
		{
			open();
		}

		catch(Exception e)
		{
			e.printStackTrace();
		}
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
		return runcommand;
	}

	@Override
	public void rebuild()
	{
		req = true;
		requestRebuild();
	}
}
