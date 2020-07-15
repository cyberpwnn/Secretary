package com.volmit.secretary;

import com.volmit.secretary.command.CommandBuild;
import com.volmit.secretary.command.CommandDev;
import com.volmit.secretary.command.CommandSecretary;
import com.volmit.secretary.command.CommandWorkspace;
import com.volmit.secretary.services.PluginSVC;
import com.volmit.volume.bukkit.U;
import com.volmit.volume.bukkit.VolumePlugin;
import com.volmit.volume.bukkit.command.Command;
import com.volmit.volume.bukkit.command.CommandTag;
import com.volmit.volume.bukkit.pawn.Start;
import com.volmit.volume.bukkit.pawn.Stop;

@CommandTag("&e[&8Secretary&e]&7: ")
public class Secretary extends VolumePlugin
{
	@Command
	public CommandSecretary cmdSec;

	@Command
	public CommandDev cmdDev;

	@Command
	public CommandBuild cmdBuild;

	@Command
	public CommandWorkspace cmdWorkspace;

	@Start
	public void start()
	{
		U.startService(PluginSVC.class);
	}

	@Stop
	public void stop()
	{

	}
}
