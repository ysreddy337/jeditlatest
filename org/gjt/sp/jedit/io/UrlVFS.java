/*
 * UrlVFS.java - Url VFS
 * Copyright (C) 2000 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.io;

import java.awt.Component;
import java.io.*;
import java.net.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * URL VFS.
 * @author Slava Pestov
 * @version $Id: UrlVFS.java,v 1.9 2000/11/11 02:59:31 sp Exp $
 */
public class UrlVFS extends VFS
{
	public UrlVFS()
	{
		super("url");
	}

	public int getCapabilities()
	{
		return READ_CAP | WRITE_CAP;
	}

	public String constructPath(String parent, String path)
	{
		if(parent.endsWith("/"))
			return parent + path;
		else
			return parent + '/' + path;
	}

	public String getParentOfPath(String path)
	{
		return MiscUtilities.getParentOfPath(path);
	}

	public InputStream _createInputStream(Object session,
		String path, boolean ignoreErrors, Component comp)
		throws IOException
	{
		try
		{
			return new URL(path).openStream();
		}
		catch(MalformedURLException mu)
		{
			Log.log(Log.ERROR,this,mu);
			String[] args = { path };
			VFSManager.error(comp,"badurl",args);
			return null;
		}
	}

	public OutputStream _createOutputStream(Object session, String path,
		Component comp) throws IOException
	{
		try
		{
			return new URL(path).openConnection()
				.getOutputStream();
		}
		catch(MalformedURLException mu)
		{
			Log.log(Log.ERROR,this,mu);
			String[] args = { path };
			VFSManager.error(comp,"badurl",args);
			return null;
		}
	}
}

/*
 * ChangeLog:
 * $Log: UrlVFS.java,v $
 * Revision 1.9  2000/11/11 02:59:31  sp
 * FTP support moved out of the core into a plugin
 *
 * Revision 1.8  2000/09/26 10:19:47  sp
 * Bug fixes, spit and polish
 *
 * Revision 1.7  2000/07/31 11:32:09  sp
 * VFS file chooser is now in a minimally usable state
 *
 * Revision 1.6  2000/07/30 09:04:19  sp
 * More VFS browser hacking
 *
 * Revision 1.5  2000/07/29 12:24:08  sp
 * More VFS work, VFS browser started
 *
 */
