/*
 * TextEditingTargetWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.layout.RequiresVisibilityChanged;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.core.client.widget.WarningBar;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget.Display;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplace;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarWidget;

public class TextEditingTargetWidget
      extends ResizeComposite
      implements Display, RequiresVisibilityChanged
{
   public TextEditingTargetWidget(Commands commands,
                                  UIPrefs uiPrefs,
                                  DocDisplay editor,
                                  TextFileType fileType,
                                  EventBus events)
   {
      commands_ = commands;
      uiPrefs_ = uiPrefs;
      editor_ = editor;
      sourceOnSave_ = new CheckBox("Source on Save");
      statusBar_ = new StatusBarWidget();
      panel_ = new PanelWithToolbar(createToolbar(),
                                    editor.asWidget(),
                                    statusBar_);
      adaptToFileType(fileType);

      initWidget(panel_);
   }

   private StatusBarWidget statusBar_;

   private Toolbar createToolbar()
   {
      Toolbar toolbar = new EditingTargetToolbar(commands_);
       
      toolbar.addLeftWidget(commands_.saveSourceDoc().createToolbarButton());
      toolbar.addLeftWidget(sourceOnSave_);

      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(createFindReplaceButton());
      toolbar.addLeftWidget(createCodeTransformMenuButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.compilePDF().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.publishPDF().createToolbarButton());

      toolbar.addRightWidget(commands_.executeCode().createToolbarButton());
      toolbar.addRightSeparator();
      toolbar.addRightWidget(commands_.executeLastCode().createToolbarButton());
      toolbar.addRightSeparator();
      final String SOURCE_BUTTON_TITLE = "Source the active document"; 
      
      sourceButton_ = new ToolbarButton(
            "Source", 
            commands_.sourceActiveDocument().getImageResource(), 
            new ClickHandler() 
            {
               @Override
               public void onClick(ClickEvent event)
               {
                  if (uiPrefs_.sourceWithEcho().getValue())
                     commands_.sourceActiveDocumentWithEcho().execute();
                  else
                     commands_.sourceActiveDocument().execute();
               }
            });
      
      sourceButton_.setTitle(SOURCE_BUTTON_TITLE);
      toolbar.addRightWidget(sourceButton_);
      
      uiPrefs_.sourceWithEcho().addValueChangeHandler(
                                       new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            if (event.getValue())
               sourceButton_.setTitle(SOURCE_BUTTON_TITLE + " (with echo)");
            else
               sourceButton_.setTitle(SOURCE_BUTTON_TITLE);
         }
      });
            
      ToolbarPopupMenu sourceMenu = new ToolbarPopupMenu();
      sourceMenu.addItem(commands_.sourceActiveDocument().createMenuItem(false));
      sourceMenu.addItem(commands_.sourceActiveDocumentWithEcho().createMenuItem(false));
      
      sourceMenuButton_ = new ToolbarButton(sourceMenu, true);
      toolbar.addRightWidget(sourceMenuButton_);      
            
      return toolbar;
   }

   private Widget createFindReplaceButton()
   {
      if (findReplaceBar_ == null)
      {
         findReplaceButton_ = new ToolbarButton(
               FindReplaceBar.getFindIcon(),
               new ClickHandler() {
                  public void onClick(ClickEvent event)
                  {
                     if (findReplaceBar_ == null)
                        showFindReplace();
                     else
                        hideFindReplace();
                  }
               });
         findReplaceButton_.setTitle("Find/Replace");
      }
      return findReplaceButton_;
   }

   private Widget createCodeTransformMenuButton()
   {
      if (codeTransform_ == null)
      {
         ImageResource icon = ThemeResources.INSTANCE.codeTransform();

         ToolbarPopupMenu menu = new ToolbarPopupMenu();
         menu.addItem(commands_.goToFunctionDefinition().createMenuItem(false));
         menu.addSeparator();
         menu.addItem(commands_.extractFunction().createMenuItem(false));
         menu.addItem(commands_.commentUncomment().createMenuItem(false));
         menu.addItem(commands_.reindent().createMenuItem(false));
         codeTransform_ = new ToolbarButton("", icon, menu);
         codeTransform_.setTitle("Code Tools");
      }
      return codeTransform_;
   }

   public void adaptToFileType(TextFileType fileType)
   {
      editor_.setFileType(fileType);
      sourceOnSave_.setVisible(fileType.canSourceOnSave());
      codeTransform_.setVisible(fileType.canExecuteCode());
      sourceButton_.setVisible(fileType.canExecuteCode());
      sourceMenuButton_.setVisible(fileType.canExecuteCode());
   }

   public HasValue<Boolean> getSourceOnSave()
   {
      return sourceOnSave_;
   }

   public void ensureVisible()
   {
      fireEvent(new EnsureVisibleEvent());
   }

   public void showWarningBar(String warning)
   {
      if (warningBar_ == null)
      {
         warningBar_ = new WarningBar();
         panel_.insertNorth(warningBar_, warningBar_.getHeight(), null);
      }
      warningBar_.setText(warning);
   }

   public void hideWarningBar()
   {
      if (warningBar_ != null)
      {
         panel_.remove(warningBar_);
      }
   }

   public void showFindReplace()
   {
      if (findReplaceBar_ == null)
      {
         findReplaceBar_ = new FindReplaceBar();
         new FindReplace((AceEditor)editor_,
                         findReplaceBar_,
                         RStudioGinjector.INSTANCE.getGlobalDisplay());
         panel_.insertNorth(findReplaceBar_,
                            findReplaceBar_.getHeight(),
                            warningBar_);
         findReplaceBar_.getCloseButton().addClickHandler(new ClickHandler()
         {
            public void onClick(ClickEvent event)
            {
               hideFindReplace();
            }
         });

         findReplaceButton_.setLeftImage(FindReplaceBar.getFindLatchedIcon());
      }
      findReplaceBar_.focusFindField(true);
   }

   private void hideFindReplace()
   {
      if (findReplaceBar_ != null)
      {
         panel_.remove(findReplaceBar_);
         findReplaceBar_ = null;
         findReplaceButton_.setLeftImage(FindReplaceBar.getFindIcon());
      }
      editor_.focus();
   }

   public void onActivate()
   {
      editor_.onActivate();
   }

   public void setFontSize(double size)
   {
      editor_.setFontSize(size);
   }

   public StatusBar getStatusBar()
   {
      return statusBar_;
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   public void onVisibilityChanged(boolean visible)
   {
      editor_.onVisibilityChanged(visible);
   }

   private final Commands commands_;
   private final UIPrefs uiPrefs_;
   private final DocDisplay editor_;
   private CheckBox sourceOnSave_;
   private PanelWithToolbar panel_;
   private WarningBar warningBar_;
   private FindReplaceBar findReplaceBar_;
   private ToolbarButton findReplaceButton_;
   private ToolbarButton codeTransform_;
   private ToolbarButton sourceButton_;
   private ToolbarButton sourceMenuButton_;
}
