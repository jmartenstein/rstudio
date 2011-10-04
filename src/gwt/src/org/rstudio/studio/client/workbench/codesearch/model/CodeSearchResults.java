/*
 * CodeSearchResults.java
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

import org.rstudio.core.client.jsonrpc.RpcObjectList;

import com.google.gwt.core.client.JavaScriptObject;

public class CodeSearchResults extends JavaScriptObject
{
   protected CodeSearchResults()
   {
      
   }

   public final native RpcObjectList<RFileItem> getRFileItems() /*-{
      return this.file_items;
   }-*/;
   
   public final native RpcObjectList<RSourceItem> getRSourceItems() /*-{
      return this.source_items;
   }-*/;

   public final native boolean getMoreAvailable() /*-{
      return this.more_available;
   }-*/;

}
