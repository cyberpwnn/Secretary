package com.volmit.secretary.command;

import java.io.File;

import com.volmit.secretary.services.PluginSVC;
import com.volmit.volume.bukkit.U;
import com.volmit.volume.bukkit.command.PawnCommand;
import com.volmit.volume.bukkit.command.VolumeSender;
import com.volmit.volume.bukkit.util.text.C;

public class CommandWorkspace extends PawnCommand
{
	public CommandWorkspace()
	{
		super("workspace", "work", "ws");
	}

	@Override
	public boolean handle(VolumeSender sender, String[] args)
	{
		if(args.length == 0)
		{
			File f = U.getService(PluginSVC.class).getWorkspaceFolder();

			if(f == null)
			{
				sender.sendMessage("A developer workspace has not been setup yet.");
				sender.sendMessage("E.g. /sec dev work C:/Users/you/development/workspace");
			}

			else
			{
				sender.sendMessage("Current Workspace: " + C.WHITE + f.getAbsolutePath());
			}

			return true;
		}

		String ws = "";

		for(String i : args)
		{
			ws += (" " + i);
		}

		ws = ws.substring(1);

		try
		{
			File f = new File(ws);

			if(f.exists() && f.isDirectory())
			{
				U.getService(PluginSVC.class).setWorkspaceFolder(f);
				sender.sendMessage("Workspace Linked: " + C.WHITE + f.getAbsolutePath());
			}

			else
			{
				sender.sendMessage("Unable to locate folder " + C.WHITE + ws);
			}
		}

		catch(Throwable e)
		{
			sender.sendMessage("Unable to link workspace " + C.WHITE + ws);
			sender.sendMessage("Check Console");
			e.printStackTrace();
		}

		return true;
	}
}
