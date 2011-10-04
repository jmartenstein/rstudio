/*
 * Files.java
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
package org.rstudio.studio.client.workbench.views.files;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.cellview.ColumnSortInfo;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.ConsoleDispatcher;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.events.OpenFileInBrowserEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenFileInBrowserHandler;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientInitState;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.model.helper.StringStateValue;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.files.events.*;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.files.model.PendingFileUpload;

import java.util.ArrayList;

public class Files
      extends BasePresenter
      implements FileChangeHandler, 
                 OpenFileInBrowserHandler,
                 DirectoryNavigateHandler
{
   interface Binder extends CommandBinder<Commands, Files> {}

 
   public interface Display extends WorkbenchView
   {   
      public interface NavigationObserver
      {
         void onFileNavigation(FileSystemItem file);
         void onSelectAllValueChanged(boolean value);
      }
      
      public interface Observer extends NavigationObserver
      {
         void onFileSelectionChanged();
         void onColumnSortOrderChanaged(JsArray<ColumnSortInfo> sortOrder);
      }
      
      void setObserver(Observer observer);
           
      
      void setColumnSortOrder(JsArray<ColumnSortInfo> sortOrder);
      
      void listDirectory(FileSystemItem directory, 
                         ServerDataSource<JsArray<FileSystemItem>> filesDS);
      
      void updateDirectoryListing(FileChange action);
      
      void renameFile(FileSystemItem from, FileSystemItem to);
      
      void selectAll();
      void selectNone();
      
      ArrayList<FileSystemItem> getSelectedFiles();
       
      void showFolderPicker(
            String title,
            RemoteFileSystemContext context,
            FileSystemItem initialDir,
            ProgressOperationWithInput<FileSystemItem> operation);
      
      void showFileUpload(
                     String targetURL,
                     FileSystemItem targetDirectory, 
                     RemoteFileSystemContext fileSystemContext,
                     OperationWithInput<PendingFileUpload> completedOperation);
      
      void showFileExport(String defaultName,
                          String defaultExtension,
                          ProgressOperationWithInput<String> operation);
   }

   @Inject
   public Files(Display view, 
                EventBus eventBus,
                FilesServerOperations server,
                RemoteFileSystemContext fileSystemContext,
                GlobalDisplay globalDisplay,
                Session session,
                Commands commands,
                Provider<FilesCopy> pFilesCopy,
                Provider<FilesUpload> pFilesUpload,
                FileTypeRegistry fileTypeRegistry,
                ConsoleDispatcher consoleDispatcher,
                WorkbenchContext workbenchContext)
   {
      super(view);
      view_ = view ;
      view_.setObserver(new DisplayObserver());
      fileTypeRegistry_ = fileTypeRegistry;
      consoleDispatcher_ = consoleDispatcher;
      workbenchContext_ = workbenchContext;
      
      eventBus_ = eventBus;
      server_ = server;
      fileSystemContext_ = fileSystemContext;
      globalDisplay_ = globalDisplay ;
      session_ = session;
      pFilesCopy_ = pFilesCopy;
      pFilesUpload_ = pFilesUpload;

      ((Binder)GWT.create(Binder.class)).bind(commands, this);

      
      eventBus_.addHandler(FileChangeEvent.TYPE, this);

      initSession();
   }

   private void initSession()
   {
      final SessionInfo sessionInfo = session_.getSessionInfo();
      ClientInitState state = sessionInfo.getClientState();

      // make the column sort order persistent
      new JSObjectStateValue(MODULE_FILES, KEY_SORT_ORDER, ClientState.PROJECT_PERSISTENT, state, false)
      {
         @Override
         protected void onInit(JsObject value)
         {
            if (value != null)
               columnSortOrder_ = value.cast();
            else
               columnSortOrder_ = null;
            
            lastKnownState_ = columnSortOrder_;
            
            view_.setColumnSortOrder(columnSortOrder_);
         }

         @Override
         protected JsObject getValue()
         {
            if (columnSortOrder_ != null)
               return columnSortOrder_.cast();
            else
               return null;
         }

         @Override
         protected boolean hasChanged()
         {
            if (lastKnownState_ != columnSortOrder_)
            {
               lastKnownState_ = columnSortOrder_;
               return true;
            }
            else
            {
               return false;
            }
         }

         private JsArray<ColumnSortInfo> lastKnownState_ = null;
      };
      
      
      // navigate to previous directory (works for resumed case)
      new StringStateValue(MODULE_FILES, KEY_PATH, ClientState.PROJECT_PERSISTENT, state) {
         @Override
         protected void onInit(final String value)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               public void execute()
               {
                  // if we've already navigated to a directory then
                  // we don't need to initialize from client state
                  if (hasNavigatedToDirectory_)
                     return;

                  // compute start dir
                  String path = transformPathStateValue(value);
                  FileSystemItem start = path != null
                                    ? FileSystemItem.createDir(path)
                                    : FileSystemItem.createDir(
                                          sessionInfo.getInitialWorkingDir());
                  navigateToDirectory(start);
               }
            });
         }

         @Override
         protected String getValue()
         {
            return currentPath_.getPath();
         }
         
         private String transformPathStateValue(String value)
         {
            // if the value is null then return null
            if (value == null)
               return null;
            
            // only respect the value for projects
            String projectFile = session_.getSessionInfo().getActiveProjectFile();
            if (projectFile == null)
               return null;
            
            // ensure that the value is within the project dir (it wouldn't 
            // be if the project directory has been moved or renamed)
            String projectDirPath = 
               FileSystemItem.createFile(projectFile).getParentPathString();
            if (value.startsWith(projectDirPath))
               return value;
            else
               return null;
         }
      };
   }
   
   

   public Display getDisplay()
   {
      return view_ ;
   }
   
   // observer for display
   public class DisplayObserver implements Display.Observer {
      
      public void onFileSelectionChanged()
      {    
      }
      
      public void onFileNavigation(FileSystemItem file)
      {
         if (file.isDirectory())
         {
            navigateToDirectory(file);
         }
         else
         {
            fileTypeRegistry_.openFile(file);
         }
      }
      
      public void onSelectAllValueChanged(boolean value)
      {
         if (value)
            view_.selectAll();
         else
            view_.selectNone();
      }

      public void onColumnSortOrderChanaged(
                                    JsArray<ColumnSortInfo> sortOrder)
      {
         columnSortOrder_ = sortOrder;
      }
   };
    

   @Handler
   void onRefreshFiles()
   {
      view_.listDirectory(currentPath_, currentPathFilesDS_);
   }

   @Handler
   void onNewFolder()
   {
      globalDisplay_.promptForText(
            "New Folder",
            "Please enter the new folder name",
            null,
            new ProgressOperationWithInput<String>()
            {
               public void execute(String input,
                                   final ProgressIndicator progress)
               {
                  progress.onProgress("Creating folder...");

                  String folderPath = currentPath_.completePath(input);
                  FileSystemItem folder = FileSystemItem.createDir(folderPath);

                  server_.createFolder(
                        folder,
                        new VoidServerRequestCallback(progress));
               }
            });
   }

   void onUploadFile()
   {
      pFilesUpload_.get().execute(currentPath_, fileSystemContext_);
   }
   
   @Handler
   void onCopyFile()
   {
      // copy selected files then clear the selection
      pFilesCopy_.get().execute(view_.getSelectedFiles(),
                                currentPath_,
                                new Command() {
                                    public void execute()
                                    {
                                       view_.selectNone();
                                    }});
   }
   
   
   @Handler
   void onMoveFiles()
   {
      // get currently selected files
      final ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();
      
      // validation: some selection exists
      if  (selectedFiles.size() == 0)
         return ;

      view_.showFolderPicker(
                        "Choose Folder", 
                        fileSystemContext_,
                        currentPath_,
                        new ProgressOperationWithInput<FileSystemItem>() {

         public void execute(final FileSystemItem targetDir,
                             final ProgressIndicator progress)
         {
            if (targetDir == null)
                  return;
            
            // check to make sure that we aren't moving any folders
            // onto themselves or moving any files into the directory
            // where they currently reside
            for (int i=0; i<selectedFiles.size(); i++)
            {
               FileSystemItem file = selectedFiles.get(i);
               FileSystemItem fileParent = file.getParentPath();
               
               if (file.getPath().equals(targetDir.getPath()) ||
                   fileParent.getPath().equals(targetDir.getPath()))
               {
                  progress.onError("Invalid target folder");
                  return ;
               } 
            }
            
            progress.onProgress("Moving files...");
            
            view_.selectNone();
      
            server_.moveFiles(selectedFiles, 
                              targetDir, 
                              new VoidServerRequestCallback(progress)); 
         }
      });
   }
   

   @Handler
   void onExportFiles()
   {
      // get currently selected files
      final ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();
      
      // validation: some selection exists
      if  (selectedFiles.size() == 0)
         return ;
         
      // case: single file which is not a folder 
      if ((selectedFiles.size()) == 1 && !selectedFiles.get(0).isDirectory())
      {
         final FileSystemItem file = selectedFiles.get(0);
         
         view_.showFileExport(file.getStem(),
                              file.getExtension(),
                              new ProgressOperationWithInput<String>(){
            public void execute(String name, ProgressIndicator progress)
            {
               // execute the download (open in a new window)
               globalDisplay_.openWindow(server_.getFileExportUrl(name, file));
               
            }
         });
      }
      
      // case: folder or multiple files
      else
      {
         // determine the default zip file name based on the selection
         String defaultArchiveName;
         if (selectedFiles.size() == 1)
            defaultArchiveName = selectedFiles.get(0).getStem();
         else
            defaultArchiveName = "rstudio-export";
         
         // prompt user
         final String ZIP = ".zip";
         view_.showFileExport(defaultArchiveName,
                              ZIP,
                              new ProgressOperationWithInput<String>(){
            
            public void execute(String archiveName, ProgressIndicator progress)
            {
               // force zip extension in case the user deleted it
               if (!archiveName.endsWith(ZIP))
                  archiveName += ZIP;
               
               // build list of filenames for current selection
               ArrayList<String> filenames = new ArrayList<String>();
               for (FileSystemItem file : selectedFiles)
                  filenames.add(file.getName());
               
               // execute the download (open in a new window)
               globalDisplay_.openWindow(server_.getFileExportUrl(archiveName, 
                                                                  currentPath_, 
                                                                  filenames));
            }
         });
      }
   }

   @Handler
   void onRenameFile()
   {
      // get currently selected files
      ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();
      
      // validation: some selection exists
      if  (selectedFiles.size() == 0)
         return ;
      
      // validation: no more than one file selected
      if  (selectedFiles.size() > 1)
      {
         globalDisplay_.showErrorMessage(
                           "Invalid Selection", 
                           "Please select only one file to rename");
         return ;
      }
      
      // prompt for new file name then execute the rename
      final FileSystemItem file = selectedFiles.get(0);
      globalDisplay_.promptForText("Rename File",
                                   "Please enter the new file name:",
                                   file.getName(),
                                   0,
                                   file.getStem().length(),
                                   null,
                                   new ProgressOperationWithInput<String>() {

                                      public void execute(String input,
                             final ProgressIndicator progress)
         {
            progress.onProgress("Renaming file...");

            String path = file.getParentPath().completePath(input);
            FileSystemItem target ;
            if (file.isDirectory())
               target = FileSystemItem.createDir(path);
            else
               target = FileSystemItem.createFile(path);
              
            // clear selection
            view_.selectNone();
            
            // premptively rename in the UI then fallback to refreshing
            // the view if there is an error
            view_.renameFile(file, target);
            
            // execute on the server
            server_.renameFile(file, 
                               target, 
                               new VoidServerRequestCallback(progress) {
                                 @Override
                                 protected void onFailure()
                                 {
                                    onRefreshFiles();
                                 }
                              });        
         }                                
      }); 
   }
   
   @Handler
   void onDeleteFiles()
   {
      // get currently selected files
      final ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();
      
      // validation: some selection exists
      if  (selectedFiles.size() == 0)
         return ;
      
      // confirm delete then execute it
      globalDisplay_.showYesNoMessage(
                        GlobalDisplay.MSG_QUESTION,
                        "Confirm Delete", 
                        "Are you sure you want to delete the selected files?", 
                        new ProgressOperation() {
                           public void execute(final ProgressIndicator progress)
                           {
                              progress.onProgress("Deleting files...");
                             
                              view_.selectNone();
                              
                              server_.deleteFiles(
                                    selectedFiles, 
                                    new VoidServerRequestCallback(progress));
                           }
                        },
                       true);
   }

   @Handler
   void onVcsRevertFiles()
   {
      // get currently selected files
      final ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();

      // validation: some selection exists
      if  (selectedFiles.size() == 0)
         return ;

      String noun = selectedFiles.size() == 1 ? "file" : "files";
      globalDisplay_.showYesNoMessage(
            GlobalDisplay.MSG_WARNING,
            "Revert Changes",
            "Changes to the selected " + noun + " will be lost, including " +
            "staged changes.\n\nAre you sure you want to continue?",
            new Operation()
            {
               @Override
               public void execute()
               {
                  ArrayList<String> paths = new ArrayList<String>();
                  for (FileSystemItem file : selectedFiles)
                     paths.add(file.getPath());
                  server_.vcsRevert(
                        paths,
                        new SimpleRequestCallback<Void>());
               }
            },
            false);
   }

   @Handler
   void onVcsAddFiles()
   {
      // get currently selected files
      final ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();

      // validation: some selection exists
      if  (selectedFiles.size() == 0)
         return ;

      ProgressIndicator progress = globalDisplay_.getProgressIndicator(
            session_.getSessionInfo().getVcsName());
      progress.onProgress("Adding file(s)...");

      ArrayList<String> paths = new ArrayList<String>();
      for (FileSystemItem file : selectedFiles)
         paths.add(file.getPath());
      server_.vcsAdd(
            paths,
            new VoidServerRequestCallback(progress));
   }

   @Handler
   void onVcsRemoveFiles()
   {
      // get currently selected files
      final ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();

      // validation: some selection exists
      if  (selectedFiles.size() == 0)
         return ;

      ProgressIndicator progress = globalDisplay_.getProgressIndicator(
            session_.getSessionInfo().getVcsName());
      progress.onProgress("Removing file(s)...");

      ArrayList<String> paths = new ArrayList<String>();
      for (FileSystemItem file : selectedFiles)
         paths.add(file.getPath());
      server_.vcsRemove(
            paths,
            new VoidServerRequestCallback(progress));
   }

   void onGoToWorkingDir()
   {
      view_.bringToFront();
      navigateToDirectory(workbenchContext_.getCurrentWorkingDir());
   }
   
   @Handler
   void onSetAsWorkingDir()
   {
      consoleDispatcher_.executeSetWd(currentPath_, true);
   }
   
   void onSetWorkingDirToFilesPane()
   {
      onSetAsWorkingDir();
   }
   

   @Handler
   void onShowFolder()
   {
      eventBus_.fireEvent(new ShowFolderEvent(currentPath_));
   }
   
   public void onFileChange(FileChangeEvent event)
   {
      view_.updateDirectoryListing(event.getFileChange());
   }

   public void onOpenFileInBrowser(OpenFileInBrowserEvent event)
   {
      FileSystemItem file = event.getFile();
      // show the file in a new window if we can get a file url for it
      String fileURL = server_.getFileUrl(file);
      if (fileURL !=  null)
      {
         globalDisplay_.openWindow(fileURL);
      }
   }
   
   public void onDirectoryNavigate(DirectoryNavigateEvent event)
   {
      navigateToDirectory(event.getDirectory());
   }
  
   private void navigateToDirectory(FileSystemItem directoryEntry)
   {
      hasNavigatedToDirectory_ = true;
      currentPath_ = directoryEntry;
      view_.listDirectory(currentPath_, currentPathFilesDS_);
      session_.persistClientState();
   }
   
   // data source for listing files on the current path which can 
   // be passed to the files view
   ServerDataSource<JsArray<FileSystemItem>> currentPathFilesDS_ = 
      new ServerDataSource<JsArray<FileSystemItem>>()
      {
         public void requestData(
               ServerRequestCallback<JsArray<FileSystemItem>> requestCallback)
         {
            // pass true to enable monitoring for all calls to list_files
            server_.listFiles(currentPath_, true, requestCallback);
         }
      };

   private final Display view_ ;
   private final FileTypeRegistry fileTypeRegistry_;
   private final ConsoleDispatcher consoleDispatcher_;
   private final WorkbenchContext workbenchContext_;
   private final FilesServerOperations server_;
   private final EventBus eventBus_;
   private final GlobalDisplay globalDisplay_ ;
   private final RemoteFileSystemContext fileSystemContext_;
   private final Session session_;
   private FileSystemItem currentPath_ = FileSystemItem.home();
   private boolean hasNavigatedToDirectory_ = false;
   private final Provider<FilesCopy> pFilesCopy_;
   private final Provider<FilesUpload> pFilesUpload_;
   private static final String MODULE_FILES = "files-pane";
   private static final String KEY_PATH = "path";
   private static final String KEY_SORT_ORDER = "sortOrder";
   private JsArray<ColumnSortInfo> columnSortOrder_ = null;
}
