/*
 * CommandShortcutsOptionPane.java - Command shortcuts options panel
 * Copyright (C) 1999 Slava Pestov
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

package org.gjt.sp.jedit.options;

import javax.swing.table.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import org.gjt.sp.jedit.*;

/**
 * Option pane for editing command shortcuts.
 * @author Slava Pestov
 * @version $Id: CommandShortcutsOptionPane.java,v 1.4 2000/04/28 09:29:12 sp Exp $
 */
public class CommandShortcutsOptionPane extends ShortcutsOptionPane
{
	public CommandShortcutsOptionPane()
	{
		super("command-keys");
	}

	// protected members
	protected void _save()
	{
		super._save();
		jEdit.reloadKeyBindings();
	}

	protected Vector createBindings()
	{
		EditAction[] actions = jEdit.getActions();

		Vector bindings = new Vector(actions.length);

		for(int i = 0; i < actions.length; i++)
		{
			String name = actions[i].getName();
			String label = jEdit.getProperty(name + ".label");
			// Skip certain actions this way (ENTER, TAB)
			if(label == null)
				continue;
			label = GUIUtilities.prettifyMenuLabel(label);
			String shortcut = jEdit.getProperty(name + ".shortcut");
			bindings.addElement(new KeyBinding(name,label,shortcut));
		}

		return bindings;
	}
}

/*
 * ChangeLog:
 * $Log: CommandShortcutsOptionPane.java,v $
 * Revision 1.4  2000/04/28 09:29:12  sp
 * Key binding handling improved, VFS updates, some other stuff
 *
 * Revision 1.3  2000/04/16 08:56:24  sp
 * Option pane updates
 *
 * Revision 1.2  2000/04/14 11:57:39  sp
 * Text area actions moved to org.gjt.sp.jedit.actions package
 *
 * Revision 1.1  1999/12/19 08:12:34  sp
 * 2.3 started. Key binding changes  don't require restart, expand-abbrev renamed to complete-word, new splash screen
 *
 */
