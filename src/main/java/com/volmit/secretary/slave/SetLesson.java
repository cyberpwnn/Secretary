package com.volmit.secretary.slave;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.volmit.secretary.services.PluginSVC;
import com.volmit.volume.bukkit.U;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ninja.bytecode.shuriken.io.IO;
import ninja.bytecode.shuriken.logging.L;
import ninja.bytecode.shuriken.web.Parcel;
import ninja.bytecode.shuriken.web.ParcelDescription;
import ninja.bytecode.shuriken.web.ParcelRequest;
import ninja.bytecode.shuriken.web.ParcelResponseError;
import ninja.bytecode.shuriken.web.ParcelResponseSuccess;
import ninja.bytecode.shuriken.web.Parcelable;
import ninja.bytecode.shuriken.web.UploadParcelable;

@ToString
@EqualsAndHashCode(callSuper = false)
@ParcelRequest("Lesson")
@ParcelDescription("Set the requested lesson by id")
@ParcelResponseSuccess(type = ServerOK.class, reason = "If the lesson was updated / created")
@ParcelResponseError(type = ServerError.class, reason = "Invalid Token or Server Error")
public class SetLesson extends Parcel implements UploadParcelable
{
	private static final long serialVersionUID = -6806767374147741101L;

	@Getter
	@Setter
	@ParcelDescription("The hashed password.")
	private String password;

	@Getter
	@Setter
	@ParcelDescription("The plugin name (not the file name)")
	private String plugin;

	public SetLesson()
	{
		super("inject");
	}

	@Override
	public Parcelable respond(InputStream in)
	{
		try
		{
			L.i("Receiving Remote Plugin Injection for " + plugin);
			File jar = U.getService(PluginSVC.class).getFileFor(plugin);
			boolean unload = jar.exists();

			if(unload)
			{
				Plugin p = Bukkit.getPluginManager().getPlugin(plugin);

				if(p != null)
				{
					L.i("Unloading " + p.getName() + " " + p.getDescription().getVersion() + " (remote injection)");
					U.getService(PluginSVC.class).unload(p);
				}
			}

			L.i("Streaming " + plugin + " Binary into " + jar.getName());
			FileOutputStream fos = new FileOutputStream(jar);
			IO.fullTransfer(in, fos, 8192);
			fos.close();
			L.i("Received Remote Plugin Injection for " + plugin);
			L.i("Loading " + jar.getName() + " (" + plugin + ")");
			U.getService(PluginSVC.class).load(jar);
			L.i("Injection Complete");

			return new ServerOK();
		}

		catch(Throwable e)
		{
			L.ex(e);
		}

		return new ServerError("Internal Error");
	}

	@Override
	public Parcelable respond()
	{
		return new ServerError("Invalid Method");
	}
}
