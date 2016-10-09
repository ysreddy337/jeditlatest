/*
 * XModeHandler.java - XML handler for mode files
 * Copyright (C) 1999 mike dillon
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

package org.gjt.sp.jedit.syntax;

import com.microstar.xml.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Stack;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class XModeHandler extends HandlerBase
{
	// public members
	public XModeHandler (String path)
	{
		this.path = path;
	}

	// begin HandlerBase implementation
	public Object resolveEntity(String publicId, String systemId)
	{
		if("xmode.dtd".equals(systemId))
		{
			String path = MiscUtilities.constructPath(
				jEdit.getJEditHome(),"modes","xmode.dtd");
			try
			{
				return new BufferedReader(new FileReader(path));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,"Error while opening"
					+ " xmode.dtd:");
				Log.log(Log.ERROR,this,e);
			}
		}

		return null;
	}

	public void attribute(String aname, String value, boolean isSpecified)
	{
		String tag = peekElement();
		aname = (aname == null) ? null : aname.intern();
		value = (value == null) ? null : value.intern();

		if (aname == "NAME")
		{
			// first NAME is the mode name
			if(modeName == null)
			{
				modeName = value;
				marker.setName(modeName);
			}
			else
			{
				propName = value;
			}
		}
		else if (aname == "VALUE")
		{
			propValue = value;
		}
		else if (aname == "TYPE")
		{
			lastTokenID = stringToToken(value);
		}
		else if (aname == "AT_LINE_START")
		{
			lastAtLineStart = (isSpecified) ? (value == "TRUE") :
				false;
		}
		else if (aname == "NO_LINE_BREAK")
		{
			lastNoLineBreak = (isSpecified) ? (value == "TRUE") :
				false;
		}
		else if (aname == "NO_WORD_BREAK")
		{
			lastNoWordBreak = (isSpecified) ? (value == "TRUE") :
				false;
		}
		else if (aname == "EXCLUDE_MATCH")
		{
			lastExcludeMatch = (isSpecified) ? (value == "TRUE") :
				false;
		}
		else if (aname == "IGNORE_CASE")
		{
			lastIgnoreCase = (isSpecified) ? (value != "FALSE") :
				true;
		}
		else if (aname == "HIGHLIGHT_DIGITS")
		{
			lastHighlightDigits = (isSpecified) ? (value != "FALSE") :
				false;
		}
		else if (aname == "AT_CHAR")
		{
			try
			{
				if (isSpecified) termChar =
					Integer.parseInt(value);
			}
			catch (NumberFormatException e)
			{
				String[] args = { path, value };
				GUIUtilities.error(null,"xmode-termchar-invalid",args);
				termChar = -1;
			}
		}
		else if (aname == "ESCAPE")
		{
			lastEscape = value;
		}
		else if (aname == "SET")
		{
			lastSetName = value;
		}
		else if (aname == "DELEGATE")
		{
			lastDelegateSet = value;
		}
		else if (aname == "DEFAULT")
		{
			lastDefaultID = stringToToken(value);
		}
	}

	public void doctypeDecl(String name, String publicId,
		String systemId) throws Exception
	{
		if ("MODE".equalsIgnoreCase(name)) return;

		String[] args = { path, name };
		GUIUtilities.error(null,"xmode-invalid-doctype",args);
	}

	public void charData(char[] c, int off, int len)
	{
		String tag = peekElement();
		String text = new String(c, off, len);

		if (tag == "WHITESPACE" ||
			tag == "EOL_SPAN" ||
			tag == "MARK_PREVIOUS" ||
			tag == "MARK_FOLLOWING" ||
			tag == "SEQ" ||
			tag == "BEGIN"
		)
		{
			lastStart = text;
		}
		else if (tag == "END")
		{
			lastEnd = text;
		}
		else
		{
			lastKeyword = text;
		}
	}

	public void startElement (String tag)
	{
		tag = pushElement(tag);

		if (tag == "MODE")
		{
			mode = jEdit.getMode(modeName);
			if (mode == null)
			{
				mode = new Mode(modeName);
				jEdit.addMode(mode);
			}
			mode.setProperty("grammar",path);
		}
		else if (tag == "KEYWORDS")
		{
			keywords = new KeywordMap(true);
		}
		else if (tag == "RULES")
		{
			rules = new ParserRuleSet();
			rules.setIgnoreCase(lastIgnoreCase);
			rules.setHighlightDigits(lastHighlightDigits);
			rules.setEscape(lastEscape);
			rules.setDefault(lastDefaultID);
		}
	}

	public void endElement (String name)
	{
		if (name == null) return;

		String tag = peekElement();

		if (name.equalsIgnoreCase(tag))
		{
			if (tag == "MODE")
			{
				mode.init();
				mode.setTokenMarker(marker);
			}
			else if (tag == "PROPERTY")
			{
				try
				{
					mode.setProperty(propName,
						new Integer(propValue));
				}
				catch(NumberFormatException nf)
				{
					mode.setProperty(propName,propValue);
				}
			}
			else if (tag == "KEYWORDS")
			{
				keywords.setIgnoreCase(lastIgnoreCase);
				lastIgnoreCase = true;
			}
			else if (tag == "RULES")
			{
				marker.addRuleSet(lastSetName, rules);
				rules.setKeywords(keywords);
				keywords = null;
				lastSetName = null;
				lastEscape = null;
				lastIgnoreCase = true;
				lastHighlightDigits = false;
				lastDefaultID = Token.NULL;
				rules = null;
			}
			else if (tag == "TERMINATE")
			{
				setTerminateChar(termChar);
				termChar = -1;
			}
			else if (tag == "WHITESPACE")
			{
				addRule(ParserRuleFactory.createWhitespaceRule(
					lastStart));
				lastStart = null;
				lastEnd = null;
			}
			else if (tag == "EOL_SPAN")
			{
				addRule(ParserRuleFactory.createEOLSpanRule(
					lastStart,lastTokenID,lastAtLineStart,
					lastExcludeMatch));
				lastStart = null;
				lastEnd = null;
				lastTokenID = Token.NULL;
				lastAtLineStart = false;
				lastExcludeMatch = false;
			}
			else if (tag == "MARK_PREVIOUS")
			{
				addRule(ParserRuleFactory
					.createMarkPreviousRule(lastStart,
					lastTokenID,lastAtLineStart,
					lastExcludeMatch));
				lastStart = null;
				lastEnd = null;
				lastTokenID = Token.NULL;
				lastAtLineStart = false;
				lastExcludeMatch = false;
			}
			else if (tag == "MARK_FOLLOWING")
			{
				addRule(ParserRuleFactory
					.createMarkFollowingRule(lastStart,
					lastTokenID,lastAtLineStart,
					lastExcludeMatch));
				lastStart = null;
				lastEnd = null;
				lastTokenID = Token.NULL;
				lastAtLineStart = false;
				lastExcludeMatch = false;
			}
			else if (tag == "SEQ")
			{
				addRule(ParserRuleFactory.createSequenceRule(
					lastStart,lastTokenID,lastAtLineStart));
				lastStart = null;
				lastEnd = null;
				lastTokenID = Token.NULL;
				lastAtLineStart = false;
			}
			else if (tag == "END")
			{
				if (lastDelegateSet == null)
				{
					addRule(ParserRuleFactory
						.createSpanRule(lastStart,
						lastEnd,lastTokenID,
						lastNoLineBreak,
						lastAtLineStart,
						lastExcludeMatch,
						lastNoWordBreak));
				}
				else
				{
					if (lastDelegateSet.indexOf("::") == -1)
					{
						lastDelegateSet = modeName + "::" + lastDelegateSet;
					}

					addRule(ParserRuleFactory
						.createDelegateSpanRule(
						lastStart,lastEnd,
						lastDelegateSet,
						lastTokenID,lastNoLineBreak,
						lastAtLineStart,
						lastExcludeMatch,
						lastNoWordBreak));
				}
				lastStart = null;
				lastEnd = null;
				lastTokenID = Token.NULL;
				lastAtLineStart = false;
				lastNoLineBreak = false;
				lastExcludeMatch = false;
				lastNoWordBreak = false;
				lastDelegateSet = null;
			}
			else if (tag == "NULL")
			{
				addKeyword(lastKeyword,Token.NULL);
			}
			else if (tag == "COMMENT1")
			{
				addKeyword(lastKeyword,Token.COMMENT1);
			}
			else if (tag == "COMMENT2")
			{
				addKeyword(lastKeyword,Token.COMMENT2);
			}
			else if (tag == "LITERAL1")
			{
				addKeyword(lastKeyword,Token.LITERAL1);
			}
			else if (tag == "LITERAL2")
			{
				addKeyword(lastKeyword,Token.LITERAL2);
			}
			else if (tag == "LABEL")
			{
				addKeyword(lastKeyword,Token.LABEL);
			}
			else if (tag == "KEYWORD1")
			{
				addKeyword(lastKeyword,Token.KEYWORD1);
			}
			else if (tag == "KEYWORD2")
			{
				addKeyword(lastKeyword,Token.KEYWORD2);
			}
			else if (tag == "KEYWORD3")
			{
				addKeyword(lastKeyword,Token.KEYWORD3);
			}
			else if (tag == "FUNCTION")
			{
				addKeyword(lastKeyword,Token.FUNCTION);
			}
			else if (tag == "MARKUP")
			{
				addKeyword(lastKeyword,Token.MARKUP);
			}
			else if (tag == "OPERATOR")
			{
				addKeyword(lastKeyword,Token.OPERATOR);
			}
			else if (tag == "DIGIT")
			{
				addKeyword(lastKeyword,Token.DIGIT);
			}

			popElement();
		}
		else
		{
			// can't happen
			throw new InternalError();
		}
	}

	public void startDocument()
	{
		marker = new TokenMarker();

		try
		{
			pushElement(null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	// end HandlerBase implementation

	// private members
	private String path;

	private TokenMarker marker;
	private KeywordMap keywords;
	private Mode mode;
	private Stack stateStack;
	private String modeName;
	private String propName;
	private String propValue;
	private String lastStart;
	private String lastEnd;
	private String lastKeyword;
	private String lastSetName;
	private String lastEscape;
	private String lastDelegateSet;
	private ParserRuleSet rules;
	private byte lastDefaultID = Token.NULL;
	private byte lastTokenID;
	private int termChar = -1;
	private boolean lastNoLineBreak;
	private boolean lastNoWordBreak;
	private boolean lastAtLineStart;
	private boolean lastExcludeMatch;
	private boolean lastIgnoreCase = true;
	private boolean lastHighlightDigits;

	private byte stringToToken(String value)
	{
		if (value == "NULL")
		{
			return Token.NULL;
		}
		else if (value == "COMMENT1")
		{
			return Token.COMMENT1;
		}
		else if (value == "COMMENT2")
		{
			return Token.COMMENT2;
		}
		else if (value == "LITERAL1")
		{
			return Token.LITERAL1;
		}
		else if (value == "LITERAL2")
		{
			return Token.LITERAL2;
		}
		else if (value == "LABEL")
		{
			return Token.LABEL;
		}
		else if (value == "KEYWORD1")
		{
			return Token.KEYWORD1;
		}
		else if (value == "KEYWORD2")
		{
			return Token.KEYWORD2;
		}
		else if (value == "KEYWORD3")
		{
			return Token.KEYWORD3;
		}
		else if (value == "FUNCTION")
		{
			return Token.FUNCTION;
		}
		else if (value == "MARKUP")
		{
			return Token.MARKUP;
		}
		else if (value == "OPERATOR")
		{
			return Token.OPERATOR;
		}
		else if (value == "DIGIT")
		{
			return Token.DIGIT;
		}
		else
		{
			// XXX invalid token id
			return Token.NULL;
		}
	}

	private void addKeyword(String k, byte id)
	{
		if (keywords == null) return;
		keywords.add(k,id);
	}

	private void addRule(ParserRule r)
	{
		if (rules == null) return;
		rules.addRule(r);
	}

	private void setTerminateChar(int atChar)
	{
		rules.setTerminateChar(atChar);
	}

	private String pushElement(String name)
	{
		if (stateStack == null) stateStack = new Stack();

		name = (name == null) ? null : name.intern();

		stateStack.push(name);

		return name;
	}

	private String peekElement()
	{
		if (stateStack == null) stateStack = new Stack();

		return (String) stateStack.peek();
	}

	private String popElement()
	{
		if (stateStack == null) stateStack = new Stack();

		return (String) stateStack.pop();
	}
}

/*
 * Change Log:
 * $Log: XModeHandler.java,v $
 * Revision 1.1  2000/10/30 07:14:04  sp
 * 2.7pre1 branched, GUI improvements
 *
 */
