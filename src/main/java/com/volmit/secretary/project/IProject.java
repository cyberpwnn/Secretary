package com.volmit.secretary.project;

import java.io.File;
import java.io.IOException;

public interface IProject
{
	public void scanProjectMetadata() throws Exception;

	public File getRootDirectory();

	public String getStatus();

	public String getVersion();

	public String getGroupId();

	public String getArtifactId();

	public File getTargetDirectory();

	public File getWatchedDirectory();

	public File getSrcDirectory();
	
	public File getArtifact();

	public String getProjectName();

	public long getLastModification();

	public long getLastBuild();

	public String getRunCommand();

	public MonitorMode getMonitor();

	public boolean hasSecretary();

	public void open() throws IOException, Exception;

	public void close();

	public void rebuild();
}
