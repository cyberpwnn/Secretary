package com.volmit.secretary.slave;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import ninja.bytecode.shuriken.web.Parcel;
import ninja.bytecode.shuriken.web.ParcelDescription;
import ninja.bytecode.shuriken.web.ParcelResponse;
import ninja.bytecode.shuriken.web.Parcelable;

@ToString
@EqualsAndHashCode(callSuper = false)
@ParcelResponse("Icarus")
@ParcelDescription("This is a server response usually to another request's success.")
public class ServerOK extends Parcel
{
	private static final long serialVersionUID = -6806767374147741101L;

	public ServerOK()
	{
		super("ok");
	}

	@Override
	public Parcelable respond()
	{
		return null;
	}
}
