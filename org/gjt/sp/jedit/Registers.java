/*
 * Registers.java - Register manager
 * Copyright (C) 1999, 2000 Slava Pestov
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

package org.gjt.sp.jedit;

import javax.swing.text.*;
import java.awt.datatransfer.*;
import java.awt.Toolkit;
import java.io.*;
import java.util.Vector;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.RegistersChanged;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

/**
 * jEdit's registers are an extension of the clipboard metaphor. There
 * can be an unlimited number of registers, each one holding a string,
 * caret position, or file name. By default, register "$" contains the
 * clipboard, and all other registers are empty. Actions invoked by
 * the user and the methods in this class can change register contents.
 *
 * @author Slava Pestov
 * @version $Id: Registers.java,v 1.18 2001/04/19 08:07:25 sp Exp $
 */
public class Registers
{
	/**
	 * Convinience method that copies the text selected in the specified
	 * text area into the specified register.
	 * @param textArea The text area
	 * @param register The register
	 * @since jEdit 2.7pre2
	 */
	public static void copy(JEditTextArea textArea, char register)
	{
		String selection = textArea.getSelectedText();
		if(selection == null)
			return;

		setRegister(register,selection);
		HistoryModel.getModel("clipboard").addItem(selection);
	}

	/**
	 * Convinience method that appends the text selected in the specified
	 * text area to the specified register, with a newline between the old
	 * and new text.
	 * @param textArea The text area
	 * @param register The register
	 */
	public static void append(JEditTextArea textArea, char register)
	{
		append(textArea,register,"\n");
	}

	/**
	 * Convinience method that appends the text selected in the specified
	 * text area to the specified register.
	 * @param textArea The text area
	 * @param register The register
	 * @param separator The text to insert between the old and new text
	 */
	public static void append(JEditTextArea textArea, char register,
		String separator)
	{
		String selection = textArea.getSelectedText();
		if(selection == null)
			return;

		Register reg = getRegister(register);

		if(reg != null && reg.toString() != null)
			selection = reg.toString() + separator + selection;

		setRegister(register,selection);
		HistoryModel.getModel("clipboard").addItem(selection);
	}

	/**
	 * Convinience method that copies the text selected in the specified
	 * text area into the specified register, and then removes it from the
	 * text area.
	 * @param textArea The text area
	 * @param register The register
	 * @since jEdit 2.7pre2
	 */
	public static void cut(JEditTextArea textArea, char register)
	{
		if(textArea.isEditable())
		{
			String selection = textArea.getSelectedText();
			if(selection == null)
				return;

			setRegister(register,selection);
			HistoryModel.getModel("clipboard").addItem(selection);

			textArea.setSelectedText("");
		}
		else
			textArea.getToolkit().beep();
	}

	/**
	 * Convinience method that pastes the contents of the specified
	 * register into the text area.
	 * @param textArea The text area
	 * @param register The register
	 * @since jEdit 2.7pre2
	 */
	public static void paste(JEditTextArea textArea, char register)
	{
		Register reg = getRegister(register);
		if(reg == null)
		{
			textArea.getToolkit().beep();
			return;
		}
		else
		{
			String selection = reg.toString();
			if(selection == null)
			{
				textArea.getToolkit().beep();
				return;
			}

			textArea.setSelectedText(selection);
			HistoryModel.getModel("clipboard").addItem(selection);
		}
	}

	/**
	 * Convinience method that exchanges the caret position with that
	 * stored in the specified register.
	 * @param editPane The edit pane
	 * @param register The register
	 */
	public static void exchangeCaretRegister(EditPane editPane,
		char register)
	{
		Register reg = Registers.getRegister(register);
		JEditTextArea textArea = editPane.getTextArea();

		if(reg instanceof CaretRegister)
		{
			CaretRegister caretReg = (CaretRegister)reg;
			Buffer buffer = caretReg.openFile();
			if(buffer == null)
				return;

			int offset = caretReg.getOffset();

			setRegister(register,new CaretRegister(editPane.getBuffer(),
				textArea.getCaretPosition()));

			editPane.setBuffer(buffer);
			textArea.setCaretPosition(offset);
		}
		else
			editPane.getToolkit().beep();
	}

