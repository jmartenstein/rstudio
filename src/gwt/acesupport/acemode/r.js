/*
 * r.js
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * The Initial Developer of the Original Code is
 * Ajax.org B.V.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
define("mode/r", function(require, exports, module)
{
   var Editor = require("ace/editor").Editor;
   var EditSession = require("ace/edit_session").EditSession;
   var Range = require("ace/range").Range;
   var oop = require("pilot/oop");
   var TextMode = require("ace/mode/text").Mode;
   var Tokenizer = require("ace/tokenizer").Tokenizer;
   var TextHighlightRules = require("ace/mode/text_highlight_rules")
         .TextHighlightRules;
   var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;
   var RCodeModel = require("mode/r_code_model").RCodeModel;
   var MatchingBraceOutdent = require("ace/mode/matching_brace_outdent").MatchingBraceOutdent;
   var AutoBraceInsert = require("mode/auto_brace_insert").AutoBraceInsert;

   var Mode = function(suppressHighlighting, doc)
   {
      if (suppressHighlighting)
         this.$tokenizer = new Tokenizer(new TextHighlightRules().getRules());
      else
         this.$tokenizer = new Tokenizer(new RHighlightRules().getRules());
      this.$outdent = new MatchingBraceOutdent();

      this.$rCodeModel = new RCodeModel(doc, this.$tokenizer, null);
   };
   oop.inherits(Mode, TextMode);

   (function()
   {
      this.getNextLineIndent = function(state, line, tab, tabSize, row)
      {
         return this.$rCodeModel.getNextLineIndent(row, line, state, tab, tabSize);
      };

      this.getCurrentFunction = function(position)
      {
         return this.$rCodeModel.getCurrentFunction(position);
      };

      this.getFunctionTree = function()
      {
         return this.$rCodeModel.getFunctionTree();
      };

      this.findFunctionDefinitionFromUsage = function(usagePos, functionName)
      {
         return this.$rCodeModel.findFunctionDefinitionFromUsage(usagePos,
                                                                 functionName);
      };

      this.checkOutdent = function(state, line, input) {
         if (! /^\s+$/.test(line))
            return false;

         return /^\s*[\{\}]/.test(input);
      };

      this.getIndentForOpenBrace = function(openBracePos)
      {
         return this.$rCodeModel.getIndentForOpenBrace(openBracePos);
      };

      this.autoOutdent = function(state, doc, row) {
         if (row == 0)
            return 0;

         var line = doc.getLine(row);

         var match = line.match(/^(\s*\})/);
         if (match)
         {
            var column = match[1].length;
            var openBracePos = doc.findMatchingBracket({row: row, column: column});

            if (!openBracePos || openBracePos.row == row) return 0;

            var indent = this.$rCodeModel.getIndentForOpenBrace(openBracePos);
            doc.replace(new Range(row, 0, row, column-1), indent);
         }

         match = line.match(/^(\s*\{)/);
         if (match)
         {
            var column = match[1].length;
            var indent = this.$rCodeModel.getBraceIndent(row-1);
            doc.replace(new Range(row, 0, row, column-1), indent);
         }
      };

      this.$getIndent = function(line) {
         var match = line.match(/^(\s+)/);
         if (match) {
            return match[1];
         }

         return "";
      };
   }).call(Mode.prototype);
   exports.Mode = Mode;
});
