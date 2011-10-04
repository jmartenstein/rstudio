/*
 * RFileItem.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.codesearch.model;

import com.google.gwt.core.client.JavaScriptObject;


public class RFileItem extends JavaScriptObject
{
   protected RFileItem()
   {
   }
   
  
   public final native String getFilename() /*-{
      return this.filename;
   }-*/;
   
  
   public final native String getPath() /*-{
      return this.path;
   }-*/;
   
}
