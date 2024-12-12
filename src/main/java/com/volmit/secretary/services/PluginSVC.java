package com.volmit.secretary.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.inventivetalent.spiget.client.Callback;
import org.inventivetalent.spiget.client.SpigetClient;
import org.inventivetalent.spiget.downloader.SpigetDownloader;
import org.spiget.client.SpigetDownload;

import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.volmit.secretary.Secretary;
import com.volmit.secretary.project.IProject;
import com.volmit.secretary.project.MavenProject;
import com.volmit.secretary.project.NetbeansProject;
import com.volmit.secretary.util.MavenArtifact;
import com.volmit.volume.bukkit.VolumePlugin;
import com.volmit.volume.bukkit.command.VolumeSender;
import com.volmit.volume.bukkit.pawn.Async;
import com.volmit.volume.bukkit.pawn.Start;
import com.volmit.volume.bukkit.pawn.Stop;
import com.volmit.volume.bukkit.pawn.Tick;
import com.volmit.volume.bukkit.service.IService;
import com.volmit.volume.bukkit.util.plugin.PluginUtil;
import com.volmit.volume.lang.collections.C;
import com.volmit.volume.lang.collections.GList;
import com.volmit.volume.lang.collections.GMap;
import com.volmit.volume.lang.collections.NetCache;
import com.volmit.volume.lang.io.VIO;
import com.volmit.volume.lang.json.JSONArray;
import com.volmit.volume.lang.json.JSONObject;

public class PluginSVC implements IService
{
	private File cacheFolder;
	private File tempFolder;
	private File pluginFolder;
	private File dataFolder;
	private JSONObject cacheListing;
	private boolean saveCache;
	private NetCache<File, PluginDescriptionFile> descriptionCache;
	private NetCache<String, JSONObject> resourceCache;
	private GMap<IProject, Plugin> pluginProjects;
	private GList<IProject> emptyProjects;
	private GMap<Plugin, File> hotFiles;
	private SpigetClient spiget;

	public void status(VolumeSender sender)
	{
		sender.sendMessage("There " + ((pluginProjects.size() + emptyProjects.size()) == 1 ? "is " : "are ") + (pluginProjects.size() + emptyProjects.size()) + " monitored project" + ((pluginProjects.size() + emptyProjects.size()) == 1 ? "" : "s") + ".");

		for(IProject i : pluginProjects.k())
		{
			sender.sendMessage("* " + ChatColor.WHITE + i.toString() + ChatColor.GRAY + " -> " + ChatColor.GREEN + ChatColor.BOLD + i.getStatus());
		}

		for(IProject i : emptyProjects.copy())
		{
			sender.sendMessage("* " + ChatColor.WHITE + i.toString() + ChatColor.GRAY + " -> " + ChatColor.GREEN + ChatColor.BOLD + i.getStatus() + ChatColor.RESET + ChatColor.YELLOW + " (not in server yet)");
		}
	}

	public File getProjectFolder(String aid)
	{
		return Secretary.vpi.getDataFolder("projects", aid);
	}

	public void setWorkspaceFolder(File xf) throws IOException
	{
		File f = Secretary.vpi.getDataFile("caches", "workspace-location.txt");
		f.getParentFile().mkdirs();
		VIO.writeAll(f, xf.getAbsolutePath());
	}

	public File getWorkspaceFolder()
	{
		File f = Secretary.vpi.getDataFile("caches", "workspace-location.txt");

		if(f.exists())
		{
			try
			{
				File fwsp = new File(VIO.readAll(f).trim());

				if(fwsp.exists() && fwsp.isDirectory())
				{
					return fwsp;
				}
			}

			catch(Throwable e)
			{
				e.printStackTrace();
			}
		}

		return null;
	}

