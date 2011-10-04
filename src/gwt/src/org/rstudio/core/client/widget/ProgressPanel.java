/*
 * ProgressPanel.java
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

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import org.rstudio.core.client.widget.images.ProgressImages;

public class ProgressPanel extends Composite
{
   public ProgressPanel()
   {
      this(ProgressImages.createLarge());
   }
   
   public ProgressPanel(Image progressImage)
   { 
      progressImage_ = progressImage;
      HorizontalCenterPanel progressPanel = new HorizontalCenterPanel(
                                                            progressImage_, 
                                                            100);
      progressImage_.setVisible(false);
      progressPanel.setSize("100%", "100%");
      initWidget(progressPanel);
   }
   
   public void beginProgressOperation(int delayMs)
   {
      clearTimer();
      progressImage_.setVisible(false);

      timer_ = new Timer() {
         public void run() {
            if (timer_ != this)
               return; // This should never happen, but, just in case

            progressImage_.setVisible(true);
         }
      };
      timer_.schedule(delayMs);
   }

   public void endProgressOperation()
   {
      clearTimer();
      progressImage_.setVisible(false);
   }

   private void clearTimer()
   {
      if (timer_ != null)
      {
         timer_.cancel();
         timer_ = null;
      }
   }

   private final Image progressImage_ ;
   private Timer timer_;
}
