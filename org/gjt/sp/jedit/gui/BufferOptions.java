/*
 * BufferOptions.java - Buffer-specific options dialog
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

package org.gjt.sp.jedit.gui;

import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.*;

/**
 * Buffer-specific options dialog.
 * @author Slava Pestov
 * @version $Id: BufferOptions.java,v 1.24 2000/12/08 04:03:43 sp Exp $
 */
public class BufferOptions extends EnhancedDialog
{
	public BufferOptions(View view, Buffer buffer)
	{
		super(view,jEdit.getProperty("buffer-options.title"),true);
		this.view = view;
		this.buffer = buffer;

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		ActionHandler actionListener = new ActionHandler();
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		Insets nullInsets = new Insets(0,0,0,0);
		Insets labelInsets = new Insets(0,0,0,12);

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridx = cons.gridy = 0;
		cons.gridwidth = cons.gridheight = 1;
		cons.fill = GridBagConstraints.BOTH;
		cons.weightx = 0.0f;
		cons.insets = labelInsets;

		// Edit mode
		cons.gridx = 0;
		cons.weightx = 0.0f;
		cons.insets = labelInsets;
		JLabel label = new JLabel(jEdit.getProperty(
			"buffer-options.mode"),SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		cons.gridx = 1;
		cons.weightx = 1.0f;
		cons.insets = nullInsets;
		modes = jEdit.getModes();
		String bufferMode = buffer.getMode().getName();
		int index = 0;
		String[] modeNames = new String[modes.length];
		for(int i = 0; i < modes.length; i++)
		{
			Mode mode = modes[i];
			modeNames[i] = mode.getName();
			if(bufferMode.equals(mode.getName()))
				index = i;
		}
		mode = new JComboBox(modeNames);
		mode.setSelectedIndex(index);
		mode.addActionListener(actionListener);
		layout.setConstraints(mode,cons);
		panel.add(mode);

		// Tab size
		cons.gridx = 0;
		cons.gridy = 1;
		cons.weightx = 0.0f;
		cons.insets = labelInsets;
		label = new JLabel(jEdit.getProperty(
			"options.editing.tabSize"),SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		cons.gridx = 1;
		cons.weightx = 1.0f;
		cons.insets = nullInsets;
		String[] tabSizes = { "2", "4", "8" };
		tabSize = new JComboBox(tabSizes);
		tabSize.setEditable(true);
		tabSize.setSelectedItem(buffer.getProperty("tabSize"));
		tabSize.addActionListener(actionListener);
		layout.setConstraints(tabSize,cons);
		panel.add(tabSize);

		// Indent size
		cons.gridx = 0;
		cons.gridy = 2;
		cons.weightx = 0.0f;
		cons.insets = labelInsets;
		label = new JLabel(jEdit.getProperty(
			"options.editing.indentSize"),SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		cons.gridx = 1;
		cons.weightx = 1.0f;
		cons.insets = nullInsets;
		indentSize = new JComboBox(tabSizes);
		indentSize.setEditable(true);
		indentSize.setSelectedItem(buffer.getProperty("indentSize"));
		indentSize.addActionListener(actionListener);
		layout.setConstraints(indentSize,cons);
		panel.add(indentSize);

		// Max line length
		cons.gridx = 0;
		cons.gridy = 3;
		cons.weightx = 0.0f;
		cons.insets = labelInsets;
		label = new JLabel(jEdit.getProperty(
			"options.editing.maxLineLen"),SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		String[] lineLengths = { "0", "72", "76", "80" };

		cons.gridx = 1;
		cons.weightx = 1.0f;
		cons.insets = nullInsets;
		maxLineLen = new JComboBox(lineLengths);
		maxLineLen.setEditable(true);
		maxLineLen.setSelectedItem(buffer.getProperty("maxLineLen"));
		maxLineLen.addActionListener(actionListener);
		layout.setConstraints(maxLineLen,cons);
		panel.add(maxLineLen);

		// Line separator
		cons.gridx = 0;
		cons.gridy = 4;
		cons.weightx = 0.0f;
		cons.insets = labelInsets;
		label = new JLabel(jEdit.getProperty("buffer-options.lineSeparator"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		cons.gridx = 1;
		cons.weightx = 1.0f;
		cons.insets = nullInsets;
		String[] lineSeps = { jEdit.getProperty("lineSep.unix"),
			jEdit.getProperty("lineSep.windows"),
			jEdit.getProperty("lineSep.mac") };
		lineSeparator = new JComboBox(lineSeps);
		String lineSep = (String)buffer.getProperty(Buffer.LINESEP);
		if(lineSep == null)
			lineSep = System.getProperty("line.separator");
		if("\n".equals(lineSep))
			lineSeparator.setSelectedIndex(0);
		else if("\r\n".equals(lineSep))
			lineSeparator.setSelectedIndex(1);
		else if("\r".equals(lineSep))
			lineSeparator.setSelectedIndex(2);
		lineSeparator.addActionListener(actionListener);
		layout.setConstraints(lineSeparator,cons);
		panel.add(lineSeparator);

		// Syntax highlighting
		cons.gridx = 0;
		cons.gridy = 5;
		cons.weightx = 0.0f;
		cons.gridwidth = cons.REMAINDER;
		cons.fill = GridBagConstraints.NONE;
		cons.anchor = GridBagConstraints.WEST;
		syntax = new JCheckBox(jEdit.getProperty(
			"options.editing.syntax"));
		syntax.setSelected(buffer.getBooleanProperty("syntax"));
		syntax.addActionListener(actionListener);
		layout.setConstraints(syntax,cons);
		panel.add(syntax);

		// Indent on tab
		cons.gridy = 6;
		indentOnTab = new JCheckBox(jEdit.getProperty(
			"options.editing.indentOnTab"));
		indentOnTab.setSelected(buffer.getBooleanProperty("indentOnTab"));
		indentOnTab.addActionListener(actionListener);
		layout.setConstraints(indentOnTab,cons);
		panel.add(indentOnTab);

		// Indent on enter
		cons.gridy = 7;
		indentOnEnter = new JCheckBox(jEdit.getProperty(
			"options.editing.indentOnEnter"));
		indentOnEnter.setSelected(buffer.getBooleanProperty("indentOnEnter"));
		indentOnEnter.addActionListener(actionListener);
		layout.setConstraints(indentOnEnter,cons);
		panel.add(indentOnEnter);

		// Soft tabs
		cons.gridy = 8;
		noTabs = new JCheckBox(jEdit.getProperty(
			"options.editing.noTabs"));
		noTabs.setSelected(buffer.getBooleanProperty("noTabs"));
		noTabs.addActionListener(actionListener);
		layout.setConstraints(noTabs,cons);
		panel.add(noTabs);

		// Props label
		cons.gridy = 9;
		cons.insets = new Insets(6,0,6,0);
		label = new JLabel(jEdit.getProperty("buffer-options.props"));
		layout.setConstraints(label,cons);
		panel.add(label);

		content.add(BorderLayout.NORTH,panel);

		props = new JTextArea(4,4);
		props.setLineWrap(true);
		props.setWrapStyleWord(false);
		content.add(BorderLayout.CENTER,new JScrollPane(props));
		updatePropsField();

		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(12,0,0,0));
		panel.add(Box.createGlue());
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(actionListener);
		getRootPane().setDefaultButton(ok);
		panel.add(ok);
		panel.add(Box.createHorizontalStrut(6));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(actionListener);
		panel.add(cancel);
		panel.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,panel);

		pack();
		setLocationRelativeTo(view);
		show();
	}

	// EnhancedDialog implementation
	public void ok()
	{
		try
		{
			buffer.putProperty("tabSize",new Integer(
				tabSize.getSelectedItem().toString()));
		}
		catch(NumberFormatException nf)
		{
		}

		try
		{
			buffer.putProperty("indentSize",new Integer(
				indentSize.getSelectedItem().toString()));
		}
		catch(NumberFormatException nf)
		{
		}

		try
		{
			buffer.putProperty("maxLineLen",new Integer(
				maxLineLen.getSelectedItem().toString()));
		}
		catch(NumberFormatException nf)
		{
		}

		int index = mode.getSelectedIndex();
		buffer.setMode(modes[index]);

		index = lineSeparator.getSelectedIndex();
		String lineSep;
		if(index == 0)
			lineSep = "\n";
		else if(index == 1)
			lineSep = "\r\n";
		else if(index == 2)
			lineSep = "\r";
		else
			throw new InternalError();

		String oldLineSep = (String)buffer.getProperty("lineSeparator");
		if(oldLineSep == null)
			oldLineSep = System.getProperty("line.separator");
		if(!oldLineSep.equals(lineSep))
		{
			buffer.putProperty("lineSeparator",lineSep);
			buffer.setDirty(true);
		}

		buffer.putBooleanProperty("syntax",syntax.isSelected());
		buffer.putBooleanProperty("indentOnTab",indentOnTab.isSelected());
		buffer.putBooleanProperty("indentOnEnter",indentOnEnter.isSelected());
		buffer.putBooleanProperty("noTabs",noTabs.isSelected());

		buffer.propertiesChanged();
		dispose();

		// Update text area
		view.getTextArea().getPainter().repaint();
	}

	public void cancel()
	{
		dispose();
	}
	// end EnhancedDialog implementation

        // private members
	private View view;
	private Buffer buffer;
	private JComboBox tabSize;
	private JComboBox indentSize;
	private JComboBox maxLineLen;
	private Mode[] modes;
	private JComboBox mode;
	private JComboBox lineSeparator;
	private JCheckBox indentOnTab;
	private JCheckBox indentOnEnter;
	private JCheckBox syntax;
	private JCheckBox noTabs;
	private JTextArea props;
	private JButton ok;
	private JButton cancel;

	private void updatePropsField()
	{
		props.setText(":mode=" + modes[mode.getSelectedIndex()].getName()
			+ ":tabSize=" + tabSize.getSelectedItem()
			+ ":indentSize=" + indentSize.getSelectedItem()
			+ ":noTabs=" + noTabs.isSelected()
			+ ":indentOnTab=" + indentOnTab.isSelected()
			+ ":indentOnEnter=" + indentOnEnter.isSelected()
			+ ":syntax=" + syntax.isSelected()
			+ ":maxLineLen=" + maxLineLen.getSelectedItem()
			+ ":");
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == ok)
				ok();
			else if(source == cancel)
				cancel();
			else if(source == mode)
			{
				Mode _mode = jEdit.getMode((String)
					mode.getSelectedItem());
				tabSize.setSelectedItem(_mode.getProperty(
					"tabSize"));
				indentSize.setSelectedItem(_mode.getProperty(
					"indentSize"));
				maxLineLen.setSelectedItem(_mode.getProperty(
					"maxLineLen"));
				indentOnTab.setSelected(_mode.getBooleanProperty(
					"indentOnTab"));
				indentOnEnter.setSelected(_mode.getBooleanProperty(
					"indentOnEnter"));
				syntax.setSelected(_mode.getBooleanProperty(
					"syntax"));
				noTabs.setSelected(_mode.getBooleanProperty(
					"noTabs"));
				updatePropsField();
			}
			else
				updatePropsField();
		}
	}
}

/*
 * ChangeLog:
 * $Log: BufferOptions.java,v $
 * Revision 1.24  2000/12/08 04:03:43  sp
 * bug fixes
 *
 * Revision 1.23  2000/12/06 07:00:40  sp
 * Lotsa bug fixes
 *
 * Revision 1.22  2000/11/12 05:36:49  sp
 * BeanShell integration started
 *
 * Revision 1.21  2000/11/07 10:08:31  sp
 * Options dialog improvements, documentation changes, bug fixes
 *
 * Revision 1.20  2000/11/05 05:25:46  sp
 * Word wrap, format and remove-trailing-ws commands from TextTools moved into core
 *
 * Revision 1.19  2000/11/02 09:19:33  sp
 * more features
 *
 * Revision 1.18  2000/10/30 07:14:04  sp
 * 2.7pre1 branched, GUI improvements
 *
 * Revision 1.17  2000/07/15 10:10:17  sp
 * improved printing
 *
 * Revision 1.16  2000/05/21 03:00:51  sp
 * Code cleanups and bug fixes
 *
 * Revision 1.15  2000/04/25 11:00:20  sp
 * FTP VFS hacking, some other stuff
 *
 * Revision 1.14  2000/04/15 04:14:47  sp
 * XML files updated, jEdit.get/setBooleanProperty() method added
 *
 * Revision 1.13  2000/04/07 06:57:26  sp
 * Buffer options dialog box updates, API docs updated a bit in syntax package
 *
 * Revision 1.12  1999/11/26 07:37:11  sp
 * Escape/enter handling code moved to common superclass, bug fixes
 *
 * Revision 1.11  1999/10/23 03:48:22  sp
 * Mode system overhaul, close all dialog box, misc other stuff
 *
 * Revision 1.10  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.9  1999/08/21 01:48:18  sp
 * jEdit 2.0pre8
 *
 * Revision 1.8  1999/07/16 23:45:49  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.7  1999/04/19 05:44:34  sp
 * GUI updates
 *
 */