	public void rescanDev()
	{
		for(IProject i : pluginProjects.k())
		{
			i.close();
		}

		pluginProjects.clear();

		if(getWorkspaceFolder() != null)
		{
			System.out.println("Scanning Workspace");

			for(File i : getWorkspaceFolder().listFiles())
			{
				if(i.isDirectory() && !i.getName().equals("Secretary"))
				{
					File f = new File(i, "pom.xml");
					File p = new File(i, "src/main/resources/plugin.yml");
					if(f.exists() && p.exists())
					{
						//System.out.println("Checking " + i.getName());
						try
						{
							IProject proj = new MavenProject(i);

							if(proj.hasSecretary())
							{
								boolean fx = false;

								for(Plugin j : Bukkit.getPluginManager().getPlugins())
								{
									String mainPath = j.getDescription().getMain().replaceAll("\\Q.\\E", "/");
									File c = new File(proj.getSrcDirectory(), "main/java/" + mainPath + ".java");
									if(c.exists())
									{
										pluginProjects.put(proj, j);
										((MavenProject) proj).start();
										System.out.println("Found Project: " + proj.toString());
										fx = true;
										break;
									}
								}

								if(!fx)
								{
									System.out.println("Found Project: " + proj.toString() + " (couldnt find the plugin on the server yet though)");
									emptyProjects.add(proj);
									((MavenProject) proj).start();
								}
							}
						}

						catch(Throwable e)
						{
							e.printStackTrace();
						}
					} else {
						f = new File(i, "build.xml");
						p = new File(i, "src/plugin.yml");
						if(f.exists() && p.exists()) {
							try
							{
								IProject proj = new NetbeansProject(i);

								boolean fx = false;

								for(Plugin j : Bukkit.getPluginManager().getPlugins())
								{
									String mainPath = j.getDescription().getMain().replaceAll("\\Q.\\E", "/");
									File c = new File(proj.getSrcDirectory(), mainPath + ".java");
									if(c.exists())
									{
										pluginProjects.put(proj, j);
										((NetbeansProject) proj).start();
										System.out.println("Found Project: " + proj.toString());
										fx = true;
										break;
									}
								}

								if(!fx)
								{
									System.out.println("Found Project: " + proj.toString() + " (couldnt find the plugin on the server yet though)");
									emptyProjects.add(proj);
									((NetbeansProject) proj).start();
								}
							}

							catch(Throwable e)
							{

							}
						}
					}
				}
			}
			System.out.println("Scanning Complete!");
		}
	}

	@Start
	public void start()
	{
		dataFolder = VolumePlugin.vpi.getDataFolder();
		pluginFolder = dataFolder.getParentFile();
		cacheFolder = new File(dataFolder, "caches");
		tempFolder = new File(dataFolder, "tmp");
		cacheFolder.mkdirs();
		tempFolder.mkdirs();
		VIO.delete(tempFolder);
		tempFolder.mkdirs();
		loadCacheListing();
		hotFiles = new GMap<>();
		saveCache();
		resourceCache = new NetCache<String, JSONObject>((id) -> getActualResourceData(id));
		descriptionCache = new NetCache<File, PluginDescriptionFile>((f) -> getActualDescription(f));
		spiget = new SpigetClient();
		pluginProjects = new GMap<>();
		emptyProjects = new GList<>();
		rescanDev();
	}

	@Stop
	public void stop()
	{
		saveCacheListing();

		for(IProject i : pluginProjects.k())
		{
			i.close();
		}

		pluginProjects.clear();
	}

	@Async
	@Tick(200)
	public void updateCacheListing()
	{
		if(saveCache)
		{
			saveCache = false;
			saveCacheListing();
		}
	}

	public void download(String id, VolumeSender sender) throws IOException, InterruptedException
	{
		JSONObject data = getResourceData(id);
		File f = new File(pluginFolder, data.getString("name") + ".jar");
		sender.sendMessage("Downloading " + data.getString("name") + ".jar...");
		JSONObject file = data.getJSONObject("file");
		if(file.has("type") && !file.getString("type").equals(".jar"))
		{
			throw new IOException("Cannot download " + file.getString("type") + " files");
		}

		if(f.exists())
		{
			throw new IOException("Cannot Replace " + f.getName() + " as it may be running.");
		}

		SpigetDownload download = new SpigetDownloader().download("https://www.spigotmc.org/" + file.getString("url"));
		ReadableByteChannel channel = Channels.newChannel(download.getInputStream());

		try(FileOutputStream out = new FileOutputStream(f))
		{
			out.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
		}

		if(!isPlugin(f))
		{
			f.delete();
			throw new IOException(f.getName() + " doesnt seem to be a plugin...");
		}

		cache("resource." + getName(f), id);
		load(getName(f), sender);
	}

