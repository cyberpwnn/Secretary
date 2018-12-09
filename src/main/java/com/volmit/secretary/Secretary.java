package com.volmit.secretary;

import com.volmit.secretary.command.CommandSecretary;
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
