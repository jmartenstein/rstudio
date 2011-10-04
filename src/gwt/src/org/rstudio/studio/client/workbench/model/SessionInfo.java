/*
 * SessionInfo.java
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
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

public class SessionInfo extends JavaScriptObject
{
   protected SessionInfo()
   {
   }
   
   public final native String getClientId() /*-{
      return this.clientId;
   }-*/;
   
   public final native double getClientVersion() /*-{
      return this.version;
   }-*/;

   public final native String getUserIdentity() /*-{
      return this.userIdentity;
   }-*/;

   public final native boolean isTexInstalled() /*-{
      return this.tex_installed;
   }-*/;

   public final native String getLogDir() /*-{
      return this.log_dir;
   }-*/;

   public final native String getScratchDir() /*-{
      return this.scratch_dir;
   }-*/;
   
   public final native String getTempDir() /*-{
      return this.temp_dir;
   }-*/;

   public final native JsObject getUiPrefs() /*-{
      if (!this.ui_prefs)
         this.ui_prefs = {};
      return this.ui_prefs;
   }-*/;

   public final static String DESKTOP_MODE = "desktop";
   public final static String SERVER_MODE = "server";
   
   public final native String getMode() /*-{
      return this.mode;
   }-*/;

   public final native boolean getResumed() /*-{
      return this.resumed;
   }-*/;
   
   public final native String getDefaultPrompt() /*-{
      return this.prompt;
   }-*/;
   
   public final native JsArrayString getConsoleHistory() /*-{
      return this.console_history;
   }-*/;
   
   public final native int getConsoleHistoryCapacity() /*-{
      return this.console_history_capacity;
   }-*/;

   public final native RpcObjectList<ConsoleAction> getConsoleActions() /*-{
      return this.console_actions;
   }-*/;

   public final native int getConsoleActionsLimit() /*-{
      return this.console_actions_limit;
   }-*/;

   public final native ClientInitState getClientState() /*-{
      return this.client_state;
   }-*/;
   
   public final native JsArray<SourceDocument> getSourceDocuments() /*-{
      return this.source_documents;
   }-*/;
   
   public final native boolean hasAgreement() /*-{
      return this.hasAgreement;
   }-*/;
   
   public final native Agreement pendingAgreement() /*-{
      return this.pendingAgreement;
   }-*/;
   
   public final native String docsURL() /*-{
      return this.docsURL;
   }-*/;

   public final native boolean isGoogleDocsIntegrationEnabled() /*-{
      return this.googleDocsIntegrationEnabled;
   }-*/;
   
   public final native String getRstudioVersion() /*-{
      return this.rstudio_version;
   }-*/;

   public final native String getSystemEncoding() /*-{
      return this.system_encoding;
   }-*/;

   public final boolean isVcsEnabled()
   {
      return !StringUtil.isNullOrEmpty(getVcsName());
   }

   public final String[] getAvailableVCS()
   {
      return this.<JsObject>cast().getString("vcs_available", true).split(",");
   }
   
   public final native String getVcsName() /*-{
      return this.vcs;
   }-*/;

   // TODO: The check for null was for migration in the presence of 
   // sessions that couldn't suspend (3/21/2011). Remove this check
   // once we are sufficiently clear of this date window.
   public final native String getInitialWorkingDir() /*-{
      if (!this.initial_working_dir)
         this.initial_working_dir = "~/";
      return this.initial_working_dir;
   }-*/;
    
   public final native String getActiveProjectFile() /*-{
      return this.active_project_file;
   }-*/;
   
   public final FileSystemItem getActiveProjectDir()
   {
      String projFile = getActiveProjectFile();
      if (projFile != null)
      {
         return FileSystemItem.createFile(projFile).getParentPath();
      }
      else
      {
         return null;
      }
   }
  
   public final native JsObject getProjectUIPrefs() /*-{
      if (!this.project_ui_prefs)
         this.project_ui_prefs = {};
      return this.project_ui_prefs;
   }-*/;   
}