	public void download(String id, File f) throws IOException, InterruptedException
	{
		JSONObject data = getResourceData(id);

		if(!data.getString("type").equals(".jar"))
		{
			throw new IOException("Cannot download " + data.getString("type") + " files");
		}

		if(f.exists())
		{
			throw new IOException("Cannot Replace " + f.getName() + " as it may be running.");
		}

		SpigetDownload download = new SpigetDownloader().download("https://www.spigotmc.org/" + data.getDouble("url"));
		ReadableByteChannel channel = Channels.newChannel(download.getInputStream());

		try(FileOutputStream out = new FileOutputStream(f))
		{
			out.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
		}

		if(!isPlugin(f))
		{
			f.delete();
			throw new IOException(f.getName() + " doesnt seem to be a plugin...");
		}

		cache("resource." + getName(f), id);
	}

	public String getId(String resourceName)
	{
		return getCacheString("resource." + resourceName);
	}

	public JSONObject getResourceData(String id)
	{
		return resourceCache.get(id);
	}

	private JSONObject getActualResourceData(String id)
	{
		C<JSONObject> j = new C<>();
		C<Boolean> done = new C<>(false);

		spiget.request("resources/" + id).getJson(new Callback<JsonElement>()
		{
			@Override
			public void call(JsonElement v, Throwable error)
			{
				if(v != null)
				{
					done.s(true);
					j.s(new JSONObject(v.toString()));
				}

				else if(error != null)
				{
					error.printStackTrace();
				}
			}
		});

		while(!done.g())
		{
			try
			{
				Thread.sleep(10);
			}

			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}

		return j.g();
	}

	public void searchResources(String q, int max, int page, Callback<GList<JSONObject>> c)
	{
		GList<JSONObject> k = new GList<JSONObject>();
		spiget.request("/search/resources/" + q + "?field=name&size=" + max + "&page=1").getJson(new Callback<JsonElement>()
		{
			@Override
			public void call(JsonElement v, Throwable error)
			{
				if(v == null)
				{
					call(new JsonArray(), error);
					return;
				}

				JSONArray ja = new JSONArray(v.toString());

				for(int i = 0; i < ja.length(); i++)
				{
					k.add(ja.getJSONObject(i));
				}

				c.call(k, error);
			}
		});
	}

	public void delete(Plugin p)
	{
		File f = getFileFor(p);
		unload(p);
		delete(f);
	}

	public void delete(File f)
	{
		f.delete();
	}

	public void reload(Plugin p)
	{
		File f = getFileFor(p);
		unload(p);
		load(f);
	}

