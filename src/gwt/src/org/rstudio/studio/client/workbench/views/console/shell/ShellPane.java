/*
 * ShellPane.java
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
package org.rstudio.studio.client.workbench.views.console.shell;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Text;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.VirtualConsole;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.core.client.widget.BottomScrollPanel;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.PreWidget;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.model.ConsoleAction;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.console.events.HelpAtCursorEvent;
import org.rstudio.studio.client.workbench.views.console.events.HelpAtCursorHandler;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor.NewLineMode;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;

public class ShellPane extends Composite implements Shell.Display,
                                                    RequiresResize
{
   @Inject
   public ShellPane(AceEditor editor, UIPrefs uiPrefs)
   {
      styles_ = ConsoleResources.INSTANCE.consoleStyles();

      SelectInputClickHandler secondaryInputHandler = new SelectInputClickHandler();

      output_ = new PreWidget();
      output_.setStylePrimaryName(styles_.output());
      output_.addClickHandler(secondaryInputHandler);

      pendingInput_ = new PreWidget();
      pendingInput_.setStyleName(styles_.output());
      pendingInput_.addClickHandler(secondaryInputHandler);

      prompt_ = new HTML() ;
      prompt_.setStylePrimaryName(styles_.prompt()) ;
      prompt_.addStyleName(KEYWORD_CLASS_NAME);

      input_ = editor ;
      input_.setShowLineNumbers(false);
      input_.setShowPrintMargin(false);
      input_.setFileType(FileTypeRegistry.R, true);
      if (!Desktop.isDesktop())
         input_.setNewLineMode(NewLineMode.Unix);
      input_.setUseWrapMode(true);
      input_.setPadding(0);
      input_.autoHeight();
      final Widget inputWidget = input_.asWidget();
      input_.addClickHandler(secondaryInputHandler) ;
      inputWidget.addStyleName(styles_.input());
      input_.addCursorChangedHandler(new CursorChangedHandler()
      {
         public void onCursorChanged(CursorChangedEvent event)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  input_.scrollToCursor(scrollPanel_, 8, 60);
               }
            });
         }
      });
      input_.addCapturingKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            // If the user hits Page-Up from inside the console input, we need
            // to simulate pageup because focus is not contained in the scroll
            // panel (it's in the hidden textarea that Ace uses under the
            // covers).

            int keyCode = event.getNativeKeyCode();
            switch (keyCode)
            {
               case KeyCodes.KEY_PAGEUP:
                  event.stopPropagation();
                  event.preventDefault();

                  // Can't scroll any further up. Return before we change focus.
                  if (scrollPanel_.getVerticalScrollPosition() == 0)
                     return;

                  scrollPanel_.focus();
                  int newScrollTop = scrollPanel_.getVerticalScrollPosition() -
                                     scrollPanel_.getOffsetHeight() + 40;
                  scrollPanel_.setVerticalScrollPosition(Math.max(0, newScrollTop));
                  break;
            }
         }
      });

      uiPrefs.syntaxColorConsole().bind(new CommandWithArg<Boolean>()
      {
         public void execute(Boolean arg)
         {
            if (arg)
               inputWidget.removeStyleName("nocolor");
            else
               inputWidget.addStyleName("nocolor");
         }
      });

      inputLine_ = new DockPanel();
      inputLine_.setHorizontalAlignment(DockPanel.ALIGN_LEFT);
      inputLine_.setVerticalAlignment(DockPanel.ALIGN_TOP);
      inputLine_.add(prompt_, DockPanel.WEST);
      inputLine_.setCellWidth(prompt_, "1");
      inputLine_.add(input_.asWidget(), DockPanel.CENTER);
      inputLine_.setCellWidth(input_.asWidget(), "100%");
      inputLine_.setWidth("100%");

      verticalPanel_ = new VerticalPanel() ;
      verticalPanel_.setStylePrimaryName(styles_.console());
      verticalPanel_.addStyleName("ace_text-layer");
      verticalPanel_.addStyleName("ace_line");
      FontSizer.applyNormalFontSize(verticalPanel_);
      verticalPanel_.add(output_) ;
      verticalPanel_.add(pendingInput_) ;
      verticalPanel_.add(inputLine_) ;
      verticalPanel_.setWidth("100%") ;

      scrollPanel_ = new ClickableScrollPanel() ;
      scrollPanel_.setWidget(verticalPanel_) ;
      scrollPanel_.addStyleName("ace_editor");
      scrollPanel_.addStyleName("ace_scroller");
      scrollPanel_.addClickHandler(secondaryInputHandler);
      scrollPanel_.addKeyDownHandler(secondaryInputHandler);

      secondaryInputHandler.setInput(editor);

      scrollToBottomCommand_ = new TimeBufferedCommand(5)
      {
         @Override
         protected void performAction(boolean shouldSchedulePassive)
         {
            if (!DomUtils.selectionExists())
               scrollPanel_.scrollToBottom();
         }
      };

      initWidget(scrollPanel_) ;

      addCopyHook(getElement());
   }

   private native void addCopyHook(Element element) /*-{
      if ($wnd.desktop) {
         var clean = function() {
            setTimeout(function() {
               $wnd.desktop.cleanClipboard(true);
            }, 100)
         };
         element.addEventListener("copy", clean, true);
         element.addEventListener("cut", clean, true);
      }
   }-*/;

   private void scrollToBottomAsync()
   {
      scrollToBottomCommand_.nudge();
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            input_.autoHeight();
            // Console scroll pos jumps on first typing without this, because the
            // textarea is in the upper left corner of the screen and when focus
            // moves to it scrolling ensues.
            input_.forceCursorChange();
            scrollPanel_.scrollToBottom();
         }
      });
   }

   public void consoleWriteError(String error)
   {
      output(error, styles_.error(), false);
      scrollToBottomAsync();
   }

   public void consoleWriteOutput(String output)
   {
      output(output, styles_.output(), false);
      scrollToBottomAsync();
   }

   public void consoleWriteInput(String input)
   {
      pendingInput_.setText("");
      pendingInput_.setVisible(false);
      output(input, styles_.command() + KEYWORD_CLASS_NAME, false);
      scrollToBottomAsync();
   }

   public void consoleWritePrompt(String prompt)
   {
      output(prompt, styles_.prompt() + KEYWORD_CLASS_NAME, false);
      scrollToBottomAsync();
   }

   public void consolePrompt(String prompt)
   {
      if (prompt != null)
         prompt = VirtualConsole.consolify(prompt);

      prompt_.getElement().setInnerText(prompt);
      //input_.clear() ;
      ensureInputVisible();

      // Deal gracefully with multi-line prompts
      int promptLines = StringUtil.notNull(prompt).split("\\n").length;
      input_.asWidget().getElement().getStyle().setPaddingTop((promptLines - 1) * 15,
                                                   Unit.PX);
   }

   public void ensureInputVisible()
   {
      scrollPanel_.scrollToBottom();
   }

   private boolean output(String text,
                          String className,
                          boolean addToTop)
   {
      Node node;
      boolean isOutput = StringUtil.isNullOrEmpty(className)
                         || className.equals(styles_.output());

      if (isOutput && !addToTop && trailingOutput_ != null)
      {
         // Short-circuit the case where we're appending output to the
         // bottom, and there's already some output there. We need to
         // treat this differently in case the new output uses control
         // characters to pound over parts of the previous output.

         int oldLineCount = DomUtils.countLines(trailingOutput_, true);
         trailingOutputConsole_.submit(text);
         trailingOutput_.setNodeValue(
               ensureNewLine(trailingOutputConsole_.toString()));
         int newLineCount = DomUtils.countLines(trailingOutput_, true);
         lines_ += newLineCount - oldLineCount;
      }
      else
      {
         Element outEl = output_.getElement();

         text = VirtualConsole.consolify(text);
         if (isOutput)
         {
            VirtualConsole console = new VirtualConsole();
            console.submit(text);
            String consoleSnapshot = console.toString();

            // We use ensureNewLine to make sure that even if output
            // doesn't end with \n, a prompt will appear on its own line.
            // However, if we call ensureNewLine indiscriminantly (i.e.
            // on an output that's going to be followed by another output)
            // we can end up inserting newlines where they don't belong.
            //
            // It's safe to add a newline when we're appending output to
            // the end of the console, because if the next append is also
            // output, we'll use the contents of VirtualConsole and the
            // newline we add here will be plowed over.
            //
            // If we're prepending output to the top of the console, then
            // it's safe to add a newline if the next chunk (which is already
            // there) is something besides output.
            if (!addToTop ||
                (!outEl.hasChildNodes()
                 || outEl.getFirstChild().getNodeType() != Node.TEXT_NODE))
            {
               consoleSnapshot = ensureNewLine(consoleSnapshot);
            }

            node = Document.get().createTextNode(consoleSnapshot);
            if (!addToTop)
            {
               trailingOutput_ = (Text) node;
               trailingOutputConsole_ = console;
            }
         }
         else
         {
            SpanElement span = Document.get().createSpanElement();
            span.setClassName(className);
            span.setInnerText(text);
            node = span;
            if (!addToTop)
            {
               trailingOutput_ = null;
               trailingOutputConsole_ = null;
            }
         }

         if (addToTop)
            outEl.insertFirst(node);
         else
            outEl.appendChild(node);

         lines_ += DomUtils.countLines(node, true);
      }
      return !trimExcess();
   }

   private String ensureNewLine(String s)
   {
      if (s.length() == 0 || s.charAt(s.length() - 1) == '\n')
         return s;
      else
         return s + '\n';
   }

   private boolean trimExcess()
   {
      if (maxLines_ <= 0)
         return false;  // No limit in effect

      int linesToTrim = lines_ - maxLines_;
      if (linesToTrim > 0)
      {
         lines_ -= DomUtils.trimLines(output_.getElement(),
                                      lines_ - maxLines_);
         return true;
      }

      return false;
   }

   public void playbackActions(final RpcObjectList<ConsoleAction> actions)
   {
      Scheduler.get().scheduleIncremental(new RepeatingCommand()
      {
         private int i = actions.length() - 1;
         private int chunksize = 1000;

         public boolean execute()
         {
            int end = i - chunksize;
            chunksize = 10;
            for (; i > end && i >= 0; i--)
            {
               // User hit Ctrl+L at some point--we're done.
               if (cleared_)
                  return false;

               boolean canContinue = false;

               ConsoleAction action = actions.get(i);
               switch (action.getType())
               {
                  case ConsoleAction.INPUT:
                     canContinue = output(action.getData() + "\n",
                                          styles_.command() + " " + KEYWORD_CLASS_NAME,
                                          true);
                     break;
                  case ConsoleAction.OUTPUT:
                     canContinue = output(action.getData(),
                                          styles_.output(),
                                          true);
                     break;
                  case ConsoleAction.ERROR:
                     canContinue = output(action.getData(),
                                          styles_.error(),
                                          true);
                     break;
                  case ConsoleAction.PROMPT:
                     canContinue = output(action.getData(),
                                          styles_.prompt() + " " + KEYWORD_CLASS_NAME,
                                          true);
                     break;
               }
               if (!canContinue)
                  return false;
            }
            if (!DomUtils.selectionExists())
               scrollPanel_.scrollToBottom();

            return i >= 0;
         }
      });
   }

   public void setFocus(boolean focused)
   {
      input_.setFocus(focused) ;
   }
   
   /**
    * Directs focus/selection to the input box when a (different) widget
    * is clicked.
    */
   private class SelectInputClickHandler implements ClickHandler,
                                                    KeyDownHandler
   {
      public void onClick(ClickEvent event)
      {
         // If clicking on the input panel already, stop propagation.
         if (event.getSource() == input_)
         {
            event.stopPropagation();
            return;
         }

         // Don't drive focus to the input unless there is no selection.
         // Otherwise it would interfere with the ability to select stuff
         // from the output buffer for copying to the clipboard.
         // (BUG: DomUtils.selectionExists() doesn't work in a timely
         // fashion on IE8.)
         if (!DomUtils.selectionExists() && isInputOnscreen())
         {
            input_.setFocus(true) ;
            DomUtils.collapseSelection(false);
         }
      }

      public void onKeyDown(KeyDownEvent event)
      {
         if (event.getSource() == input_)
            return;

         // Filter out some keystrokes you might reasonably expect to keep
         // focus inside the output pane
         switch (event.getNativeKeyCode())
         {
            case KeyCodes.KEY_PAGEDOWN:
            case KeyCodes.KEY_PAGEUP:
            case KeyCodes.KEY_HOME:
            case KeyCodes.KEY_END:
            case KeyCodes.KEY_CTRL:
            case KeyCodes.KEY_ALT:
            case KeyCodes.KEY_SHIFT:
            case 224: // META (Command) on Firefox/Mac
               return;
            case 91: case 93: // Left/Right META (Command), but also [ and ], on Safari
               if (event.isMetaKeyDown())
                  return;
               break;
            case 'C':
               if (event.isControlKeyDown() || event.isMetaKeyDown())
                  return;
               break;
            case 112: // F1
               int modifiers = KeyboardShortcut.getModifierValue(event.getNativeEvent());
               if (modifiers == KeyboardShortcut.NONE)
               {
                  if (DomUtils.isSelectionInElement(scrollPanel_.getElement()))
                  {
                     String sel = DomUtils.getSelectionText(Document.get());
                     if (!StringUtil.isNullOrEmpty(sel))
                     {
                        event.preventDefault();
                        event.stopPropagation();
                        fireEvent(new HelpAtCursorEvent(sel));
                        return;
                     }
                  }
               }
         }
         input_.setFocus(true);
         delegateEvent(input_.asWidget(), event);
      }

      public void setInput(AceEditor input)
      {
         input_ = input;
      }

      private AceEditor input_;
   }

   private boolean isInputOnscreen()
   {
      return DomUtils.isVisibleVert(scrollPanel_.getElement(),
                                    inputLine_.getElement());
   }

   private class ClickableScrollPanel extends BottomScrollPanel
   {
      private ClickableScrollPanel()
      {
         super();
         getElement().setTabIndex(-1);
      }

      public HandlerRegistration addClickHandler(ClickHandler handler)
      {
         return addDomHandler(handler, ClickEvent.getType());
      }

      public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
      {
         return addDomHandler(handler, KeyDownEvent.getType());
      }

      public void focus()
      {
         getElement().focus();
      }
   }

   public void clearOutput()
   {
      output_.setText("") ;
      lines_ = 0;
      cleared_ = true;
   }

   public InputEditorDisplay getInputEditorDisplay()
   {
      return input_ ;
   }

   public String processCommandEntry()
   {
      // parse out the command text
      String promptText = prompt_.getElement().getInnerText();
      String commandText = input_.getCode();
      input_.setText("");
      // Force render to avoid subtle command movement in the console, caused
      // by the prompt disappearing before the input line does
      input_.forceImmediateRender();
      prompt_.setHTML("");

      SpanElement pendingPrompt = Document.get().createSpanElement();
      pendingPrompt.setInnerText(promptText);
      pendingPrompt.setClassName(styles_.prompt() + " " + KEYWORD_CLASS_NAME);

      SpanElement pendingInput = Document.get().createSpanElement();
      pendingInput.setInnerText(StringUtil.notNull(commandText).split("\n")[0] + "\n");
      pendingInput.setClassName(styles_.command() + " " + KEYWORD_CLASS_NAME);
      pendingInput_.getElement().appendChild(pendingPrompt);
      pendingInput_.getElement().appendChild(pendingInput);
      pendingInput_.setVisible(true);

      ensureInputVisible();

      return commandText ;
   }

   public HandlerRegistration addCapturingKeyDownHandler(KeyDownHandler handler)
   {
      return input_.addCapturingKeyDownHandler(handler) ;
   }

   @Override
   public HandlerRegistration addHelpAtCursorHandler(HelpAtCursorHandler handler)
   {
      return addHandler(handler, HelpAtCursorEvent.TYPE);
   }

   public HandlerRegistration addKeyPressHandler(KeyPressHandler handler)
   {
      return input_.addKeyPressHandler(handler) ;
   }
   
   public int getCharacterWidth()
   {
      // create width checker label and add it to the root panel
      Label widthChecker = new Label();
      widthChecker.setStylePrimaryName(styles_.console());
      FontSizer.applyNormalFontSize(widthChecker);
      RootPanel.get().add(widthChecker, -1000, -1000);
      
      // put the text into the label, measure it, and remove it
      String text = new String("abcdefghijklmnopqrstuvwzyz0123456789");
      widthChecker.setText(text);
      int labelWidth = widthChecker.getOffsetWidth();
      RootPanel.get().remove(widthChecker);
      
      // compute the points per character 
      int pointsPerCharacter = labelWidth / text.length();
      
      // compute client width
      int clientWidth = getElement().getClientWidth();
      int offsetWidth = getOffsetWidth();
      if (clientWidth == offsetWidth)
      {
         // if the two widths are the same then there are no scrollbars.
         // however, we know there will eventually be a scrollbar so we 
         // should offset by an estimated amount
         // (is there a more accurate way to estimate this?)
         final int ESTIMATED_SCROLLBAR_WIDTH = 19;
         clientWidth -= ESTIMATED_SCROLLBAR_WIDTH;
      }
      
      // compute character width (add pad so characters aren't flush to right)
      final int RIGHT_CHARACTER_PAD = 2;
      int width = (clientWidth / pointsPerCharacter) - RIGHT_CHARACTER_PAD ;
      
      // enforce a minimum width
      final int MINIMUM_WIDTH = 30;
      return Math.max(width, MINIMUM_WIDTH);
   }

   public boolean isPromptEmpty()
   {
      return StringUtil.isNullOrEmpty(prompt_.getText());
   }

   public void setMaxOutputLines(int maxLines)
   {
      maxLines_ = maxLines;
      trimExcess();
   }

   public void onResize()
   {
      if (getWidget() instanceof RequiresResize)
         ((RequiresResize)getWidget()).onResize();
   }

   private int lines_ = 0;
   private int maxLines_ = -1;
   private boolean cleared_ = false;
   private final PreWidget output_ ;
   private PreWidget pendingInput_ ;
   // Save a reference to the most recent output text node in case the
   // next bit of output contains \b or \r control characters
   private Text trailingOutput_ ;
   private VirtualConsole trailingOutputConsole_ ;
   private final HTML prompt_ ;
   private final AceEditor input_ ;
   private final DockPanel inputLine_ ;
   private final VerticalPanel verticalPanel_ ;
   private final ClickableScrollPanel scrollPanel_ ;
   private ConsoleResources.ConsoleStyles styles_;
   private final TimeBufferedCommand scrollToBottomCommand_;

   private static final String KEYWORD_CLASS_NAME = " ace_keyword";
}
