package com.volmit.secretary;

import java.io.File;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.volmit.volume.lang.io.VIO;
import com.volmit.volume.lang.json.JSONException;
import com.volmit.volume.lang.json.JSONObject;

public class Config
{
	public boolean slaveMode = false;
	public int slavePort = 14945;
	public String slavePassword = "somethingactuallysecure";

	public static Config load()
	{
		File configLocation = Secretary.vpi.getDataFile("slave-config.json");
		configLocation.getParentFile().mkdirs();

		if(!configLocation.exists())
		{
			try
			{
				VIO.writeAll(configLocation, new JSONObject(new Gson().toJson(new Config())).toString(4));
			}

			catch(JSONException | IOException e)
			{
				e.printStackTrace();
			}
		}

		try
		{
			return new Gson().fromJson(VIO.readAll(configLocation), Config.class);
		}

		catch(JsonSyntaxException | IOException e)
		{
			e.printStackTrace();
			configLocation.delete();
			return load();
		}
	}
}