	public Plugin load(File f)
	{
		try
		{
			Plugin p = Bukkit.getPluginManager().loadPlugin(f);
			p.onLoad();
			Bukkit.getPluginManager().enablePlugin(p);
			return p;
		}

		catch(InvalidDescriptionException e)
		{
			e.printStackTrace();
			return null;
		}

		catch(InvalidPluginException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public void unload(Plugin p)
	{
		PluginUtil.unload(p);
	}

	public File getFileFor(Plugin p)
	{
		if(isCached(p.getName() + "-" + p.getDescription().getMain()))
		{
			File f = new File(pluginFolder, getCacheString(p.getName() + "-" + p.getDescription().getMain()));

			if(f.exists() && f.isFile())
			{
				return f;
			}
		}

		for(File i : pluginFolder.listFiles())
		{
			if(i.isFile() && i.getName().endsWith(".jar") && isPlugin(i) && getName(i).equals(p.getName()))
			{
				cache(p.getName() + "-" + p.getDescription().getMain(), i.getName());
				return i;
			}
		}

		return null;
	}

	public File getFileFor(String p)
	{
		if(isCached("pxname-" + p))
		{
			return new File(getCacheString("pxname-" + p));
		}

		for(File i : pluginFolder.listFiles())
		{
			if(i.isFile() && i.getName().endsWith(".jar") && isPlugin(i) && getName(i).equals(p))
			{
				cache("pxname-" + p, i.getAbsolutePath());
				return i;
			}
		}

		return null;
	}

	public void cache(String s, String v)
	{
		cacheListing.put(s, v);
		saveCache();
	}

	public void cache(String s, boolean v)
	{
		cacheListing.put(s, v);
		saveCache();
	}

	public boolean getCacheBoolean(String s)
	{
		return cacheListing.getBoolean(s);
	}

	public String getCacheString(String s)
	{
		if(!isCached(s))
		{
			return "null";
		}

		return cacheListing.getString(s);
	}

	public boolean isCached(String name)
	{
		return cacheListing.has(name);
	}

	public void reinstall(IProject mavenProject, File artifact) throws IOException
	{
		File ff = new File(new File("plugins"), artifact.getName());

		if(emptyProjects.contains(mavenProject))
		{
			emptyProjects.remove(mavenProject);
		}

		else
		{
			Plugin plugin = pluginProjects.remove(mavenProject);

			if(plugin != null)
			{
				try
				{
					delete(plugin);
				}

				catch(Throwable e)
				{

				}
			}
		}

		Files.copy(artifact, ff);
		Plugin np = load(ff);

		if(np != null)
		{
			pluginProjects.put(mavenProject, np);
			hotFiles.put(np, ff);
		}
	}

	public InputStream readCache(String name)
	{
		if(!isCached(name))
		{
			return null;
		}

		try
		{
			File f = new File(cacheFolder, cacheListing.getString(name));
			return new FileInputStream(f);
		}

		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public void invalidate()
	{
		for(String i : new GList<String>(cacheListing.keySet()))
		{
			invalidate(i);
		}
	}

	public void invalidate(String name)
	{
		if(!isCached(name))
		{
			return;
		}

		if(!(cacheListing.get(name) instanceof String))
		{
			cacheListing.remove(name);
			saveCache();
			return;
		}

		File f = new File(cacheFolder, cacheListing.getString(name));
		cacheListing.remove(name);
		saveCache();

		if(f.exists())
		{
			f.deleteOnExit();
			f.getParentFile().deleteOnExit();
			f.getParentFile().getParentFile().deleteOnExit();
		}
	}

	public void cache(String name, InputStream in)
	{
		try
		{
			String guid = UUID.randomUUID().toString();
			String path = guid.split("-")[1] + "/" + guid.split("-")[2] + "/" + guid;
			File f = new File(cacheFolder, path);
			f.getParentFile().mkdirs();
			VIO.fullTransfer(in, new FileOutputStream(f), 8192);
			cacheListing.put(name, path);
			saveCache();
		}

		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public boolean isPlugin(File jar)
	{
		if(!isCached("is.plugin." + jar.getName()))
		{
			cache("is.plugin." + jar.getName(), getDescription(jar) != null);
		}

		return getCacheBoolean("is.plugin." + jar.getName());
	}

	public MavenArtifact getMavenProject(File jar)
	{
		if(!isCached("maven.plugin." + jar.getName()))
		{
			GMap<MavenArtifact, String> artifacts = new GMap<MavenArtifact, String>();
			GMap<MavenArtifact, GList<MavenArtifact>> children = new GMap<MavenArtifact, GList<MavenArtifact>>();

			try
			{
				for(String i : VIO.listEntries(jar))
				{
					if(i.startsWith("META-INF/maven/"))
					{
						if(i.endsWith("/pom.xml"))
						{
							VIO.readEntry(jar, i, (in) ->
							{
								try
								{
									String data = VIO.readAll(in);
									MavenArtifact a = new MavenArtifact("?", "?", "?");
									boolean afterparent = false;
									boolean hasparent = data.contains("</parent>");

									for(String j : data.split("\n"))
									{
										if(j.contains("</parent>"))
										{
											afterparent = true;
											continue;
										}

										if(j.trim().startsWith("<artifactId>") && ((hasparent && afterparent) || (!hasparent)))
										{
											a.setArtifactId(j.trim().replaceAll("\\Q<artifactId>\\E", "").replaceAll("\\Q</artifactId>\\E", ""));
										}

										if(j.trim().startsWith("<groupId>"))
										{
											a.setGroupId(j.trim().replaceAll("\\Q<groupId>\\E", "").replaceAll("\\Q</groupId>\\E", ""));
										}

										if(j.trim().startsWith("<version>"))
										{
											a.setVersion(j.trim().replaceAll("\\Q<version>\\E", "").replaceAll("\\Q</version>\\E", ""));
										}

										if(!a.getArtifactId().equals("?") && !a.getGroupId().equals("?") && !a.getVersion().equals("?"))
										{
											artifacts.put(a, data);
											break;
										}
									}
								}

								catch(IOException e)
								{
									e.printStackTrace();
								}
							});
						}
					}
				}

				for(MavenArtifact i : artifacts.k())
				{
					String pom = artifacts.get(i);
					GList<MavenArtifact> childs = new GList<MavenArtifact>();

					for(MavenArtifact j : artifacts.k())
					{
						if(pom.contains("<artifactId>" + j.getArtifactId() + "</artifactId>"))
						{
							childs.add(j);
						}
					}

					children.put(i, childs);
				}

				GList<MavenArtifact> coverage = children.k();

				for(MavenArtifact i : children.k())
				{
					for(MavenArtifact j : children.get(i))
					{
						if(j.equals(i))
						{
							continue;
						}

						coverage.remove(j);
					}
				}

				if(coverage.size() == 1)
				{
					cache("maven.plugin." + jar.getName(), coverage.get(0).toString());

					return coverage.get(0);
				}
			}

			catch(IOException e)
			{
				e.printStackTrace();
			}

			return null;
		}

		return new MavenArtifact(getCacheString("maven.plugin." + jar.getName()));
	}

	public String getName(File jar)
	{
		return getDescription(jar).getName();
	}

	public String getVersion(File jar)
	{
		return getDescription(jar).getVersion();
	}

	public String getMain(File jar)
	{
		return getDescription(jar).getMain();
	}

	public String getAuthor(File jar)
	{
		return getDescription(jar).getAuthors().isEmpty() ? "A Ghost" : getDescription(jar).getAuthors().get(0);
	}

	public PluginDescriptionFile getDescription(File jar)
	{
		return descriptionCache.get(jar);
	}

	private PluginDescriptionFile getActualDescription(File jar)
	{
		if(isCached("plugin.description." + jar.getName()))
		{
			try
			{
				return new PluginDescriptionFile(readCache("plugin.description." + jar.getName()));
			}

			catch(InvalidDescriptionException e)
			{
				invalidate("plugin.description." + jar.getName());
				e.printStackTrace();
			}
		}

		try
		{
			C<PluginDescriptionFile> f = new C<>();
			VIO.readEntry(jar, "plugin.yml", (in) ->
			{
				try
				{
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					VIO.fullTransfer(in, os, 8192);
					byte[] data = os.toByteArray();
					f.s(new PluginDescriptionFile(new ByteArrayInputStream(data)));
					cache("plugin.description." + jar.getName(), new ByteArrayInputStream(data));
				}

				catch(InvalidDescriptionException | IOException e)
				{
					e.printStackTrace();
				}
			});

			return f.g();
		}

		catch(IOException e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public void saveCache()
	{
		saveCache = true;
	}

	private void saveCacheListing()
	{
		File listing = new File(cacheFolder, "listing.json");
		try
		{
			VIO.writeAll(listing, cacheListing);
		}

		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	private void loadCacheListing()
	{
		File listing = new File(cacheFolder, "listing.json");

		if(listing.exists())
		{
			try
			{
				cacheListing = new JSONObject(VIO.readAll(listing));
			}

			catch(Exception e)
			{
				listing.delete();
			}
		}

		cacheListing = new JSONObject();
	}

	public boolean isLoaded(File f)
	{
		for(Plugin i : Bukkit.getPluginManager().getPlugins())
		{
			if(getFileFor(i).equals(f))
			{
				return true;
			}
		}

		return false;
	}

	public Plugin pluginFor(File f)
	{
		for(Plugin i : Bukkit.getPluginManager().getPlugins())
		{
			if(getFileFor(i).equals(f))
			{
				return i;
			}
		}

		return null;
	}

	public void load(String s, VolumeSender sender) throws IOException
	{
		for(File i : pluginFolder.listFiles())
		{
			if(i.isFile() && isPlugin(i) && getName(i).equalsIgnoreCase(s))
			{
				if(isLoaded(i))
				{
					throw new IOException(getName(i) + " is already loaded.");
				}

				load(i);
				sender.sendMessage("Loaded " + getName(i));
				return;
			}
		}

		for(File i : pluginFolder.listFiles())
		{
			if(i.isFile() && isPlugin(i) && getName(i).toLowerCase().contains(s.toLowerCase()))
			{
				if(isLoaded(i))
				{
					throw new IOException(getName(i) + " is already loaded.");
				}

				load(i);
				sender.sendMessage("Loaded " + getName(i));
			}
		}
	}

	public void unload(String s, VolumeSender sender) throws IOException
	{
		for(File i : pluginFolder.listFiles())
		{
			if(i.isFile() && isPlugin(i) && getName(i).equalsIgnoreCase(s))
			{
				if(!isLoaded(i))
				{
					throw new IOException(getName(i) + " is not loaded.");
				}

				Plugin p = pluginFor(i);

				if(pluginProjects.containsValue(p))
				{
					for(IProject xi : pluginProjects.k())
					{
						if(pluginProjects.get(xi).equals(p))
						{
							((MavenProject) xi).log("Plugin was unloaded!");
							pluginProjects.remove(xi);
						}
					}
				}

				unload(p);
				sender.sendMessage("Unloaded " + getName(i));
				return;
			}
		}

		for(File i : pluginFolder.listFiles())
		{
			if(i.isFile() && isPlugin(i) && getName(i).toLowerCase().contains(s.toLowerCase()))
			{
				if(!isLoaded(i))
				{
					throw new IOException(getName(i) + " is not loaded.");
				}

				Plugin p = pluginFor(i);

				if(pluginProjects.containsValue(p))
				{
					for(IProject xi : pluginProjects.k())
					{
						if(pluginProjects.get(xi).equals(p))
						{
							((MavenProject) xi).log("Plugin was unloaded!");
							pluginProjects.remove(xi);
						}
					}
				}

				unload(p);
				sender.sendMessage("Unloaded " + getName(i));
			}
		}
	}

	public void delete(String s, VolumeSender sender) throws IOException
	{
		for(File i : pluginFolder.listFiles())
		{
			if(i.isFile() && isPlugin(i) && getName(i).equalsIgnoreCase(s))
			{
				if(isLoaded(i))
				{
					unload(pluginFor(i));
					sender.sendMessage("Unloaded " + getName(i));
				}

				delete(i);
				sender.sendMessage("Deleted " + getName(i));
				return;
			}
		}

		for(File i : pluginFolder.listFiles())
		{
			if(i.isFile() && isPlugin(i) && getName(i).toLowerCase().contains(s.toLowerCase()))
			{
				if(isLoaded(i))
				{
					unload(pluginFor(i));
					sender.sendMessage("Unloaded " + getName(i));
				}

				delete(i);
				sender.sendMessage("Deleted " + getName(i));
			}
		}
	}

	public void reload(String s, VolumeSender sender) throws IOException
	{
		for(File i : pluginFolder.listFiles())
		{
			if(i.isFile() && isPlugin(i) && getName(i).equalsIgnoreCase(s))
			{
				if(!isLoaded(i))
				{
					throw new IOException(getName(i) + " is not loaded.");
				}

				reload(pluginFor(i));
				sender.sendMessage("Reloaded " + getName(i));
				return;
			}
		}

		for(File i : pluginFolder.listFiles())
		{
			if(i.isFile() && isPlugin(i) && getName(i).toLowerCase().contains(s.toLowerCase()))
			{
				if(!isLoaded(i))
				{
					throw new IOException(getName(i) + " is not loaded.");
				}

				reload(pluginFor(i));
				sender.sendMessage("Reloaded " + getName(i));
			}
		}
	}

	public void doRebuild(VolumeSender sender, String string)
	{
		for(IProject i : pluginProjects.k())
		{
			if(string.equalsIgnoreCase(i.getProjectName()) || string.equalsIgnoreCase(i.getArtifactId()))
			{
				sender.sendMessage("Rebuilding " + i.getProjectName() + " " + i.getVersion());
				i.rebuild();
				return;
			}
		}

		sender.sendMessage("Couldn't find the project '" + string + "'");
	}
}
