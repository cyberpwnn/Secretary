package com.volmit.secretary.command;

import com.volmit.volume.bukkit.command.Command;
import com.volmit.volume.bukkit.command.PawnCommand;
import com.volmit.volume.bukkit.command.VolumeSender;

public class CommandSecretary extends PawnCommand
{
	@Command
	public CommandLoad load;

	@Command
	public CommandUnload unload;

	@Command
	public CommandReload reload;

	@Command
	public CommandDelete delete;

	@Command
	public CommandSearch search;

	@Command
	public CommandInstall install;

	public CommandSecretary()
	{
		super("secretary", "sec", "plug");
	}

	@Override
	public boolean handle(VolumeSender sender, String[] args)
	{
		sender.sendMessage("/sec load <plugin>");
		sender.sendMessage("/sec unload <plugin>");
		sender.sendMessage("/sec delete <plugin>");
		sender.sendMessage("/sec search <query>");
		sender.sendMessage("/sec install <id>");
		return true;
	}
}
