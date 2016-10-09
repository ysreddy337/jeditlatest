/*
 * TextAreaPainter.java - Paints the text area
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

package org.gjt.sp.jedit.textarea;

import javax.swing.text.*;
import javax.swing.JComponent;
import java.awt.event.MouseEvent;
import java.awt.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.util.Log;

/**
 * The text area repaint manager. It performs double buffering and paints
 * lines of text.
 * @author Slava Pestov
 * @version $Id: TextAreaPainter.java,v 1.49 2000/11/11 02:59:31 sp Exp $
 */
public class TextAreaPainter extends JComponent implements TabExpander
{
	/**
	 * Creates a new painter. Do not create instances of this class
	 * directly.
	 */
	public TextAreaPainter(JEditTextArea textArea)
	{
		enableEvents(AWTEvent.FOCUS_EVENT_MASK
			| AWTEvent.KEY_EVENT_MASK
			| AWTEvent.MOUSE_EVENT_MASK);

		this.textArea = textArea;

		setAutoscrolls(true);
		setDoubleBuffered(true);
		setOpaque(true);

		setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

		setFont(new Font("Monospaced",Font.PLAIN,14));
		setForeground(Color.black);
		setBackground(Color.white);

		cols = 80;
		rows = 25;
	}

	/**
	 * Returns if this component can be traversed by pressing the
	 * Tab key. This returns false.
	 */
	public final boolean isManagingFocus()
	{
		return false;
	}

	/**
	 * Returns the syntax styles used to paint colorized text. Entry <i>n</i>
	 * will be used to paint tokens with id = <i>n</i>.
	 * @see org.gjt.sp.jedit.syntax.Token
	 */
	public final SyntaxStyle[] getStyles()
	{
		return styles;
	}

	/**
	 * Sets the syntax styles used to paint colorized text. Entry <i>n</i>
	 * will be used to paint tokens with id = <i>n</i>.
	 * @param styles The syntax styles
	 * @see org.gjt.sp.jedit.syntax.Token
	 */
	public final void setStyles(SyntaxStyle[] styles)
	{
		this.styles = styles;
		repaint();
	}

	/**
	 * Returns the caret color.
	 */
	public final Color getCaretColor()
	{
		return caretColor;
	}

	/**
	 * Sets the caret color.
	 * @param caretColor The caret color
	 */
	public final void setCaretColor(Color caretColor)
	{
		this.caretColor = caretColor;
		invalidateSelectedLines();
	}

	/**
	 * Returns the selection color.
	 */
	public final Color getSelectionColor()
	{
		return selectionColor;
	}

	/**
	 * Sets the selection color.
	 * @param selectionColor The selection color
	 */
	public final void setSelectionColor(Color selectionColor)
	{
		this.selectionColor = selectionColor;
		invalidateSelectedLines();
	}

	/**
	 * Returns the line highlight color.
	 */
	public final Color getLineHighlightColor()
	{
		return lineHighlightColor;
	}

	/**
	 * Sets the line highlight color.
	 * @param lineHighlightColor The line highlight color
	 */
	public final void setLineHighlightColor(Color lineHighlightColor)
	{
		this.lineHighlightColor = lineHighlightColor;
		invalidateSelectedLines();
	}

	/**
	 * Returns true if line highlight is enabled, false otherwise.
	 */
	public final boolean isLineHighlightEnabled()
	{
		return lineHighlight;
	}

	/**
	 * Enables or disables current line highlighting.
	 * @param lineHighlight True if current line highlight should be enabled,
	 * false otherwise
	 */
	public final void setLineHighlightEnabled(boolean lineHighlight)
	{
		this.lineHighlight = lineHighlight;
		invalidateSelectedLines();
	}

	/**
	 * Returns the bracket highlight color.
	 */
	public final Color getBracketHighlightColor()
	{
		return bracketHighlightColor;
	}

	/**
	 * Sets the bracket highlight color.
	 * @param bracketHighlightColor The bracket highlight color
	 */
	public final void setBracketHighlightColor(Color bracketHighlightColor)
	{
		this.bracketHighlightColor = bracketHighlightColor;
		invalidateLine(textArea.getBracketLine());
	}

