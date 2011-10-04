/*
 * DiffFileHeader.java
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
package org.rstudio.studio.client.workbench.views.vcs.diff;

import java.util.ArrayList;

public class DiffFileHeader
{
   public DiffFileHeader(ArrayList<String> headerLines,
                         String oldFile,
                         String newFile)
   {
      headerLines_ = headerLines;
      oldFile_ = oldFile.replaceFirst("^a/", "");
      newFile_ = newFile.replaceFirst("^b/", "");
   }

   public String getDescription()
   {
      if (oldFile_.equals(newFile_))
         return oldFile_;

      if (oldFile_.equals("/dev/null"))
         return newFile_;
      if (newFile_.equals("/dev/null"))
         return oldFile_;
      return oldFile_ + " => " + newFile_;
   }

   @SuppressWarnings("unused")
   private final ArrayList<String> headerLines_;
   private final String oldFile_;
   private final String newFile_;
}
