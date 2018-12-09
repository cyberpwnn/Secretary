package com.volmit.secretary.command;

import com.volmit.secretary.services.PluginSVC;
import com.volmit.volume.bukkit.U;
import com.volmit.volume.bukkit.command.PawnCommand;
import com.volmit.volume.bukkit.command.VolumeSender;
import com.volmit.volume.bukkit.task.A;

public class CommandInstall extends PawnCommand
{
	public CommandInstall()
	{
		super("install", "inst");
	}

	@Override
	public boolean handle(VolumeSender sender, String[] args)
	{
		new A()
		{
			@Override
			public void run()
			{
				for(String i : args)
				{
					try
					{
						U.getService(PluginSVC.class).download(i, sender);
					}

					catch(Exception e)
					{
						sender.sendMessage("Failed to download: " + e.getMessage());
					}
				}
			}
		};

		return true;
	}
}
