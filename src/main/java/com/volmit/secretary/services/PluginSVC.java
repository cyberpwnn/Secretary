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
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.inventivetalent.spiget.client.Callback;
import org.inventivetalent.spiget.client.SpigetClient;
import org.inventivetalent.spiget.downloader.SpigetDownloader;
import org.spiget.client.SpigetDownload;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
	private SpigetClient spiget;

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
		saveCache();
		resourceCache = new NetCache<String, JSONObject>((id) -> getActualResourceData(id));
		descriptionCache = new NetCache<File, PluginDescriptionFile>((f) -> getActualDescription(f));
		spiget = new SpigetClient();
	}

	@Stop
	public void stop()
	{
		saveCacheListing();
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
				done.s(true);
				j.s(new JSONObject(v.toString()));
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

	public void load(File f)
	{
		try
		{
			Plugin p = Bukkit.getPluginManager().loadPlugin(f);
			p.onLoad();
			Bukkit.getPluginManager().enablePlugin(p);
		}

		catch(InvalidDescriptionException e)
		{
			e.printStackTrace();
			return;
		}

		catch(InvalidPluginException e)
		{
			e.printStackTrace();
			return;
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

				unload(pluginFor(i));
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

				unload(pluginFor(i));
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
}
