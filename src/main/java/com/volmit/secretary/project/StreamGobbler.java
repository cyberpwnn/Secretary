package com.volmit.secretary.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class StreamGobbler extends Thread
{
	private final InputStream is;
	private final Consumer<String> logger;

	public StreamGobbler(InputStream is, Consumer<String> logger)
	{
		this.is = is;
		this.logger = logger;
		start();
	}

	@Override
	public void run()
	{
		try
		{
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;

			while((line = br.readLine()) != null)
			{
				logger.accept(line);
			}
		}

		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
}