	/**
	 * Convinience method that stores the caret position in the
	 * specified register.
	 * @param editPane The edit pane
	 * @param register The register
	 */
	public static void setCaretRegister(EditPane editPane, char register)
	{
		Registers.setRegister(register,new CaretRegister(
			editPane.getBuffer(),editPane.getTextArea().getCaretPosition()));
	}

	/**
	 * Convinience method that selects from the caret position to the
	 * position stored in the specified register.
	 * @param editPane The edit pane
	 * @param register The register
	 */
	public static void selectCaretRegister(EditPane editPane, char register)
	{
		Register reg = getRegister(register);

		if(reg instanceof CaretRegister)
		{
			CaretRegister caretReg = (CaretRegister)reg;
			if(caretReg.openFile() != editPane.getBuffer())
			{
				editPane.getToolkit().beep();
				return;
			}

			editPane.getTextArea().select(editPane.getTextArea()
				.getCaretPosition(),caretReg.getOffset());
		}
		else
			editPane.getToolkit().beep();
	}

	/**
	 * Convinience method that opens the buffer and moves the caret to the
	 * position pointed to by the specified register.
	 * @param editPane The edit pane
	 * @param register The register
	 */
	public static void goToCaretRegister(EditPane editPane, char register)
	{
		Register reg = getRegister(register);

		if(reg instanceof CaretRegister)
		{
			CaretRegister caretReg = (CaretRegister)reg;
			Buffer buffer = caretReg.openFile();
			if(buffer == null)
				return;
			editPane.setBuffer(buffer);
			editPane.getTextArea().setCaretPosition(caretReg.getOffset());
		}
		else
			editPane.getToolkit().beep();
	}

	/**
	 * Returns the specified register.
	 * @param name The name
	 */
	public static Register getRegister(char name)
	{
		if(registers == null || name >= registers.length)
			return null;
		else
			return registers[name];
	}

	/**
	 * Sets the specified register.
	 * @param name The name
	 * @param value The new value
	 */
	public static void setRegister(char name, String value)
	{
		setRegister(name,new StringRegister(value));
	}

	/**
	 * Sets the specified register.
	 * @param name The name
	 * @param newRegister The new value
	 */
	public static void setRegister(char name, Register newRegister)
	{
		if(registers == null)
		{
			registers = new Register[name + 1];
			registers[name] = newRegister;
		}
		else if(name >= registers.length)
		{
			Register[] newRegisters = new Register[
				Math.min(1<<16,name * 2)];
			System.arraycopy(registers,0,newRegisters,0,
				registers.length);
			registers = newRegisters;
			registers[name] = newRegister;
		}
		else
		{
			Register register = registers[name];

			if(register instanceof ClipboardRegister)
			{
				if(newRegister instanceof StringRegister)
				{
					((ClipboardRegister)register).setValue(
						newRegister.toString());
				}
			}
			else
			{
				if(register != null)
					register.dispose();
				registers[name] = newRegister;
			}
		}

		EditBus.send(new RegistersChanged(null));
	}

	/**
	 * Clears (i.e. it's value to null) the specified register.
	 * @param name The register name
	 */
	public static void clearRegister(char name)
	{
		if(name >= registers.length)
			return;

		Register register = registers[name];
		if(register instanceof ClipboardRegister)
			((ClipboardRegister)register).setValue("");
		else
		{
			if(register != null)
				register.dispose();
			registers[name] = null;
		}

		EditBus.send(new RegistersChanged(null));
	}

	/**
	 * Returns an array of all available registers. Some of the elements
	 * of this array might be null.
	 */
	public static Register[] getRegisters()
	{
		return registers;
	}

