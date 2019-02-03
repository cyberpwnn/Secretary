package com.volmit.secretary.command;

import java.io.File;

import com.volmit.secretary.services.PluginSVC;
import com.volmit.volume.bukkit.U;
import com.volmit.volume.bukkit.command.PawnCommand;
import com.volmit.volume.bukkit.command.VolumeSender;

public class CommandBuild extends PawnCommand
{
	public CommandBuild()
	{
		super("build", "b", "rb", "rebuild");
	}

	@Override
	public boolean handle(VolumeSender sender, String[] args)
	{
		if(args.length != 1)
		{
			sender.sendMessage("/sec dev rb <PLUGIN>");
			return true;
		}

		File f = U.getService(PluginSVC.class).getWorkspaceFolder();

		if(f == null)
		{
			sender.sendMessage("A developer workspace has not been setup yet.");
			sender.sendMessage("E.g. /sec dev work C:/Users/you/development/workspace");
		}

		else
		{
			U.getService(PluginSVC.class).doRebuild(sender, args[0]);
		}

		return true;
	}
}
