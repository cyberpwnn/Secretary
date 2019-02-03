package com.volmit.secretary.command;

import com.volmit.secretary.services.PluginSVC;
import com.volmit.volume.bukkit.U;
import com.volmit.volume.bukkit.command.Command;
import com.volmit.volume.bukkit.command.PawnCommand;
import com.volmit.volume.bukkit.command.VolumeSender;

public class CommandDev extends PawnCommand
{
	@Command
	public CommandWorkspace workspace;

	@Command
	public CommandRescan rescan;

	@Command
	public CommandBuild build;

	public CommandDev()
	{
		super("developer", "dev");
	}

	@Override
	public boolean handle(VolumeSender sender, String[] args)
	{
		sender.sendMessage("/sec dev workspace [absolute directory]");
		sender.sendMessage("/sec dev build <plugin>");
		sender.sendMessage("/sec dev rescan");
		U.getService(PluginSVC.class).status(sender);

		return true;
	}
}
