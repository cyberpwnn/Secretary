package com.volmit.secretary.services;

import com.volmit.secretary.Secretary;
import com.volmit.volume.bukkit.pawn.Start;
import com.volmit.volume.bukkit.pawn.Stop;
import com.volmit.volume.bukkit.service.IService;

import ninja.bytecode.shuriken.web.ParcelWebServer;

public class SlaveSVC implements IService
{
	private ParcelWebServer server;
	
	@Start
	public void start()
	{
		if(Secretary.config.slaveMode)
		{
			//@builder
			server = new ParcelWebServer()
					.configure()
					.https(true)
					.http(false)
					.serverPath("/")
					.maxFormContentSize(1024*1024*1024)
					.httpsPort(Secretary.config.slavePort)
				.applySettings()
				.addParcelables()
				.start();
			//@done
		}
	}

	@Stop
	public void stop()
	{
		
	}
}
