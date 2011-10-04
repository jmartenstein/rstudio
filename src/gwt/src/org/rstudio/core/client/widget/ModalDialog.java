/*
 * ModalDialog.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;


public abstract class ModalDialog<T> extends ModalDialogBase
{
   public ModalDialog(String caption, 
                      final OperationWithInput<T> operation)
   {
      super();
    
      ThemedButton okButton = new ThemedButton("OK", new ClickHandler() {
         public void onClick(ClickEvent event) {
            T input = collectInput();
            if (validate(input))
            {
               closeDialog();
               if (operation != null)
                  operation.execute(input);
               onSuccess();
            }      
         }
      });
      
      commonInit(caption, okButton, null);
   }

   protected void onSuccess()
   {
   }


   public ModalDialog(String caption,
                      final ProgressOperationWithInput<T> operation)
   {
      this(caption, operation, null);
   }

   public ModalDialog(String caption, 
                      final ProgressOperationWithInput<T> operation,
                      Operation cancelOperation)
   {
      super();

      final ProgressIndicator progressIndicator = addProgressIndicator();
      
      ThemedButton okButton = new ThemedButton("OK", new ClickHandler() {
         public void onClick(ClickEvent event) {
            T input = collectInput();
            if (validate(input))
            {
               operation.execute(input, progressIndicator);
               onSuccess();
            }      
         }
      });
      
      commonInit(caption, okButton, cancelOperation);
   }
   
   private void commonInit(String caption,
                           ThemedButton okButton,
                           final Operation cancelOperation)
   {
      setText(caption);
      addOkButton(okButton);
      ThemedButton cancelButton = addCancelButton();
      if (cancelOperation != null)
      {
         cancelButton.addClickHandler(new ClickHandler()
         {
            @Override
            public void onClick(ClickEvent event)
            {
               cancelOperation.execute();
            }
         });
      }
   }
   
   protected abstract T collectInput();
   
   protected abstract boolean validate(T input);
  
}
