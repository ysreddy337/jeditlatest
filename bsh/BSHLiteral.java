/*****************************************************************************
 *                                                                           *
 *  This file is part of the BeanShell Java Scripting distribution.          *
 *  Documentation and updates may be found at http://www.beanshell.org/      *
 *                                                                           *
 *  BeanShell is distributed under the terms of the LGPL:                    *
 *  GNU Library Public License http://www.gnu.org/copyleft/lgpl.html         *
 *                                                                           *
 *  Patrick Niemeyer (pat@pat.net)                                           *
 *  Author of Exploring Java, O'Reilly & Associates                          *
 *  http://www.pat.net/~pat/                                                 *
 *                                                                           *
 *****************************************************************************/


package bsh;

class BSHLiteral extends SimpleNode
{
    public Object value;

    BSHLiteral(int id) { super(id); }

    public Object eval(NameSpace namespace, Interpreter interpreter)  throws EvalError
    {
        return value;
    }

    private char getEscapeChar(char ch)
    {
        switch(ch)
        {
            case 'b':
                ch = '\b';
                break;

            case 't':
                ch = '\t';
                break;

            case 'n':
                ch = '\n';
                break;

            case 'f':
                ch = '\f';
                break;

            case 'r':
                ch = '\r';
                break;

            // do nothing - ch already contains correct character
            case '"':
            case '\'':
            case '\\':
                break;
        }

        return ch;
    }

    public void charSetup(String str)
    {
        char ch = str.charAt(0);
        if(ch == '\\')
        {
            // get next character
            ch = str.charAt(1);

            if(Character.isDigit(ch))
                ch = (char)Integer.parseInt(str.substring(1), 8);
            else
                ch = getEscapeChar(ch);
        }

        value = new Primitive(new Character(ch));
    }

    /* process string escapes - JLS 3.10.6 */
    public void stringSetup(String str)
    {
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < str.length(); i++)
        {
            char ch = str.charAt(i);
            if(ch == '\\')
            {
                // get next character
                ch = str.charAt(++i);

                if(Character.isDigit(ch))
                {
                    int endPos = i;

                    // check the next two characters
                    while(endPos < i + 2)
                    {
                        if(Character.isDigit(str.charAt(endPos + 1)))
                            endPos++;
                        else
                            break;
                    }

                    ch = (char)Integer.parseInt(str.substring(i, endPos + 1), 8);
                    i = endPos;
                }
                else
                    ch = getEscapeChar(ch);
            }

            buffer.append(ch);
        }

        value = buffer.toString();
    }
}
