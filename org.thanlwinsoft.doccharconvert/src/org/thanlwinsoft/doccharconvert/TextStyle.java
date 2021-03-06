/*
Copyright (C) 2005-2007 Keith Stribley http://www.thanlwinsoft.org/

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
------------------------------------------------------------------------------*/

package org.thanlwinsoft.doccharconvert;

import org.thanlwinsoft.doccharconvert.opendoc.ScriptType;
/**
 *
 * @author  keith
 */
public interface TextStyle
{
    /**
     * @return description of style
     */
    public String getDescription();
    /**
     * @return font name
     */
    public String getFontName();
    /**
     * @param aFontName
     */
    public void setFontName(String aFontName);
    /**
     * @return style's name
     */
    public String getStyleName();
    /**
     * @param newName
     */
    public void setStyleName(String newName);
    public boolean equals(Object obj);
    public int hashCode();
    /**
     * @return type of script
     */
    public ScriptType.Type getScriptType();
    /**
     * @param type
     */
    public void setScriptType(ScriptType.Type type);
}
