/*
 * Commands.java
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
package org.rstudio.studio.client.workbench.commands;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBundle;
import org.rstudio.core.client.command.MenuCallback;

public abstract class
      Commands extends CommandBundle
{
   public abstract void mainMenu(MenuCallback callback);

   // Workbench
   public abstract AppCommand setWorkingDir();
   
   // Source
   public abstract AppCommand newSourceDoc();
   public abstract AppCommand openSourceDoc();
   public abstract AppCommand reopenSourceDocWithEncoding();
   public abstract AppCommand saveSourceDoc();
   public abstract AppCommand saveSourceDocAs();
   public abstract AppCommand saveSourceDocWithEncoding();
   public abstract AppCommand saveAllSourceDocs();
   public abstract AppCommand closeSourceDoc();
   public abstract AppCommand closeAllSourceDocs();
   public abstract AppCommand executeAllCode();
   public abstract AppCommand sourceFile();
   public abstract AppCommand sourceActiveDocument();
   public abstract AppCommand sourceActiveDocumentWithEcho();
   public abstract AppCommand complete();
   public abstract AppCommand executeCode();
   public abstract AppCommand executeToCurrentLine();
   public abstract AppCommand executeFromCurrentLine();
   public abstract AppCommand executeCurrentFunction();
   public abstract AppCommand executeLastCode();
   public abstract AppCommand goToFunctionDefinition();
   public abstract AppCommand sourceNavigateBack();
   public abstract AppCommand sourceNavigateForward();
   public abstract AppCommand compilePDF();
   public abstract AppCommand publishPDF();
   public abstract AppCommand activateSource();
   public abstract AppCommand printSourceDoc();
   public abstract AppCommand popoutDoc();
   public abstract AppCommand findReplace();
   public abstract AppCommand extractFunction();
   public abstract AppCommand commentUncomment();
   public abstract AppCommand reindent();
   public abstract AppCommand setWorkingDirToActiveDoc();
   
   // Projects
   public abstract AppCommand newProject();
   public abstract AppCommand openProject();
   public abstract AppCommand projectMru0();
   public abstract AppCommand projectMru1();
   public abstract AppCommand projectMru2();
   public abstract AppCommand projectMru3();
   public abstract AppCommand projectMru4();
   public abstract AppCommand projectMru5();
   public abstract AppCommand projectMru6();
   public abstract AppCommand projectMru7();
   public abstract AppCommand projectMru8();
   public abstract AppCommand projectMru9();
   public abstract AppCommand clearRecentProjects();
   public abstract AppCommand closeProject();
   public abstract AppCommand projectOptions();

   // Console
   public abstract AppCommand consoleClear();
   public abstract AppCommand interruptR();
   public abstract AppCommand activateConsole();

   // Files
   public abstract AppCommand newFolder();
   public abstract AppCommand uploadFile();
   public abstract AppCommand copyFile();
   public abstract AppCommand moveFiles();
   public abstract AppCommand exportFiles();
   public abstract AppCommand renameFile();
   public abstract AppCommand deleteFiles();
   public abstract AppCommand refreshFiles();
   public abstract AppCommand activateFiles();
   public abstract AppCommand goToWorkingDir();
   public abstract AppCommand setAsWorkingDir();
   public abstract AppCommand setWorkingDirToFilesPane();
   public abstract AppCommand showFolder();
   public abstract AppCommand vcsRevertFiles();
   public abstract AppCommand vcsAddFiles();
   public abstract AppCommand vcsRemoveFiles();

   // VCS
   public abstract AppCommand vcsDiff();
   public abstract AppCommand vcsCommit();
   public abstract AppCommand vcsStage();
   public abstract AppCommand vcsUnstage();
   public abstract AppCommand vcsRevert();
   public abstract AppCommand vcsShowHistory();
   public abstract AppCommand vcsRefresh();
   public abstract AppCommand vcsIgnore();
   public abstract AppCommand vcsPull();
   public abstract AppCommand vcsPush();

   // View
   public abstract AppCommand showToolbar();
   public abstract AppCommand hideToolbar();
   public abstract AppCommand jumpToFunction();
   public abstract AppCommand goToFileFunction();
   public abstract AppCommand switchToTab();
   public abstract AppCommand previousTab();
   public abstract AppCommand nextTab();
   public abstract AppCommand firstTab();
   public abstract AppCommand lastTab();
   public abstract AppCommand goToLine();

   // History
   public abstract AppCommand historySendToSource();
   public abstract AppCommand historySendToConsole();
   public abstract AppCommand searchHistory();
   public abstract AppCommand loadHistory();
   public abstract AppCommand saveHistory();
   public abstract AppCommand historyRemoveEntries();
   public abstract AppCommand clearHistory();
   public abstract AppCommand historyDismissResults();
   public abstract AppCommand historyShowContext();
   public abstract AppCommand historyDismissContext();
   public abstract AppCommand activateHistory();

   // Workspace
   public abstract AppCommand clearWorkspace();
   public abstract AppCommand refreshWorkspace();
   public abstract AppCommand saveWorkspace();
   public abstract AppCommand loadWorkspace();
   public abstract AppCommand saveDefaultWorkspace();
   public abstract AppCommand loadDefaultWorkspace();
   public abstract AppCommand importDatasetFromFile();
   public abstract AppCommand importDatasetFromURL();
   public abstract AppCommand importDatasetFromGoogleSpreadsheet();
   public abstract AppCommand activateWorkspace();
  
   // Plots
   public abstract AppCommand nextPlot();
   public abstract AppCommand previousPlot();
   public abstract AppCommand savePlotAsImage();
   public abstract AppCommand savePlotAsPdf();
   public abstract AppCommand copyPlotToClipboard();
   public abstract AppCommand zoomPlot();
   public abstract AppCommand removePlot();
   public abstract AppCommand clearPlots();
   public abstract AppCommand refreshPlot();
   public abstract AppCommand activatePlots();
   public abstract AppCommand showManipulator();

   // Packages
   public abstract AppCommand installPackage();
   public abstract AppCommand updatePackages();
   public abstract AppCommand activatePackages();

   // Tools
   public abstract AppCommand showOptions();

   // Help
   public abstract AppCommand helpBack();
   public abstract AppCommand helpForward();
   public abstract AppCommand helpHome();
   public abstract AppCommand printHelp();
   public abstract AppCommand addToHelpFavorites();
   public abstract AppCommand clearHelpHistory();
   public abstract AppCommand helpPopout();
   public abstract AppCommand refreshHelp();
   public abstract AppCommand raiseException();
   public abstract AppCommand raiseException2();
   public abstract AppCommand activateHelp();
   public abstract AppCommand showAboutDialog();
   public abstract AppCommand checkForUpdates();
   public abstract AppCommand helpUsingRStudio();
   public abstract AppCommand helpKeyboardShortcuts();
   public abstract AppCommand showRequestLog();
   public abstract AppCommand logFocusedElement();

   // Application
   public abstract AppCommand quitSession();
   public abstract AppCommand updateCredentials();
   public abstract AppCommand showLogFiles();
   public abstract AppCommand rstudioSupport();
   public abstract AppCommand rstudioAgreement();
   public abstract AppCommand rstudioLicense();

   public abstract AppCommand showWarningBar();

   // Clipboard placeholders
   public abstract AppCommand undoDummy();
   public abstract AppCommand redoDummy();
   public abstract AppCommand cutDummy();
   public abstract AppCommand copyDummy();
   public abstract AppCommand pasteDummy();

   public abstract AppCommand mru0();
   public abstract AppCommand mru1();
   public abstract AppCommand mru2();
   public abstract AppCommand mru3();
   public abstract AppCommand mru4();
   public abstract AppCommand mru5();
   public abstract AppCommand mru6();
   public abstract AppCommand mru7();
   public abstract AppCommand mru8();
   public abstract AppCommand mru9();
   public abstract AppCommand clearRecentFiles();
}
