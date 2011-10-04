/*
 * FileTypeRegistry.java
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
package org.rstudio.studio.client.common.filetypes;

import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.reditor.EditorLanguage;

import java.util.HashMap;

@Singleton
public class FileTypeRegistry
{
   private static final FileIconResources ICONS = FileIconResources.INSTANCE;

   public static final TextFileType TEXT =
         new TextFileType("text", "Text File", EditorLanguage.LANG_PLAIN, "",
                          ICONS.iconText(),
                          true,
                          false, false, false, false, false);

   public static final TextFileType R =
         new RFileType("r_source", "R Script", EditorLanguage.LANG_R, ".R",
                       ICONS.iconRdoc(),
                       true, true, true, true, false);

   public static final TextFileType RD =
      new TextFileType("r_doc", "R Documentation", EditorLanguage.LANG_RDOC, ".Rd",
                       ICONS.iconRd(),
                       true,
                       false, false, false, false, false);

   public static final TextFileType SWEAVE =
         new TextFileType("sweave", "Sweave Document", EditorLanguage.LANG_SWEAVE, ".Rnw",
                          ICONS.iconRsweave(),
                          true,
                          false, true, true, false, true);

   public static final TextFileType TEX =
         new TextFileType("tex", "TeX Document", EditorLanguage.LANG_TEX, ".tex",
                          ICONS.iconTex(),
                          true,
                          false, false, false, false, true);
   
   public static final PlainTextFileType RHISTORY =
      new PlainTextFileType("r_history", "R History", ".Rhistory",
                            ICONS.iconRhistory(),
                            true);

   public static final RDataType RDATA = new RDataType();
   public static final RProjectType RPROJECT = new RProjectType();
   
   public static final DataFrameType DATAFRAME = new DataFrameType();
   public static final UrlContentType URLCONTENT = new UrlContentType();
   public static final CodeBrowserType CODEBROWSER = new CodeBrowserType();

   public static final BrowserType BROWSER = new BrowserType();
   
   @Inject
   public FileTypeRegistry(EventBus eventBus)
   {
      eventBus_ = eventBus;

      FileIconResources icons = ICONS;

      register("", TEXT, icons.iconText());
      register("*.txt", TEXT, icons.iconText());
      register("*.log", TEXT, icons.iconText());
      register("README", TEXT, icons.iconText());
      register(".gitignore", TEXT, icons.iconText());
      register("*.r", R, icons.iconRdoc());
      register(".rprofile", R, icons.iconRprofile());
      register("*.rhistory", RHISTORY, icons.iconRhistory());
      register("*.rproj", RPROJECT, icons.iconRproject());
      register("*.rnw", SWEAVE, icons.iconRsweave());
      register("*.snw", SWEAVE, icons.iconRsweave());
      register("*.nw", SWEAVE, icons.iconRsweave());
      register("*.tex", TEX, icons.iconTex());
      register("*.latex", TEX, icons.iconTex());
      register("*.sty", TEX, icons.iconTex());
      register("*.cls", TEX, icons.iconTex());
      register("*.bbl", TEX, icons.iconTex());
      register("*.bib", TEXT, icons.iconText());
      register("*.c", TEXT, icons.iconText());
      register("*.cpp", TEXT, icons.iconText());
      register("*.h", TEXT, icons.iconText());
      register("*.hpp", TEXT, icons.iconText());
      register("*.Rout.save", TEXT, icons.iconText());
      register("*.rd", RD, icons.iconRd());
      register("*.rdata", RDATA, icons.iconRdata());
      register("*.rda", RDATA, icons.iconRdata());
      register("*.Rproj", RPROJECT, icons.iconRproject());
      defaultType_ = BROWSER;

      registerIcon(".jpg", icons.iconPng());
      registerIcon(".jpeg", icons.iconPng());
      registerIcon(".gif", icons.iconPng());
      registerIcon(".bmp", icons.iconPng());
      registerIcon(".tiff", icons.iconPng());
      registerIcon(".tif", icons.iconPng());
      registerIcon(".png", icons.iconPng());

      registerIcon(".pdf", icons.iconPdf());
      registerIcon(".csv", icons.iconCsv());

      for (FileType fileType : FileType.ALL_FILE_TYPES)
      {
         assert !fileTypesByTypeName_.containsKey(fileType.getTypeId());
         fileTypesByTypeName_.put(fileType.getTypeId(), fileType);
      }
   }

   public void openFile(FileSystemItem file)
   {
      openFile(file, true);
   }
   
   public boolean openFile(FileSystemItem file, boolean canUseDefault)
   {
      FileType fileType = getTypeForFile(file, canUseDefault);
      if (fileType != null)
      {
         fileType.openFile(file, eventBus_);
         return true;
      }
      else
      {
         return false;
      }
   }

   public void editFile(FileSystemItem file)
   {
      editFile(file, null);
   }
   
   public void editFile(FileSystemItem file, FilePosition position)
   {
      FileType fileType = getTypeForFile(file);
      if (!(fileType instanceof TextFileType))
         fileType = TEXT;

      if (fileType != null)
         fileType.openFile(file, position, eventBus_);
   }

   public FileType getTypeByTypeName(String name)
   {
      return fileTypesByTypeName_.get(name);
   }

   public FileType getTypeForFile(FileSystemItem file)
   {
      return getTypeForFile(file, true);
   }
   
   public FileType getTypeForFile(FileSystemItem file, boolean canUseDefault)
   {
      if (file != null)
      {
         String filename = file.getName().toLowerCase();
         FileType result = fileTypesByFilename_.get(filename);
         if (result != null)
            return result;

         String extension = FileSystemItem.getExtensionFromPath(filename);
         result = fileTypesByFileExtension_.get(extension);
         if (result != null)
            return result;
         
         // last ditch -- see if this either a known text file type
         // or (for server mode) NOT a known binary type. the result of
         // this is that unknown files types are treated as text and
         // opened in the editor (we don't do this on desktop because
         // in that case users have the recourse of using a local editor)
         String defaultType = Desktop.isDesktop() ? "application/octet-stream" :
                                                    "text/plain";
         String mimeType = file.mimeType(defaultType);
         if (mimeType.startsWith("text/"))
            return TEXT;
      }
      
      if (canUseDefault)
         return defaultType_;
      else
         return null;
   }

   public TextFileType getTextTypeForFile(FileSystemItem file)
   {
      FileType type = getTypeForFile(file);
      if (type instanceof TextFileType)
         return (TextFileType) type;
      else
         return TEXT;
   }
   
   public ImageResource getIconForFile(FileSystemItem file)
   {
      if (file.isDirectory())
      {
         if (file.isPublicFolder())
            return ICONS.iconPublicFolder();
         else
            return ICONS.iconFolder();
      }

      return getIconForFilename(file.getName());
   }
   
   public ImageResource getIconForFilename(String filename)
   {
      ImageResource icon = iconsByFilename_.get(filename.toLowerCase());
      if (icon != null)
         return icon;
      String ext = FileSystemItem.getExtensionFromPath(filename);
      icon = iconsByFileExtension_.get(ext.toLowerCase());
      if (icon != null)
         return icon;

      return ICONS.iconText();
   }

   private void register(String filespec, FileType fileType, ImageResource icon)
   {
      if (filespec.startsWith("*."))
      {
         String ext = filespec.substring(1).toLowerCase();
         if (ext.equals("."))
            ext = "";
         fileTypesByFileExtension_.put(ext,
                                       fileType);
         if (icon != null)
            iconsByFileExtension_.put(ext, icon);
      }
      else if (filespec.length() == 0)
      {
         fileTypesByFileExtension_.put("", fileType);
         if (icon != null)
            iconsByFileExtension_.put("", icon);
      }
      else
      {
         assert filespec.indexOf("*") < 0 : "Unexpected filespec format";
         fileTypesByFilename_.put(filespec.toLowerCase(), fileType);
         if (icon != null)
            iconsByFileExtension_.put(filespec.toLowerCase(), icon);
      }
   }

   private void registerIcon(String extension, ImageResource icon)
   {
      iconsByFileExtension_.put(extension, icon);
   }

   private final HashMap<String, FileType> fileTypesByFileExtension_ =
         new HashMap<String, FileType>();
   private final HashMap<String, FileType> fileTypesByFilename_ =
         new HashMap<String, FileType>();
   private final HashMap<String, FileType> fileTypesByTypeName_ =
         new HashMap<String, FileType>();
   private final HashMap<String, ImageResource> iconsByFileExtension_ =
         new HashMap<String, ImageResource>();
   private final HashMap<String, ImageResource> iconsByFilename_ =
         new HashMap<String, ImageResource>();
   private final FileType defaultType_;
   private final EventBus eventBus_;
}
