/*
 * ResultCallback.java
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
package org.rstudio.core.client;

/**
 * Provides a generic interface for handling success and/or failure of an
 * operation, especially an asynchronous one.
 */
public abstract class ResultCallback<TSuccess, TFailure>
{
   public void onSuccess(TSuccess result) {}
   public void onFailure(TFailure info) {}
   public void onCancelled() {}
}