	/**
	 * Returns true if bracket highlighting is enabled, false otherwise.
	 * When bracket highlighting is enabled, the bracket matching the
	 * one before the caret (if any) is highlighted.
	 */
	public final boolean isBracketHighlightEnabled()
	{
		return bracketHighlight;
	}

	/**
	 * Enables or disables bracket highlighting.
	 * When bracket highlighting is enabled, the bracket matching the
	 * one before the caret (if any) is highlighted.
	 * @param bracketHighlight True if bracket highlighting should be
	 * enabled, false otherwise
	 */
	public final void setBracketHighlightEnabled(boolean bracketHighlight)
	{
		this.bracketHighlight = bracketHighlight;
		invalidateLine(textArea.getBracketLine());
	}

	/**
	 * Returns true if the caret should be drawn as a block, false otherwise.
	 */
	public final boolean isBlockCaretEnabled()
	{
		return blockCaret;
	}

	/**
	 * Sets if the caret should be drawn as a block, false otherwise.
	 * @param blockCaret True if the caret should be drawn as a block,
	 * false otherwise.
	 */
	public final void setBlockCaretEnabled(boolean blockCaret)
	{
		this.blockCaret = blockCaret;
		invalidateSelectedLines();
	}

	/**
	 * Returns the EOL marker color.
	 */
	public final Color getEOLMarkerColor()
	{
		return eolMarkerColor;
	}

	/**
	 * Sets the EOL marker color.
	 * @param eolMarkerColor The EOL marker color
	 */
	public final void setEOLMarkerColor(Color eolMarkerColor)
	{
		this.eolMarkerColor = eolMarkerColor;
		repaint();
	}

	/**
	 * Returns true if EOL markers are drawn, false otherwise.
	 */
	public final boolean getEOLMarkersPainted()
	{
		return eolMarkers;
	}

	/**
	 * Sets if EOL markers are to be drawn.
	 * @param eolMarkers True if EOL markers should be drawn, false otherwise
	 */
	public final void setEOLMarkersPainted(boolean eolMarkers)
	{
		this.eolMarkers = eolMarkers;
		repaint();
	}

	/**
	 * Returns the wrap guide color.
	 */
	public final Color getWrapGuideColor()
	{
		return wrapGuideColor;
	}

	/**
	 * Sets the wrap guide color.
	 * @param wrapGuideColor The wrap guide color
	 */
	public final void setWrapGuideColor(Color wrapGuideColor)
	{
		this.wrapGuideColor = wrapGuideColor;
		repaint();
	}

	/**
	 * Returns true if the wrap guide is drawn, false otherwise.
	 */
	public final boolean getWrapGuidePainted()
	{
		return wrapGuide;
	}

	/**
	 * Sets if the wrap guide is to be drawn.
	 * @param wrapGuide True if the wrap guide should be drawn, false otherwise
	 */
	public final void setWrapGuidePainted(boolean wrapGuide)
	{
		this.wrapGuide = wrapGuide;
		repaint();
	}

	/**
	 * Adds a custom highlight painter.
	 * @param highlight The highlight
	 */
	public void addCustomHighlight(TextAreaHighlight highlight)
	{
		highlight.init(textArea,highlights);
		highlights = highlight;
	}

	/**
	 * Returns the tool tip to display at the specified location.
	 * @param evt The mouse event
	 */
	public String getToolTipText(MouseEvent evt)
	{
		if(maxLineLen != 0)
		{
			int wrapGuidePos = maxLineLen + textArea.getHorizontalOffset();
			if(Math.abs(evt.getX() - wrapGuidePos) < 5)
			{
				return String.valueOf(textArea.getBuffer()
					.getProperty("maxLineLen"));
			}
		}

		if(highlights != null)
			return highlights.getToolTipText(evt);
		else
			return null;
	}

	/**
	 * Returns the font metrics used by this component.
	 */
	public FontMetrics getFontMetrics()
	{
		return fm;
	}

