/*
 * Assert.cpp
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


#include <core/Error.hpp>
#include <core/Log.hpp>

#ifdef _WIN32
#include <windows.h>
#else
#include <signal.h>
#endif

using namespace core;

namespace boost
{
void assertion_failed(char const * expr,
                      char const * function,
                      char const * file,
                      long line)
{
   // derive location
   ErrorLocation location(function, file, line);

   // always log the failure
   std::string msg = "ASSERTION FAILED: " + std::string(expr);
   core::log::logWarningMessage(msg, location);

#ifndef NDEBUG
#ifdef _WIN32
   DebugBreak();
#else
   ::raise(SIGTRAP);
#endif
#endif

}

} // namespace boost




