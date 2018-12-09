package com.volmit.secretary.command;

import java.io.IOException;

import com.volmit.secretary.services.PluginSVC;
import com.volmit.volume.bukkit.U;
import com.volmit.volume.bukkit.command.PawnCommand;
import com.volmit.volume.bukkit.command.VolumeSender;

public class CommandUnload extends PawnCommand
{
	public CommandUnload()
	{
		super("unload", "ul", "u");
	}

	@Override
	public boolean handle(VolumeSender sender, String[] args)
	{
		for(String i : args)
		{
			try
			{
				U.getService(PluginSVC.class).unload(i, sender);
			}

			catch(IOException e)
			{
				sender.sendMessage(e.getMessage());
			}
		}

		return true;
	}
}
