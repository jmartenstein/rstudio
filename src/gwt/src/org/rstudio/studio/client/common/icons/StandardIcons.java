/*
 * StandardIcons.java
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
package org.rstudio.studio.client.common.icons;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface StandardIcons extends ClientBundle
{
   public static final StandardIcons INSTANCE = GWT.create(StandardIcons.class);
   ImageResource stock_new();
   ImageResource go_up();
   ImageResource right_arrow();
   ImageResource click_feedback();
   ImageResource more_actions();
   ImageResource import_dataset();
   ImageResource empty_command();
}