	/**
	 * Returns a vector of caret registers. For internal use only.
	 */
	public static Vector getCaretRegisters()
	{
		return caretRegisters;
	}

	/**
	 * A register.
	 */
	public interface Register
	{
		/**
		 * Converts to a string.
		 */
		String toString();

		/**
		 * Called when this register is no longer available. This
		 * could remove listeners, free resources, etc.
		 */
		void dispose();
	}

	/**
	 * Register that points to a location in a file.
	 */
	public static class CaretRegister implements Register
	{
		private String path;
		private int offset;
		private Buffer buffer;
		private Position pos;

		/**
		 * Creates a new caret register.
		 * @param buffer The buffer
		 * @param offset The offset
		 */
		public CaretRegister(Buffer buffer, int offset)
		{
			path = buffer.getPath();
			this.offset = offset;
			this.buffer = buffer;
			try
			{
				pos = buffer.createPosition(offset);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}

			caretRegisters.addElement(this);
		}

		/**
		 * Converts to a string.
		 */
		public String toString()
		{
			if(buffer == null)
				return path + ":" + offset;
			else
				return buffer.getPath() + ":" + pos.getOffset();
		}

		/**
		 * Called when this register is no longer available. This
		 * implementation removes the register from the EditBus,
		 * so that it will no longer receive buffer notifications.
		 */
		public void dispose()
		{
			caretRegisters.removeElement(this);
		}

		/**
		 * Returns the buffer involved, or null if it is not open.
		 */
		public Buffer getBuffer()
		{
			return buffer;
		}

		/**
		 * Returns the buffer involved, opening it if necessary.
		 */
		public Buffer openFile()
		{
			if(buffer == null)
				return jEdit.openFile(null,path);
			else
				return buffer;
		}

		/**
		 * Returns the offset in the buffer.
		 */
		public int getOffset()
		{
			if(pos == null)
				return offset;
			else
				return pos.getOffset();
		}

		void openNotify(Buffer _buffer)
		{
			if(buffer == null && _buffer.getPath().equals(path))
			{
				buffer = _buffer;
				try
				{
					pos = buffer.createPosition(offset);
				}
				catch(BadLocationException bl)
				{
					Log.log(Log.ERROR,this,bl);
				}
			}
		}

		void closeNotify(Buffer _buffer)
		{
			if(_buffer == buffer)
			{
				buffer = null;
				offset = pos.getOffset();
				pos = null;
			}
		}
	}

	/**
	 * A clipboard register. Register "$" should always be an
	 * instance of this.
	 */
	public static class ClipboardRegister implements Register
	{
		/**
		 * Sets the clipboard contents.
		 */
		public void setValue(String value)
		{
			Clipboard clipboard = Toolkit.getDefaultToolkit()
				.getSystemClipboard();
			StringSelection selection = new StringSelection(value);
			clipboard.setContents(selection,null);
		}

		/**
		 * Returns the clipboard contents.
		 */
		public String toString()
		{
			Clipboard clipboard = Toolkit.getDefaultToolkit()
				.getSystemClipboard();
			try
			{
				String selection = (String)(clipboard
					.getContents(this).getTransferData(
					DataFlavor.stringFlavor));

				boolean trailingEOL = (selection.endsWith("\n")
					|| selection.endsWith(System.getProperty(
					"line.separator")));

				// Some Java versions return the clipboard
				// contents using the native line separator,
				// so have to convert it here
				BufferedReader in = new BufferedReader(
					new StringReader(selection));
				StringBuffer buf = new StringBuffer();
				String line;
				while((line = in.readLine()) != null)
				{
					buf.append(line);
					buf.append('\n');
				}
				// remove trailing \n
				if(!trailingEOL)
					buf.setLength(buf.length() - 1);
				return buf.toString();
			}
			catch(Exception e)
			{
				Log.log(Log.NOTICE,this,e);
				return null;
			}
		}

