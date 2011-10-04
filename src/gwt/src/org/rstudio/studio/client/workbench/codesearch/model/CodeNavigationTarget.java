/*
 * CodeNavigationTarget.java
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

import org.rstudio.core.client.FilePosition;

public class CodeNavigationTarget
{
   public static CodeNavigationTarget fromRSourceItem(RSourceItem srcItem)
   {
      return new CodeNavigationTarget(srcItem.getContext(),
                                      FilePosition.create(srcItem.getLine(), 
                                                          srcItem.getColumn()));
   }
   
   public CodeNavigationTarget(String file)
   {
      this(file, null);
   }
   
   public CodeNavigationTarget(String file, FilePosition pos)
   {
      file_ = file;
      pos_ = pos;
   }
   
   public String getFile()
   {
      return file_;
   }
   
   public FilePosition getPosition()
   {
      return pos_;
   }
   
   private final String file_;
   private final FilePosition pos_;
}