	/**
	 * Sets the font for this component. This is overridden to update the
	 * cached font metrics and to recalculate which lines are visible.
	 * @param font The font
	 */
	public void setFont(Font font)
	{
		super.setFont(font);
		fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
		textArea.recalculateVisibleLines();

		updateTabSize();
	}

	/**
	 * Repaints the text.
	 * @param g The graphics context
	 */
	public void paint(Graphics gfx)
	{
		updateTabSize();

		Rectangle clipRect = gfx.getClipBounds();

		gfx.setColor(getBackground());
		gfx.fillRect(clipRect.x,clipRect.y,clipRect.width,clipRect.height);

		int x = textArea.getHorizontalOffset();

		// We don't use yToLine() here because that method doesn't
		// return lines past the end of the buffer
		int height = fm.getHeight();
		int firstLine = textArea.getFirstLine();
		int firstInvalid = firstLine + clipRect.y / height;
		// Because the clipRect's height is usually an even multiple
		// of the font height, we subtract 1 from it, otherwise one
		// too many lines will always be painted.
		int lastInvalid = firstLine + (clipRect.y + clipRect.height - 1) / height;
		int lineCount = textArea.getLineCount();

		try
		{
			TokenMarker tokenMarker = textArea.getTokenMarker();
			int maxWidth = textArea.maxHorizontalScrollWidth;

			boolean updateMaxHorizontalScrollWidth = false;
			for(int line = firstInvalid; line <= lastInvalid; line++)
			{
				boolean valid = textArea.getBuffer().isLoaded()
					&& line >= 0 && line < lineCount;

				int width = paintLine(gfx,tokenMarker,valid,line,x)
					- x + 5 /* Yay */;
				if(valid)
				{
					tokenMarker.setLineWidth(line,width);
					if(width > maxWidth)
						updateMaxHorizontalScrollWidth = true;
				}
			}

			if(tokenMarker.isNextLineRequested())
			{
				int h = clipRect.y + clipRect.height;
				repaint(0,h,getWidth(),getHeight() - h);
			}

			if(updateMaxHorizontalScrollWidth)
				textArea.updateMaxHorizontalScrollWidth();
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,"Error repainting line"
				+ " range {" + firstInvalid + ","
				+ lastInvalid + "}:");
			Log.log(Log.ERROR,this,e);
		}
	}

	/**
	 * Marks a line as needing a repaint.
	 * @param line The line to invalidate
	 */
	public final void invalidateLine(int line)
	{
		repaint(0,textArea.lineToY(line) + fm.getDescent() + fm.getLeading(),
			getWidth(),fm.getHeight());
	}

	/**
	 * Marks a range of lines as needing a repaint.
	 * @param firstLine The first line to invalidate
	 * @param lastLine The last line to invalidate
	 */
	public final void invalidateLineRange(int firstLine, int lastLine)
	{
		repaint(0,textArea.lineToY(firstLine) + fm.getDescent() + fm.getLeading(),
			getWidth(),(lastLine - firstLine + 1) * fm.getHeight());
	}

	/**
	 * Repaints the lines containing the selection.
	 */
	public final void invalidateSelectedLines()
	{
		invalidateLineRange(textArea.getSelectionStartLine(),
			textArea.getSelectionEndLine());
	}

	/**
	 * Implementation of TabExpander interface. Returns next tab stop after
	 * a specified point.
	 * @param x The x co-ordinate
	 * @param tabOffset Ignored
	 * @return The next tab stop after <i>x</i>
	 */
	public float nextTabStop(float x, int tabOffset)
	{
		int offset = textArea.getHorizontalOffset();
		int ntabs = ((int)x - offset) / tabSize;
		return (ntabs + 1) * tabSize + offset;
	}

	/**
	 * Returns the painter's preferred size.
	 */
	public Dimension getPreferredSize()
	{
		Dimension dim = new Dimension();
		dim.width = fm.charWidth('w') * cols;
		dim.height = fm.getHeight() * rows;
		return dim;
	}

	/**
	 * Returns the painter's minimum size.
	 */
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

	// package-private members
	void updateTabSize()
	{
		if(textArea.getBuffer() == null)
			return;

		tabSize = fm.charWidth(' ') * ((Integer)textArea
			.getBuffer().getProperty(
			PlainDocument.tabSizeAttribute)).intValue();

		int _maxLineLen = ((Integer)textArea.getBuffer()
			.getProperty("maxLineLen")).intValue();

		if(_maxLineLen <= 0)
			maxLineLen = 0;
		else
			maxLineLen = fm.charWidth(' ') * _maxLineLen;
	}

	// private members
	private JEditTextArea textArea;

	private SyntaxStyle[] styles;
	private Color caretColor;
	private Color selectionColor;
	private Color lineHighlightColor;
	private Color bracketHighlightColor;
	private Color eolMarkerColor;
	private Color wrapGuideColor;

	private boolean blockCaret;
	private boolean lineHighlight;
	private boolean bracketHighlight;
	private boolean eolMarkers;
	private boolean wrapGuide;
	private int cols;
	private int rows;

	private int tabSize;
	private int maxLineLen;
	private FontMetrics fm;

	private TextAreaHighlight highlights;

	private int paintLine(Graphics gfx, TokenMarker tokenMarker,
		boolean valid, int line, int x)
	{
		int y = textArea.lineToY(line);

		paintHighlight(gfx,line,y,valid);

		if(maxLineLen != 0 && wrapGuide)
		{
			gfx.setColor(wrapGuideColor);
			int firstLine = textArea.getFirstLine();
			gfx.drawLine(x + maxLineLen,(line - firstLine) * fm.getHeight(),
				x + maxLineLen,
				(line - firstLine + 1) * fm.getHeight());
		}

		if(valid)
		{
			Font defaultFont = getFont();
			Color defaultColor = getForeground();

			gfx.setFont(defaultFont);
			gfx.setColor(defaultColor);

			x = tokenMarker.paintSyntaxLine(textArea.getBuffer(),line,
				styles,this,gfx,getBackground(),x,y + fm.getHeight());

			if(eolMarkers)
			{
				gfx.setFont(defaultFont);
				gfx.setColor(eolMarkerColor);
				gfx.drawString(".",x,y + fm.getHeight());
			}

			if(valid && line == textArea.getCaretLine() && textArea.isCaretVisible())
				paintCaret(gfx,line,y);
		}

		return x;
	}

	private void paintHighlight(Graphics gfx, int line, int y, boolean valid)
	{
		if(valid)
		{
			if(line >= textArea.getSelectionStartLine()
				&& line <= textArea.getSelectionEndLine())
				paintLineHighlight(gfx,line,y);

			if(bracketHighlight && line == textArea.getBracketLine()
				&& textArea.isHighlightVisible())
				paintBracketHighlight(gfx,line,y);
		}

		if(highlights != null)
			highlights.paintHighlight(gfx,line,y);
	}

	private void paintLineHighlight(Graphics gfx, int line, int y)
	{
		int height = fm.getHeight();
		y += fm.getLeading() + fm.getDescent();

		int selectionStart = textArea.getSelectionStart();
		int selectionEnd = textArea.getSelectionEnd();

		if(selectionStart == selectionEnd)
		{
			if(lineHighlight)
			{
				gfx.setColor(lineHighlightColor);
				gfx.fillRect(0,y,getWidth(),height);
			}
		}
		else
		{
			gfx.setColor(selectionColor);

			int selectionStartLine = textArea.getSelectionStartLine();
			int selectionEndLine = textArea.getSelectionEndLine();
			int lineStart = textArea.getLineStartOffset(line);

			int x1, x2;
			if(textArea.isSelectionRectangular())
			{
				int lineLen = textArea.getLineLength(line);
				x1 = textArea.offsetToX(line,Math.min(lineLen,
					selectionStart - textArea.getLineStartOffset(
					selectionStartLine)));
				x2 = textArea.offsetToX(line,Math.min(lineLen,
					selectionEnd - textArea.getLineStartOffset(
					selectionEndLine)));
				if(x1 == x2)
					x2++;
			}
			else if(selectionStartLine == selectionEndLine)
			{
				x1 = textArea.offsetToX(line,
					selectionStart - lineStart);
				x2 = textArea.offsetToX(line,
					selectionEnd - lineStart);
			}
			else if(line == selectionStartLine)
			{
				x1 = textArea.offsetToX(line,
					selectionStart - lineStart);
				x2 = getWidth();
			}
			else if(line == selectionEndLine)
			{
				x1 = 0;
				x2 = textArea.offsetToX(line,
					selectionEnd - lineStart);
			}
			else
			{
				x1 = 0;
				x2 = getWidth();
			}

			// "inlined" min/max()
			gfx.fillRect(x1 > x2 ? x2 : x1,y,x1 > x2 ?
				(x1 - x2) : (x2 - x1),height);
		}

	}

	private void paintBracketHighlight(Graphics gfx, int line, int y)
	{
		int position = textArea.getBracketPosition();
		if(position == -1)
			return;
		y += fm.getLeading() + fm.getDescent();
		int x = textArea.offsetToX(line,position);
		gfx.setColor(bracketHighlightColor);
		// Hack!!! Since there is no fast way to get the character
		// from the bracket matching routine, we use ( since all
		// brackets probably have the same width anyway
		gfx.drawRect(x,y,fm.charWidth('(') - 1,
			fm.getHeight() - 1);
	}

	private void paintCaret(Graphics gfx, int line, int y)
	{
		int offset = textArea.getCaretPosition() 
			- textArea.getLineStartOffset(line);
		int caretX = textArea.offsetToX(line,offset);
		int height = fm.getHeight();

		y += fm.getLeading() + fm.getDescent();

		gfx.setColor(caretColor);

		if(textArea.isOverwriteEnabled())
		{
			gfx.drawLine(caretX,y + height - 1,
				caretX + fm.charWidth('w'),y + height - 1);
		}
		else if(blockCaret)
		{
			if(textArea.getSelectionStart() == textArea.getSelectionEnd()
				&& lineHighlight)
				gfx.setXORMode(lineHighlightColor);
			else
				gfx.setXORMode(getBackground());

			gfx.fillRect(caretX,y,fm.charWidth('w'),height);
			gfx.setPaintMode();
		}
		else
		{
			gfx.drawLine(caretX,y,caretX,y + height - 1);
		}
	}
}