		/**
		 * Called when this register is no longer available. This
		 * implementation does nothing.
		 */
		public void dispose() {}
	}

	/**
	 * Register that stores a string.
	 */
	public static class StringRegister implements Register
	{
		private String value;

		/**
		 * Creates a new string register.
		 * @param value The contents
		 */
		public StringRegister(String value)
		{
			this.value = value;
		}

		/**
		 * Converts to a string.
		 */
		public String toString()
		{
			return value;
		}

		/**
		 * Called when this register is no longer available. This
		 * implementation does nothing.
		 */
		public void dispose() {}
	}

	// private members
	private static Register[] registers;
	private static Vector caretRegisters = new Vector();

	private Registers() {}

	static
	{
		EditBus.addToBus(new CaretRegisterHelper());
		setRegister('$',new ClipboardRegister());
	}

	static class CaretRegisterHelper implements EBComponent
	{
		public void handleMessage(EBMessage msg)
		{
			if(msg instanceof BufferUpdate)
				handleBufferUpdate((BufferUpdate)msg);
		}

		private void handleBufferUpdate(BufferUpdate msg)
		{
			Buffer _buffer = msg.getBuffer();
			if(msg.getWhat() == BufferUpdate.CREATED)
			{
				for(int i = 0; i < caretRegisters.size(); i++)
				{
					((CaretRegister)caretRegisters.elementAt(i))
						.openNotify(_buffer);
				}
			}
			else if(msg.getWhat() == BufferUpdate.CLOSED)
			{
				for(int i = 0; i < caretRegisters.size(); i++)
				{
					((CaretRegister)caretRegisters.elementAt(i))
						.closeNotify(_buffer);
				}
			}
		}
	}
}

/*
 * ChangeLog:
 * $Log: Registers.java,v $
 * Revision 1.18  2001/04/19 08:07:25  sp
 * Macros.input(view,prompt,defaultValue) added
 *
 * Revision 1.17  2001/03/23 10:38:50  sp
 * stuffs
 *
 * Revision 1.16  2000/12/01 07:39:59  sp
 * Batch search renamed to HyperSearch, bug fixes
 *
 * Revision 1.15  2000/11/19 07:51:25  sp
 * Documentation updates, bug fixes
 *
 * Revision 1.14  2000/11/13 11:19:26  sp
 * Search bar reintroduced, more BeanShell stuff
 *
 * Revision 1.13  2000/11/12 05:36:48  sp
 * BeanShell integration started
 *
 * Revision 1.12  2000/11/11 05:37:52  sp
 * paste bug fixed
 *
 * Revision 1.11  2000/11/11 02:59:29  sp
 * FTP support moved out of the core into a plugin
 *
 * Revision 1.10  2000/06/12 02:43:29  sp
 * pre6 almost ready
 *
 * Revision 1.9  2000/05/22 12:05:45  sp
 * Markers are highlighted in the gutter, bug fixes
 *
 * Revision 1.8  2000/03/20 03:42:55  sp
 * Smoother syntax package, opening an already open file will ask if it should be
 * reloaded, maybe some other changes
 *
 * Revision 1.7  2000/02/15 07:44:30  sp
 * bug fixes, doc updates, etc
 *
 * Revision 1.6  1999/11/21 01:20:30  sp
 * Bug fixes, EditBus updates, fixed some warnings generated by jikes +P
 *
 * Revision 1.5  1999/11/19 08:54:51  sp
 * EditBus integrated into the core, event system gone, bug fixes
 *
 * Revision 1.4  1999/11/07 06:51:43  sp
 * Check box menu items supported
 *
 * Revision 1.3  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 * Revision 1.2  1999/10/23 03:48:22  sp
 * Mode system overhaul, close all dialog box, misc other stuff
 *
 * Revision 1.1  1999/10/03 04:13:26  sp
 * Forgot to add some files
 *
 */
