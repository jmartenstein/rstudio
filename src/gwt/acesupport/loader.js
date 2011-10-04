/*
 * loader.js
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

      if (!String.prototype.trimRight) {
         var trimEndRegexp = /\s\s*$/;
         String.prototype.trimRight = function () {
            return String(this).replace(trimEndRegexp, '');
         };
      }

define("rstudio/loader", function(require, exports, module) {

var oop = require("pilot/oop");
var event = require("pilot/event");
var EventEmitter = require("pilot/event_emitter").EventEmitter;
var Editor = require("ace/editor").Editor;
var EditSession = require("ace/edit_session").EditSession;
var UndoManager = require("ace/undomanager").UndoManager;
var Range = require("ace/range").Range;

var RStudioEditor = function(renderer, session) {
   Editor.call(this, renderer, session);
};
oop.inherits(RStudioEditor, Editor);

(function() {
   this.removeLeft = function() {
      if (this.session.getMode().wrapRemoveLeft) {
         return this.session.getMode().wrapRemoveLeft(this, Editor.prototype.removeLeft);
      }
      else {
         return Editor.prototype.removeLeft.call(this);
      }
   };

   this.undo = function() {
      Editor.prototype.undo.call(this);
      this._dispatchEvent("undo");
   };

   this.redo = function() {
      Editor.prototype.redo.call(this);
      this._dispatchEvent("redo");
   };
}).call(RStudioEditor.prototype);


var RStudioEditSession = function(text, mode) {
   EditSession.call(this, text, mode);
};
oop.inherits(RStudioEditSession, EditSession);

(function() {
   this.insert = function(position, text) {
      if (this.getMode().wrapInsert) {
         return this.getMode().wrapInsert(this, EditSession.prototype.insert, position, text);
      }
      else {
         return EditSession.prototype.insert.call(this, position, text);
      }
   };
   this.reindent = function(range) {
      var mode = this.getMode();
      if (!mode.getNextLineIndent)
         return;
      var start = range.start.row;
      var end = range.end.row;
      for (var i = start; i <= end; i++) {
         // First line is always unindented
         if (i == 0) {
            this.applyIndent(i, "");
         }
         else {
            var state = this.getState(i-1);
            if (state == 'qstring' || state == 'qqstring')
               continue;
            var line = this.getLine(i-1);
            var newline = this.getLine(i);

            var shouldOutdent = mode.checkOutdent(state, " ", newline);

            var newIndent = mode.getNextLineIndent(state,
                                                   line,
                                                   this.getTabString(),
                                                   this.getTabSize(),
                                                   i-1);

            this.applyIndent(i, newIndent);

            if (shouldOutdent) {
               mode.autoOutdent(state, this, i);
            }
         }
      }
   };
   this.applyIndent = function(lineNum, indent) {
      var line = this.getLine(lineNum);
      var matchLen = line.match(/^\s*/g)[0].length;
      this.replace(new Range(lineNum, 0, lineNum, matchLen), indent);
   };
}).call(RStudioEditSession.prototype);


var RStudioUndoManager = function() {
   UndoManager.call(this);
};
oop.inherits(RStudioUndoManager, UndoManager);

(function() {
   this.peek = function() {
      return this.$undoStack.length ? this.$undoStack[this.$undoStack.length-1]
                                    : null;
   };
}).call(RStudioUndoManager.prototype);


function loadEditor(container) {
   var catalog = require("pilot/plugin_manager").catalog;
	var env = null;
	var loaded = catalog.registerPlugins(["pilot/index"]).isResolved();
    env = require("pilot/environment").create();
    loaded = catalog.startupPlugins({ env: env }).isResolved() && loaded;

	if (!loaded)
		throw new Error("Environment loading was not synchronous");

	var Renderer = require("ace/virtual_renderer").VirtualRenderer;

	var TextMode = require("ace/mode/text").Mode;
	var theme = {}; // prevent default textmate theme from loading

	env.editor = new RStudioEditor(new Renderer(container, theme), new RStudioEditSession(""));
	var session = env.editor.getSession();
	session.setMode(new TextMode());
	session.setUndoManager(new RStudioUndoManager());

	// We handle these commands ourselves.
	var canon = require("pilot/canon");
   function squelch(cmd) {
      canon.getCommand(cmd).exec = function() {};
   }
   squelch("findnext");
   squelch("findprevious");
   squelch("find");
   squelch("replace");
   squelch("togglecomment");
   squelch("gotoline");
   return env.editor;
}

exports.loadEditor = loadEditor;
});
