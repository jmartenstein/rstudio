/*
 * RStudioGinModule.java
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
package org.rstudio.studio.client;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.ApplicationView;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.application.ui.ApplicationWindow;
import org.rstudio.studio.client.common.ConsoleDispatcher;
import org.rstudio.studio.client.common.DefaultGlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;
import org.rstudio.studio.client.common.mirrors.DefaultCRANMirror;
import org.rstudio.studio.client.common.mirrors.model.MirrorsServerOperations;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.projects.Projects;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.server.remote.RemoteServer;
import org.rstudio.studio.client.workbench.ClientStateUpdater;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.WorkbenchMainView;
import org.rstudio.studio.client.workbench.codesearch.CodeSearch;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchServerOperations;
import org.rstudio.studio.client.workbench.codesearch.ui.CodeSearchWidget;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.PrefsServerOperations;
import org.rstudio.studio.client.workbench.ui.WorkbenchScreen;
import org.rstudio.studio.client.workbench.ui.WorkbenchTab;
import org.rstudio.studio.client.workbench.views.choosefile.model.ChooseFileServerOperations;
import org.rstudio.studio.client.workbench.views.console.ConsolePane;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.console.shell.Shell;
import org.rstudio.studio.client.workbench.views.console.shell.ShellPane;
import org.rstudio.studio.client.workbench.views.data.Data;
import org.rstudio.studio.client.workbench.views.data.DataPane;
import org.rstudio.studio.client.workbench.views.data.DataTab;
import org.rstudio.studio.client.workbench.views.data.model.DataServerOperations;
import org.rstudio.studio.client.workbench.views.edit.Edit;
import org.rstudio.studio.client.workbench.views.edit.model.EditServerOperations;
import org.rstudio.studio.client.workbench.views.edit.ui.EditView;
import org.rstudio.studio.client.workbench.views.files.Files;
import org.rstudio.studio.client.workbench.views.files.FilesPane;
import org.rstudio.studio.client.workbench.views.files.FilesTab;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.help.Help;
import org.rstudio.studio.client.workbench.views.help.HelpPane;
import org.rstudio.studio.client.workbench.views.help.HelpTab;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;
import org.rstudio.studio.client.workbench.views.help.search.HelpSearch;
import org.rstudio.studio.client.workbench.views.help.search.HelpSearchWidget;
import org.rstudio.studio.client.workbench.views.history.History;
import org.rstudio.studio.client.workbench.views.history.HistoryTab;
import org.rstudio.studio.client.workbench.views.history.model.HistoryServerOperations;
import org.rstudio.studio.client.workbench.views.history.view.HistoryPane;
import org.rstudio.studio.client.workbench.views.packages.Packages;
import org.rstudio.studio.client.workbench.views.packages.PackagesPane;
import org.rstudio.studio.client.workbench.views.packages.PackagesTab;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;
import org.rstudio.studio.client.workbench.views.plots.Plots;
import org.rstudio.studio.client.workbench.views.plots.PlotsPane;
import org.rstudio.studio.client.workbench.views.plots.PlotsTab;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.source.Source;
import org.rstudio.studio.client.workbench.views.source.SourcePane;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetSource;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;
import org.rstudio.studio.client.workbench.views.vcs.*;
import org.rstudio.studio.client.workbench.views.vcs.console.ConsoleBarPresenter;
import org.rstudio.studio.client.workbench.views.vcs.console.ConsoleBarView;
import org.rstudio.studio.client.workbench.views.vcs.console.ConsoleOutputPane;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTablePresenter;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTableView;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPanel;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPanel;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenter;
import org.rstudio.studio.client.workbench.views.workspace.Workspace;
import org.rstudio.studio.client.workbench.views.workspace.WorkspacePane;
import org.rstudio.studio.client.workbench.views.workspace.WorkspaceTab;
import org.rstudio.studio.client.workbench.views.workspace.model.WorkspaceServerOperations;

public class RStudioGinModule extends AbstractGinModule
{
   @Override
   protected void configure()
   {
      bind(EventBus.class).in(Singleton.class) ;
      bind(Session.class).in(Singleton.class) ;
      bind(Projects.class).in(Singleton.class);
      bind(WorkbenchContext.class).asEagerSingleton();
      bind(ApplicationQuit.class).asEagerSingleton();
      bind(ClientStateUpdater.class).asEagerSingleton();
      bind(Commands.class).in(Singleton.class);
      bind(DefaultCRANMirror.class).in(Singleton.class);
      bind(ConsoleDispatcher.class).in(Singleton.class);
      bind(FileTypeCommands.class).in(Singleton.class);
      

      bind(ApplicationView.class).to(ApplicationWindow.class)
            .in(Singleton.class) ;
      
      bind(Server.class).to(RemoteServer.class) ;
      bind(WorkbenchServerOperations.class).to(RemoteServer.class) ;

      bind(EditingTargetSource.class).to(EditingTargetSource.Impl.class);

      // Bind workbench views
      bindPane("Console", ConsolePane.class); // eager loaded
      bind(Source.Display.class).to(SourcePane.class);
      bind(History.Display.class).to(HistoryPane.class);
      bind(Workspace.Display.class).to(WorkspacePane.class);
      bind(Data.Display.class).to(DataPane.class);
      bind(Files.Display.class).to(FilesPane.class);
      bind(Plots.Display.class).to(PlotsPane.class);
      bind(Packages.Display.class).to(PackagesPane.class);
      bind(Help.Display.class).to(HelpPane.class);
      bind(Edit.Display.class).to(EditView.class);
      bind(VCS.Display.class).to(VCSPane.class);
      bindTab("Workspace", WorkspaceTab.class);
      bindTab("History", HistoryTab.class);
      bindTab("Data", DataTab.class);
      bindTab("Files", FilesTab.class);
      bindTab("Plots", PlotsTab.class);
      bindTab("Packages", PackagesTab.class);
      bindTab("Help", HelpTab.class);
      bindTab("VCS", VCSTab.class);

      bind(Shell.Display.class).to(ShellPane.class) ;
      
      bind(HelpSearch.Display.class).to(HelpSearchWidget.class) ;
      bind(CodeSearch.Display.class).to(CodeSearchWidget.class);

      bind(ReviewPresenter.Display.class).to(ReviewPanel.class);
      bind(LineTablePresenter.Display.class).to(LineTableView.class);
      bind(HistoryPresenter.Display.class).to(HistoryPanel.class);
      bind(ConsoleBarPresenter.Display.class).to(ConsoleBarView.class);
      bind(ConsoleBarPresenter.OutputDisplay.class).to(ConsoleOutputPane.class);

      bind(GlobalDisplay.class)
            .to(DefaultGlobalDisplay.class)
            .in(Singleton.class) ;

      bind(ApplicationServerOperations.class).to(RemoteServer.class) ;
      bind(ChooseFileServerOperations.class).to(RemoteServer.class) ;
      bind(CodeToolsServerOperations.class).to(RemoteServer.class) ;
      bind(ConsoleServerOperations.class).to(RemoteServer.class) ;
      bind(SourceServerOperations.class).to(RemoteServer.class) ;
      bind(DataServerOperations.class).to(RemoteServer.class);
      bind(FilesServerOperations.class).to(RemoteServer.class) ;
      bind(HistoryServerOperations.class).to(RemoteServer.class) ;
      bind(WorkspaceServerOperations.class).to(RemoteServer.class) ;
      bind(PlotsServerOperations.class).to(RemoteServer.class) ;
      bind(PackagesServerOperations.class).to(RemoteServer.class) ;
      bind(HelpServerOperations.class).to(RemoteServer.class) ;
      bind(EditServerOperations.class).to(RemoteServer.class) ;
      bind(MirrorsServerOperations.class).to(RemoteServer.class);
      bind(VCSServerOperations.class).to(RemoteServer.class);
      bind(PrefsServerOperations.class).to(RemoteServer.class);
      bind(ProjectsServerOperations.class).to(RemoteServer.class);
      bind(CodeSearchServerOperations.class).to(RemoteServer.class);


      bind(WorkbenchMainView.class).to(WorkbenchScreen.class) ;

      bind(DocDisplay.class).to(AceEditor.class);
   }

   private <T extends WorkbenchTab> void bindTab(String name, Class<T> clazz)
   {
      bind(WorkbenchTab.class).annotatedWith(Names.named(name)).to(clazz);
   }

   private <T extends Widget> void bindPane(String name, Class<T> clazz)
   {
      bind(Widget.class).annotatedWith(Names.named(name)).to(clazz);
   }
}
