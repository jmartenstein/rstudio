/*
 * WorkbenchScreen.java
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

package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.WindowStateChangeEvent;
import org.rstudio.core.client.layout.WindowState;
import org.rstudio.core.client.theme.ModuleTabLayoutPanel;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.ChangeFontSizeHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.ui.appended.ApplicationEndedPopupPanel;
import org.rstudio.studio.client.workbench.FileMRUList;
import org.rstudio.studio.client.workbench.WorkbenchMainView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.*;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchMetrics;
import org.rstudio.studio.client.workbench.ui.PaneManager.Tab;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedEvent;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedHandler;
import org.rstudio.studio.client.workbench.views.edit.Edit;
import org.rstudio.studio.client.workbench.views.edit.Edit.Shim;
import org.rstudio.studio.client.workbench.views.edit.events.ShowEditorEvent;
import org.rstudio.studio.client.workbench.views.plots.PlotsTab;
import org.rstudio.studio.client.workbench.views.source.events.LastSourceDocClosedEvent;
import org.rstudio.studio.client.workbench.views.source.events.LastSourceDocClosedHandler;

public class WorkbenchScreen extends Composite 
                             implements WorkbenchMainView,
                                        SelectionHandler<Integer>,
                                        RequiresResize
{
   interface MyCommandBinder extends CommandBinder<Commands, WorkbenchScreen>{}
   static MyCommandBinder commandBinder = GWT.create(MyCommandBinder.class);

   @Inject
   public WorkbenchScreen(EventBus eventBus,
                          Session session,
                          Provider<PaneManager> pPaneManager,
                          final Edit.Shim edit,
                          Commands commands,
                          final Provider<FileMRUList> mruList,
                          FontSizeManager fontSizeManager,
                          OptionsLoader.Shim optionsLoader)
   {
      eventBus_ = eventBus;
      session_ = session;
      edit_ = edit;
      optionsLoader_ = optionsLoader;

      eventBus_.addHandler(ShowEditorEvent.TYPE, edit);
      eventBus_.addHandler(ChangeFontSizeEvent.TYPE, new ChangeFontSizeHandler()
      {
         public void onChangeFontSize(ChangeFontSizeEvent event)
         {
            FontSizer.setNormalFontSize(Document.get(), event.getFontSize());
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               public void execute()
               {
                  // Causes the console width to be remeasured
                  doOnPaneSizesChanged();
               }
            });
         }
      });
      FontSizer.setNormalFontSize(Document.get(), fontSizeManager.getSize());


      paneManager_ = pPaneManager.get();
      tabsPanel_ = paneManager_.getPanel();
      tabsPanel_.setSize("100%", "100%");
      tabsPanel_.addStyleDependentName("Workbench");

      // Prevent doOnPaneSizesChanged() from being called more than once
      // every N milliseconds. Note that the act of sending the client metrics
      // to the server also has its own buffer, so this one only needs to
      // minimize the expense of doing the calculation, not of sending data to
      // the server.
      paneSizesChangedCommand_ = new TimeBufferedCommand(200)
      {
         @Override
         protected void performAction(boolean shouldSchedulePassive)
         {
            assert !shouldSchedulePassive;
            doOnPaneSizesChanged();
         }
      };

      eventBus.addHandler(SessionInitEvent.TYPE, new SessionInitHandler()
      {
         public void onSessionInit(SessionInitEvent sie)
         {
            prefetch();
            mruList.get();
         }
      });

      eventBus.addHandler(LastSourceDocClosedEvent.TYPE,
                          new LastSourceDocClosedHandler()
      {
         public void onLastSourceDocClosed(LastSourceDocClosedEvent event)
         {
            paneManager_.getSourceLogicalWindow().onWindowStateChange(
                  new WindowStateChangeEvent(WindowState.HIDE));
         }
      });

      eventBus.addHandler(WorkingDirChangedEvent.TYPE,
                          new WorkingDirChangedHandler()
                          {
                             public void onWorkingDirChanged(
                                   WorkingDirChangedEvent event)
                             {
                                updateWorkingDirectory(event.getPath());
                             }
                          });

      ((PlotsTab) paneManager_.getTab(Tab.Plots)).addResizeHandler(new ResizeHandler()
      {
         public void onResize(ResizeEvent event)
         {
            onPaneSizesChanged();
         }
      });
      tabsPanel_.addSplitterResizedHandler(new SplitterResizedHandler()
      {
         public void onSplitterResized(SplitterResizedEvent event)
         {
            onPaneSizesChanged();
         }
      });

      // init widget
      initWidget(tabsPanel_);

      commandBinder.bind(commands, this);
   }

   private void prefetch()
   {
      final SerializedCommandQueue prefetchQueue = new SerializedCommandQueue();
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
         public void execute()
         {
            onPaneSizesChanged();
         }
      });

      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            for (final WorkbenchTab tab : paneManager_.getAllTabs())
               prefetchQueue.addCommand(new SerializedCommand()
               {
                  public void onExecute(Command continuation)
                  {
                     tab.prefetch(continuation);
                  }
               });
            prefetchQueue.addCommand(new SerializedCommand()
            {
               public void onExecute(Command continuation)
               {
                  ApplicationEndedPopupPanel.prefetch(continuation);
               }
            });
            prefetchQueue.addCommand(new SerializedCommand()
            {
               public void onExecute(Command continuation)
               {
                  edit_.forceLoad(true, continuation);
               }
            });
            prefetchQueue.addCommand(new SerializedCommand()
            {
               public void onExecute(Command continuation)
               {
                  optionsLoader_.forceLoad(true, continuation);
               }
            });
         }
      });
   }

   private void updateWorkingDirectory(String path)
   {
      if (!path.endsWith("/"))
         path += "/";
      paneManager_.getConsoleFrame().setSubtitle(path);
   }

   public void onResize()
   {
      tabsPanel_.onResize();
      onPaneSizesChanged();
   }
         
   @Override
   protected void onLoad()
   {
      super.onLoad();
      eventBus_.fireEvent(new WorkbenchLoadedEvent());
   }
   

   private void onPaneSizesChanged()
   {
      paneSizesChangedCommand_.nudge();
   }

   private void doOnPaneSizesChanged()
   {
      // console width
      int consoleWidth = paneManager_.getConsole().getCharacterWidth();

      // plots size (don't allow negative metrics)
      WorkbenchTabPanel plotPanel = paneManager_.getOwnerTabPanel(Tab.Plots);
      Size deckPanelSize = new Size(
            plotPanel.getOffsetWidth(),
            plotPanel.getOffsetHeight() - ModuleTabLayoutPanel.BAR_HEIGHT);

      Size plotsSize = new Size(
               Math.max(deckPanelSize.width, 0),
               Math.max(deckPanelSize.height - Toolbar.DEFAULT_HEIGHT, 0));
   
      WorkbenchMetrics metrics = WorkbenchMetrics.create(consoleWidth,
                                                         plotsSize.width,
                                                         plotsSize.height);

      // make sure we don't send very similar metrics values twice (it is
      // an expensive operation since it involves at least 2 http requests)
      if (!metrics.closeEnoughToPrevious(lastMetrics_))
      {
         lastMetrics_ = metrics;
         eventBus_.fireEvent(new WorkbenchMetricsChangedEvent(metrics));
      }

      session_.persistClientState();
   }

   public void onSelection(SelectionEvent<Integer> integerSelectionEvent)
   {
      eventBus_.fireEvent(new PushClientStateEvent());
   }

   @Handler
   void onActivateWorkspace() { paneManager_.activateTab(Tab.Workspace); }
   @Handler
   void onActivateHistory() { paneManager_.activateTab(Tab.History); }
   @Handler
   void onActivateFiles() { paneManager_.activateTab(Tab.Files); }
   @Handler
   void onActivatePlots() { paneManager_.activateTab(Tab.Plots); }
   @Handler
   void onActivatePackages() { paneManager_.activateTab(Tab.Packages); }
   @Handler
   void onActivateHelp() { paneManager_.activateTab(Tab.Help); }

   @Handler
   void onShowOptions()
   {
      optionsLoader_.showOptions();
   }
   
   public Widget asWidget()
   {
      return this;
   }

   private TimeBufferedCommand paneSizesChangedCommand_;

   private WorkbenchMetrics lastMetrics_ = WorkbenchMetrics.create(0,0,0);
   
   private final EventBus eventBus_;
   private final Session session_;
   private final Shim edit_;
   private final org.rstudio.studio.client.workbench.ui.OptionsLoader.Shim optionsLoader_;

   private final MainSplitPanel tabsPanel_ ;
   private PaneManager paneManager_;

}