/*
 * ChangeLog:
 * $Log: TextAreaPainter.java,v $
 * Revision 1.49  2000/11/11 02:59:31  sp
 * FTP support moved out of the core into a plugin
 *
 * Revision 1.48  2000/11/07 10:08:33  sp
 * Options dialog improvements, documentation changes, bug fixes
 *
 * Revision 1.47  2000/11/05 05:25:46  sp
 * Word wrap, format and remove-trailing-ws commands from TextTools moved into core
 *
 * Revision 1.46  2000/11/05 00:44:15  sp
 * Improved HyperSearch, improved horizontal scroll, other stuff
 *
 * Revision 1.45  2000/11/02 09:19:34  sp
 * more features
 *
 * Revision 1.44  2000/10/30 07:14:04  sp
 * 2.7pre1 branched, GUI improvements
 *
 * Revision 1.43  2000/10/28 00:36:58  sp
 * ML mode, Haskell mode
 *
 * Revision 1.42  2000/10/12 09:28:27  sp
 * debugging and polish
 *
 * Revision 1.41  2000/09/26 10:19:47  sp
 * Bug fixes, spit and polish
 *
 * Revision 1.40  2000/07/22 03:27:04  sp
 * threaded I/O improved, autosave rewrite started
 *
 * Revision 1.39  2000/07/14 06:00:45  sp
 * bracket matching now takes syntax info into account
 *
 * Revision 1.38  2000/06/24 06:24:56  sp
 * work thread bug fixes
 *
 * Revision 1.37  2000/05/23 04:04:53  sp
 * Marker highlight updates, next/prev-marker actions
 *
 * Revision 1.36  2000/05/22 12:05:46  sp
 * Markers are highlighted in the gutter, bug fixes
 *
 * Revision 1.35  2000/05/10 08:22:21  sp
 * EOL marker bug fix, documentation updates
 *
 */
