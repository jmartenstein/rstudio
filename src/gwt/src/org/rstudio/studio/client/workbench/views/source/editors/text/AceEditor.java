/*
 * AceEditor.java
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.dom.client.Style.*;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.DynamicIFrame;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.ChangeTracker;
import org.rstudio.studio.client.workbench.model.EventBasedChangeTracker;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager.InitCompletionFilter;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionPopupPanel;
import org.rstudio.studio.client.workbench.views.console.shell.assist.NullCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.RCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.*;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.UndoRedoHandler;
import org.rstudio.studio.client.workbench.views.source.events.RecordNavigationPositionEvent;
import org.rstudio.studio.client.workbench.views.source.events.RecordNavigationPositionHandler;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

public class AceEditor implements DocDisplay, 
                                  InputEditorDisplay,
                                  NavigableSourceEditor
{
   public enum NewLineMode
   {
      Windows("windows"),
      Unix("unix"),
      Auto("auto");

      NewLineMode(String type)
      {
         this.type = type;
      }

      public String getType()
      {
         return type;
      }

      private String type;
   }

   private class Filter implements InitCompletionFilter
   {
      public boolean shouldComplete(NativeEvent event)
      {
         // Never complete if there's an active selection
         Range range = getSession().getSelection().getRange();
         if (!range.isEmpty())
            return false;

         // Don't consider Tab to be a completion if we're at the start of a
         // line (e.g. only zero or more whitespace characters between the
         // beginning of the line and the cursor)

         if (event.getKeyCode() != KeyCodes.KEY_TAB)
            return true;

         int col = range.getStart().getColumn();
         if (col == 0)
            return false;

         String row = getSession().getLine(range.getStart().getRow());
         return row.substring(0, col).trim().length() != 0;
      }
   }

   private class AnchoredSelectionImpl implements AnchoredSelection
   {
      private AnchoredSelectionImpl(Anchor start, Anchor end)
      {
         start_ = start;
         end_ = end;
      }

      public String getValue()
      {
         return getSession().getTextRange(getRange());
      }

      public void apply()
      {
         getSession().getSelection().setSelectionRange(
               getRange());
      }

      private Range getRange()
      {
         return Range.fromPoints(start_.getPosition(), end_.getPosition());
      }

      public void detach()
      {
         start_.detach();
         end_.detach();
      }

      private final Anchor start_;
      private final Anchor end_;
   }

   public static void preload()
   {
      load(null);
   }

   public static void load(final Command command)
   {
      aceLoader_.addCallback(new Callback()
      {
         public void onLoaded()
         {
            aceSupportLoader_.addCallback(new Callback()
            {
               public void onLoaded()
               {
                  if (command != null)
                     command.execute();
               }
            });
         }
      });
   }

   @Inject
   public AceEditor()
   {
      widget_ = new AceEditorWidget();
      completionManager_ = new NullCompletionManager();
      RStudioGinjector.INSTANCE.injectMembers(this);

      widget_.addValueChangeHandler(new ValueChangeHandler<Void>()
      {
         public void onValueChange(ValueChangeEvent<Void> evt)
         {
            ValueChangeEvent.fire(AceEditor.this, null);
         }
      });

      addCapturingKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            int mod = KeyboardShortcut.getModifierValue(event.getNativeEvent());
            if (mod == KeyboardShortcut.CTRL)
            {
               switch (event.getNativeKeyCode())
               {
                  case 'U':
                     event.preventDefault() ;
                     InputEditorUtil.yankBeforeCursor(AceEditor.this, true);
                     break;
                  case 'K':
                     event.preventDefault();
                     InputEditorUtil.yankAfterCursor(AceEditor.this, true);
                     break;
                  case 'Y':
                     event.preventDefault();
                     Position start = getSelectionStart();
                     InputEditorUtil.pasteYanked(AceEditor.this);
                     indentPastedRange(Range.fromPoints(start,
                                                        getSelectionEnd()));
                     break;
               }
            }

         }
      });

      addPasteHandler(new PasteEvent.Handler()
      {
         @Override
         public void onPaste(PasteEvent event)
         {
            final Position start = getSelectionStart();

            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  Range range = Range.fromPoints(start, getSelectionEnd());
                  indentPastedRange(range);
               }
            });
         }
      });
   }

   private void indentPastedRange(Range range)
   {
      String firstLinePrefix = getSession().getTextRange(
            Range.fromPoints(Position.create(range.getStart().getRow(), 0),
                             range.getStart()));

      if (firstLinePrefix.trim().length() != 0)
      {
         Position newStart = Position.create(range.getStart().getRow() + 1, 0);
         if (newStart.compareTo(range.getEnd()) >= 0)
            return;

         range = Range.fromPoints(newStart, range.getEnd());
      }

      getSession().reindent(range);
   }

   @Inject
   void initialize(CodeToolsServerOperations server)
   {
      server_ = server;
   }

   public void setFileType(TextFileType fileType)
   {
      setFileType(fileType, false);
   }

   public void setFileType(TextFileType fileType, boolean suppressCompletion)
   {
      fileType_ = fileType;
      updateLanguage(suppressCompletion);
   }
   
   public void setFileType(TextFileType fileType, 
                           CompletionManager completionManager)
   {
      fileType_ = fileType;
      updateLanguage(completionManager);
   }
   
   private void updateLanguage(boolean suppressCompletion)
   {
      if (fileType_ == null)
         return;

      CompletionManager completionManager = null;
      if (!suppressCompletion && fileType_.getEditorLanguage().useRCompletion())
      {
         completionManager = new RCompletionManager(this,
                                                     this,
                                                     new CompletionPopupPanel(),
                                                     server_,
                                                     new Filter());
      }
      else
         completionManager = new NullCompletionManager();
      
      updateLanguage(completionManager);
   }
   
   private void updateLanguage(CompletionManager completionManager)
   {
      if (fileType_ == null)
         return;
      
      completionManager_ = completionManager;
      
      widget_.getEditor().setKeyboardHandler(
            new AceCompletionAdapter(completionManager_).getKeyboardHandler());

      getSession().setEditorMode(
            fileType_.getEditorLanguage().getParserName(),
            Desktop.isDesktop() && Desktop.getFrame().suppressSyntaxHighlighting());
      getSession().setUseWrapMode(fileType_.getWordWrap());
      
   }

   public String getCode()
   {
      return getSession().getValue();
   }

   public void setCode(String code, boolean preserveCursorPosition)
   {
      // Filter out Escape characters that might have snuck in from an old
      // bug in 0.95. We can choose to remove this when 0.95 ships, hopefully
      // any documents that would be affected by this will be gone by then.
      code = code.replaceAll("\u001B", "");

      final AceEditorNative ed = widget_.getEditor();

      if (preserveCursorPosition)
      {
         final Position cursorPos;
         final int scrollTop, scrollLeft;

         cursorPos = ed.getSession().getSelection().getCursor();
         scrollTop = ed.getRenderer().getScrollTop();
         scrollLeft = ed.getRenderer().getScrollLeft();

         // Setting the value directly on the document prevents undo/redo
         // stack from being blown away
         widget_.getEditor().getSession().getDocument().setValue(code);

         ed.getSession().getSelection().moveCursorTo(cursorPos.getRow(),
                                                     cursorPos.getColumn(),
                                                     false);
         ed.getRenderer().scrollToY(scrollTop);
         ed.getRenderer().scrollToX(scrollLeft);
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               ed.getRenderer().scrollToY(scrollTop);
               ed.getRenderer().scrollToX(scrollLeft);
            }
         });
      }
      else
      {
         ed.getSession().setValue(code);
         ed.getSession().getSelection().moveCursorTo(0, 0, false);
      }
   }
   
   public int getScrollLeft()
   {
      return widget_.getEditor().getRenderer().getScrollLeft();
   }

   public void scrollToX(int x)
   {
      widget_.getEditor().getRenderer().scrollToX(x);
   }
   
   public int getScrollTop()
   {
      return widget_.getEditor().getRenderer().getScrollTop();
   }
   
   public void scrollToY(int y)
   {
      widget_.getEditor().getRenderer().scrollToY(y);
   }
   
   
   public void insertCode(String code, boolean blockMode)
   {
      // TODO: implement block mode
      getSession().replace(
            getSession().getSelection().getRange(), code);
   }

   public String getCode(Position start, Position end)
   {
      return getSession().getTextRange(Range.fromPoints(start, end));
   }

   public void focus()
   {
      widget_.getEditor().focus();
   }
   
   public void goToFunctionDefinition()
   {
      completionManager_.goToFunctionDefinition();
   }

   class PrintIFrame extends DynamicIFrame
   {
      public PrintIFrame(String code, double fontSize)
      {
         code_ = code;
         fontSize_ = fontSize;

         getElement().getStyle().setPosition(com.google.gwt.dom.client.Style.Position.ABSOLUTE);
         getElement().getStyle().setLeft(-5000, Unit.PX);
      }

      @Override
      protected void onFrameLoaded()
      {
         Document doc = getDocument();
         PreElement pre = doc.createPreElement();
         pre.setInnerText(code_);
         pre.getStyle().setProperty("whiteSpace", "pre-wrap");
         pre.getStyle().setFontSize(fontSize_, Unit.PT);
         doc.getBody().appendChild(pre);

         getWindow().print();

         // Bug 1224: ace: print from source causes inability to reconnect
         // This was caused by the iframe being removed from the document too
         // quickly after the print job was sent. As a result, attempting to
         // navigate away from the page at any point afterwards would result
         // in the error "Document cannot change while printing or in Print
         // Preview". The only thing you could do is close the browser tab.
         // By inserting a 5-minute delay hopefully Firefox would be done with
         // whatever print related operations are important.
         Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
         {
            public boolean execute()
            {
               PrintIFrame.this.removeFromParent();
               return false;
            }
         }, 1000 * 60 * 5);
      }

      private final String code_;
      private final double fontSize_;
   }

   public void print()
   {
      PrintIFrame printIFrame = new PrintIFrame(
            getCode(),
            RStudioGinjector.INSTANCE.getUIPrefs().fontSize().getValue());
      RootPanel.get().add(printIFrame);
   }

   public String getText()
   {
      return getSession().getLine(
            getSession().getSelection().getCursor().getRow());
   }

   public void setText(String string)
   {
      setCode(string, false);
      getSession().getSelection().moveCursorFileEnd();
   }

   public boolean hasSelection()
   {
      return true;
   }

   public InputEditorSelection getSelection()
   {
      Range selection = getSession().getSelection().getRange();
      return new InputEditorSelection(
            new AceInputEditorPosition(getSession(), selection.getStart()),
            new AceInputEditorPosition(getSession(), selection.getEnd()));

   }

   public String getSelectionValue()
   {
      return getSession().getTextRange(
            getSession().getSelection().getRange());
   }

   public Position getSelectionStart()
   {
      return getSession().getSelection().getRange().getStart();
   }

   public Position getSelectionEnd()
   {
      return getSession().getSelection().getRange().getEnd();
   }

   public int getLength(int row)
   {
      return getSession().getDocument().getLine(row).length();
   }

   public int getRowCount()
   {
      return getSession().getDocument().getLength();
   }

   public void setSelection(InputEditorSelection selection)
   {
      AceInputEditorPosition start = (AceInputEditorPosition)selection.getStart();
      AceInputEditorPosition end = (AceInputEditorPosition)selection.getEnd();
      getSession().getSelection().setSelectionRange(Range.fromPoints(
            start.getValue(), end.getValue()));
   }

   public Rectangle getCursorBounds()
   {
      Range range = getSession().getSelection().getRange();
      Renderer renderer = widget_.getEditor().getRenderer();
      ScreenCoordinates start = renderer.textToScreenCoordinates(
                  range.getStart().getRow(),
                  range.getStart().getColumn());
      ScreenCoordinates end = renderer.textToScreenCoordinates(
                  range.getEnd().getRow(),
                  range.getEnd().getColumn());
      // TODO: Use actual width and height
      return new Rectangle(start.getPageX(),
                           start.getPageY(),
                           end.getPageX() - start.getPageX(),
                           renderer.getLineHeight());
   }

   public Rectangle getPositionBounds(InputEditorPosition position)
   {
      Renderer renderer = widget_.getEditor().getRenderer();

      Position pos = ((AceInputEditorPosition) position).getValue();

      ScreenCoordinates start = renderer.textToScreenCoordinates(
            pos.getRow(),
            pos.getColumn());

      return new Rectangle(start.getPageX(), start.getPageY(),
                           renderer.getCharacterWidth(),
                           (int) (renderer.getLineHeight() * 0.8));
   }

   public Rectangle getBounds()
   {
      return new Rectangle(
            widget_.getAbsoluteLeft(),
            widget_.getAbsoluteTop(),
            widget_.getOffsetWidth(),
            widget_.getOffsetHeight());
   }

   public void setFocus(boolean focused)
   {
      if (focused)
         widget_.getEditor().focus();
      else
         widget_.getEditor().blur();
   }

   public String replaceSelection(String value, boolean collapseSelection)
   {
      Selection selection = getSession().getSelection();
      String oldValue = getSession().getTextRange(selection.getRange());

      replaceSelection(value);

      if (collapseSelection)
      {
         collapseSelection(false);
      }

      return oldValue;
   }

   public boolean isSelectionCollapsed()
   {
      return getSession().getSelection().isEmpty();
   }

   public void clear()
   {
      setCode("", false);
   }

   public void collapseSelection(boolean collapseToStart)
   {
      Selection selection = getSession().getSelection();
      Range rng = selection.getRange();
      Position pos = collapseToStart ? rng.getStart() : rng.getEnd();
      selection.setSelectionRange(Range.fromPoints(pos, pos));
   }

   public InputEditorSelection getStart()
   {
      return new InputEditorSelection(
            new AceInputEditorPosition(getSession(), Position.create(0, 0)));
   }

   public InputEditorSelection getEnd()
   {
      EditSession session = getSession();
      int rows = session.getLength();
      Position end = Position.create(rows, session.getLine(rows).length());
      return new InputEditorSelection(new AceInputEditorPosition(session, end));
   }

   public String getCurrentLine()
   {
      int row = getSession().getSelection().getRange().getStart().getRow();
      return getSession().getLine(row);
   }

   public int getCurrentLineNum()
   {
      Position pos = getCursorPosition();
      return getSession().documentToScreenRow(pos);
   }

   public int getCurrentLineCount()
   {
      return getSession().getScreenLength();
   }

   public void replaceCode(String code)
   {
      int endRow, endCol;

      endRow = getSession().getLength() - 1;
      if (endRow < 0)
      {
         endRow = 0;
         endCol = 0;
      }
      else
      {
         endCol = getSession().getLine(endRow).length();
      }

      Range range = Range.fromPoints(Position.create(0, 0),
                                     Position.create(endRow, endCol));
      getSession().replace(range, code);
   }

   public void replaceSelection(String code)
   {
      Range selRange = getSession().getSelection().getRange();
      Position position = getSession().replace(selRange, code);
      Range range = Range.fromPoints(selRange.getStart(), position);
      getSession().getSelection().setSelectionRange(range);
   }

   public boolean moveSelectionToNextLine(boolean skipBlankLines)
   {
      int curRow = getSession().getSelection().getCursor().getRow();
      while (++curRow < getSession().getLength())
      {
         String line = getSession().getLine(curRow);
         Pattern pattern = Pattern.create("[^\\s]");
         Match match = pattern.match(line, 0);
         if (skipBlankLines && match == null)
            continue;
         int col =  (match != null) ? match.getIndex() : 0;
         getSession().getSelection().moveCursorTo(curRow, col, false);
         return true;
      }
      return false;
   }

   @Override
   public void reindent()
   {
      boolean emptySelection = getSelection().isEmpty();
      getSession().reindent(getSession().getSelection().getRange());
      if (emptySelection)
         moveSelectionToNextLine(false);
   }

   public ChangeTracker getChangeTracker()
   {
      return new EventBasedChangeTracker<Void>(this);
   }

   public AnchoredSelection createAnchoredSelection(Position startPos,
                                                    Position endPos)
   {
      Anchor start = Anchor.createAnchor(getSession().getDocument(),
                                         startPos.getRow(),
                                         startPos.getColumn());
      Anchor end = Anchor.createAnchor(getSession().getDocument(),
                                       endPos.getRow(),
                                       endPos.getColumn());
      return new AnchoredSelectionImpl(start, end);
   }

   public void fitSelectionToLines(boolean expand)
   {
      Range range = getSession().getSelection().getRange();
      Position start = range.getStart();
      Position newStart = start;

      if (start.getColumn() > 0)
      {
         if (expand)
         {
            newStart = Position.create(start.getRow(), 0);
         }
         else
         {
            String firstLine = getSession().getLine(start.getRow());
            if (firstLine.substring(0, start.getColumn()).trim().length() == 0)
               newStart = Position.create(start.getRow(), 0);
         }
      }

      Position end = range.getEnd();
      Position newEnd = end;
      if (expand)
      {
         int endRow = end.getRow();
         if (endRow == newStart.getRow() || end.getColumn() > 0)
         {
            // If selection ends at the start of a line, keep the selection
            // there--unless that means less than one line will be selected
            // in total.
            newEnd = Position.create(
                  endRow, getSession().getLine(endRow).length());
         }
      }
      else
      {
         while (newEnd.getRow() != newStart.getRow())
         {
            String line = getSession().getLine(newEnd.getRow());
            if (line.substring(0, newEnd.getColumn()).trim().length() != 0)
               break;

            int prevRow = newEnd.getRow() - 1;
            int len = getSession().getLine(prevRow).length();
            newEnd = Position.create(prevRow, len);
         }
      }

      getSession().getSelection().setSelectionRange(
            Range.fromPoints(newStart, newEnd));
   }

   public int getSelectionOffset(boolean start)
   {
      Range range = getSession().getSelection().getRange();
      if (start)
         return range.getStart().getColumn();
      else
         return range.getEnd().getColumn();
   }

   public void onActivate()
   {
      Scheduler.get().scheduleFinally(new RepeatingCommand()
      {
         public boolean execute()
         {
            widget_.onResize();
            widget_.onActivate();

            return false;
         }
      });
   }

   public void onVisibilityChanged(boolean visible)
   {
      if (visible)
         widget_.getEditor().getRenderer().updateFontSize();
   }

   public void setHighlightSelectedLine(boolean on)
   {
      widget_.getEditor().setHighlightActiveLine(on);
   }

   public void setHighlightSelectedWord(boolean on)
   {
      widget_.getEditor().setHighlightSelectedWord(on);
   }

   public void setShowLineNumbers(boolean on)
   {
      widget_.getEditor().getRenderer().setShowGutter(on);
   }

   public void setUseSoftTabs(boolean on)
   {
      getSession().setUseSoftTabs(on);
   }

   /**
    * Warning: This will be overridden whenever the file type is set
    */
   public void setUseWrapMode(boolean useWrapMode)
   {
      getSession().setUseWrapMode(useWrapMode);
   }

   public void setTabSize(int tabSize)
   {
      getSession().setTabSize(tabSize);
   }

   public void setShowPrintMargin(boolean on)
   {
      widget_.getEditor().getRenderer().setShowPrintMargin(on);
   }

   public void setPadding(int padding)
   {
      widget_.getEditor().getRenderer().setPadding(padding);
   }

   public void setPrintMarginColumn(int column)
   {
      widget_.getEditor().getRenderer().setPrintMarginColumn(column);
   }

   public void setReadOnly(boolean readOnly)
   {
      widget_.getEditor().setReadOnly(readOnly);
   }

   public HandlerRegistration addCursorChangedHandler(final CursorChangedHandler handler)
   {
      return widget_.addCursorChangedHandler(handler);
   }

   public FunctionStart getCurrentFunction()
   {
      return getSession().getMode().getCurrentFunction(getCursorPosition());
   }

   public Position getCursorPosition()
   {
      return getSession().getSelection().getCursor();
   }

   public void setCursorPosition(Position position)
   {
      getSession().getSelection().setSelectionRange(
            Range.fromPoints(position, position));
   }

   public void moveCursorNearTop()
   {
      int screenRow = getSession().documentToScreenRow(getCursorPosition());
      widget_.getEditor().scrollToRow(Math.max(0, screenRow - 4));
   }

   public JsArray<FunctionStart> getFunctionTree()
   {
      return getSession().getMode().getFunctionTree();
   }
   
   @Override
   public SourcePosition findFunctionPositionFromCursor(String functionName)
   {
      FunctionStart func = 
         getSession().getMode().findFunctionDefinitionFromUsage(
                                                      getCursorPosition(), 
                                                      functionName);
      if (func != null)
      {
         Position position = func.getPreamble();
         return SourcePosition.create(position.getRow(), position.getColumn());
      }
      else
      {
         return null;
      }
   }
   
   @Override 
   public void recordCurrentNavigationPosition()
   {
      fireRecordNavigationPosition(getCursorPosition());
   }
   
   @Override 
   public void navigateToPosition(SourcePosition position, 
                                  boolean recordCurrent)
   {
      if (recordCurrent)
         recordCurrentNavigationPosition();
      
      navigate(position, true);
   }
   
   @Override
   public void restorePosition(SourcePosition position)
   {
      navigate(position, false);
   }
   
   @Override 
   public boolean isAtSourceRow(SourcePosition position)
   {
      Position currPos = getCursorPosition();
      return currPos.getRow() == position.getRow();
   }
   
   private void navigate(SourcePosition srcPosition, boolean addToHistory)
   {  
      // set cursor to function line
      Position position = Position.create(srcPosition.getRow(), 
                                          srcPosition.getColumn());
      setCursorPosition(position);

      // skip whitespace if necessary
      if (srcPosition.getColumn() == 0)
      {
         int curRow = getSession().getSelection().getCursor().getRow();
         String line = getSession().getLine(curRow);
         int funStart = line.indexOf(line.trim());
         position = Position.create(curRow, funStart);
         setCursorPosition(position);
      }
      
      // scroll as necessary
      if (srcPosition.getScrollPosition() != -1)
         scrollToY(srcPosition.getScrollPosition());
      else
         moveCursorNearTop();
      
      // set focus
      focus();
      
      // add to navigation history if requested and our current mode
      // supports history navigation
      if (addToHistory)
         fireRecordNavigationPosition(position);
   }
   
   private void fireRecordNavigationPosition(Position pos)
   {
      SourcePosition srcPos = SourcePosition.create(pos.getRow(), 
                                                    pos.getColumn());
      fireEvent(new RecordNavigationPositionEvent(srcPos));
   }
   
   @Override
   public HandlerRegistration addRecordNavigationPositionHandler(
                                    RecordNavigationPositionHandler handler)
   {
      return handlers_.addHandler(RecordNavigationPositionEvent.TYPE, handler);
   }

   public void setFontSize(double size)
   {
      // No change needed--the AceEditorWidget uses the "normalSize" style
      // However, we do need to resize the gutter
      widget_.getEditor().getRenderer().updateFontSize();
      widget_.forceResize();
   }

   public HandlerRegistration addValueChangeHandler(
         ValueChangeHandler<Void> handler)
   {
      return handlers_.addHandler(ValueChangeEvent.getType(), handler);
   }

   public HandlerRegistration addCapturingKeyDownHandler(KeyDownHandler handler)
   {
      return widget_.addCapturingKeyDownHandler(handler);
   }

   public HandlerRegistration addCapturingKeyPressHandler(KeyPressHandler handler)
   {
      return widget_.addCapturingKeyPressHandler(handler);
   }

   public HandlerRegistration addCapturingKeyUpHandler(KeyUpHandler handler)
   {
      return widget_.addCapturingKeyUpHandler(handler);
   }

   public HandlerRegistration addUndoRedoHandler(UndoRedoHandler handler)
   {
      return widget_.addUndoRedoHandler(handler);
   }

   public HandlerRegistration addPasteHandler(PasteEvent.Handler handler)
   {
      return widget_.addPasteHandler(handler);
   }

   public JavaScriptObject getCleanStateToken()
   {
      return getSession().getUndoManager().peek();
   }

   public boolean checkCleanStateToken(JavaScriptObject token)
   {
      JavaScriptObject other = getSession().getUndoManager().peek();
      if (token == null ^ other == null)
         return false;
      return token == null || other.equals(token);
   }

   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }

   public Widget asWidget()
   {
      return widget_;
   }

   public EditSession getSession()
   {
      return widget_.getEditor().getSession();
   }

   public HandlerRegistration addBlurHandler(BlurHandler handler)
   {
      return widget_.addBlurHandler(handler);
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return widget_.addClickHandler(handler);
   }

   public HandlerRegistration addFocusHandler(FocusHandler handler)
   {
      return widget_.addFocusHandler(handler);
   }

   public Widget getWidget()
   {
      return widget_;
   }

   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return widget_.addKeyDownHandler(handler);
   }

   public HandlerRegistration addKeyPressHandler(KeyPressHandler handler)
   {
      return widget_.addKeyPressHandler(handler);
   }

   public void autoHeight()
   {
      widget_.autoHeight();
   }

   public void forceCursorChange()
   {
      widget_.forceCursorChange();
   }

   public void scrollToCursor(ScrollPanel scrollPanel,
                              int paddingVert,
                              int paddingHoriz)
   {
      DomUtils.ensureVisibleVert(
            scrollPanel.getElement(),
            widget_.getEditor().getRenderer().getCursorElement(),
            paddingVert);
      DomUtils.ensureVisibleHoriz(
            scrollPanel.getElement(),
            widget_.getEditor().getRenderer().getCursorElement(),
            paddingHoriz, paddingHoriz,
            false);
   }

   public void forceImmediateRender()
   {
      widget_.getEditor().getRenderer().forceImmediateRender();
   }

   public void setNewLineMode(NewLineMode mode)
   {
      getSession().setNewLineMode(mode.getType());
   }

   private final HandlerManager handlers_ = new HandlerManager(this);
   private final AceEditorWidget widget_;
   private CompletionManager completionManager_;
   private CodeToolsServerOperations server_;
   private TextFileType fileType_;

   private static final ExternalJavaScriptLoader aceLoader_ =
         new ExternalJavaScriptLoader(AceResources.INSTANCE.acejs().getUrl());
   private static final ExternalJavaScriptLoader aceSupportLoader_ =
         new ExternalJavaScriptLoader(AceResources.INSTANCE.acesupportjs().getUrl());
}
