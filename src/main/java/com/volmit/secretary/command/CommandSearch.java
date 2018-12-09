package com.volmit.secretary.command;

import org.inventivetalent.spiget.client.Callback;

import com.volmit.secretary.services.PluginSVC;
import com.volmit.volume.bukkit.U;
import com.volmit.volume.bukkit.command.PawnCommand;
import com.volmit.volume.bukkit.command.VolumeSender;
import com.volmit.volume.bukkit.task.A;
import com.volmit.volume.bukkit.task.S;
import com.volmit.volume.lang.collections.GList;
import com.volmit.volume.lang.json.JSONObject;

public class CommandSearch extends PawnCommand
{
	public CommandSearch()
	{
		super("search", "s");
	}

	@Override
	public boolean handle(VolumeSender sender, String[] args)
	{
		GList<String> c = new GList<String>(args);
		String q = c.toString(" ");

		new A()
		{
			@Override
			public void run()
			{
				U.getService(PluginSVC.class).searchResources(q, 8, 1, new Callback<GList<JSONObject>>()
				{
					@Override
					public void call(GList<JSONObject> v, Throwable error)
					{
						new S()
						{
							@Override
							public void run()
							{
								for(JSONObject i : v)
								{
									sender.sendMessage(i.getString("name") + " -> " + i.getInt("id"));
								}
							}
						};
					}
				});
			}
		};

		return true;
	}
}
