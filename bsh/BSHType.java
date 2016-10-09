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

import java.lang.reflect.Array;

class BSHType extends SimpleNode
{
    private int arrayDims;
    private Class type;

    BSHType(int id) { super(id); }

    public void addArrayDimension() { arrayDims++; }

    // Returns a class for the type
    public Class getType(NameSpace namespace) throws EvalError
    {
        // return cached type if available
        if(type != null)
            return type;

        //  first node will either be PrimitiveType or AmbiguousName
        SimpleNode node = (SimpleNode)jjtGetChild(0);

        Class baseType;
        if(node instanceof BSHPrimitiveType)
            baseType = ((BSHPrimitiveType)node).getType();
        else 
            baseType = ((BSHAmbiguousName)node).toClass(namespace);

        if(arrayDims > 0)
        {
            try
            {
                // construct array which has zero length in all dimensions
                // (faster than constructing the name by hand - see below)
                int[] dims = new int[arrayDims];
                Object obj = Array.newInstance(baseType, dims);
                type = obj.getClass();
            }
            catch(Exception e)
            {
                throw new EvalError("Couldn't construct array type", this);
            }
        }
        else
            type = baseType;

        return type;
    }
}
