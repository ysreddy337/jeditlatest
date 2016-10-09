/*
 * RegistersChanged.java - Registers changed message
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

package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;

/**
 * Message sent when the value of one or more registers has changed.
 * @author Slava Pestov
 * @version $Id: RegistersChanged.java,v 1.1 2000/05/22 12:05:45 sp Exp $
 *
 * @since jEdit 2.5pre4
 */
public class RegistersChanged extends EBMessage.NonVetoable
{
	/**
	 * Creates a new registers changed message.
	 * @param source The message source
	 */
	public RegistersChanged(EBComponent source)
	{
		super(source);
	}
}
