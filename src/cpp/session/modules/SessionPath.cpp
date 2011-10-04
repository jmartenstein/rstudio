/*
 * SessionPath.cpp
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

#include "SessionPath.hpp"

#include <string>
#include <vector>

#include <boost/regex.hpp>
#include <boost/bind.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {
namespace modules { 
namespace path {

namespace {

Error readPathsFromFile(const FilePath& filePath,
                        std::vector<std::string>* pPaths)
{
   std::vector<std::string> paths;
   Error error = core::readStringVectorFromFile(filePath, &paths);
   if (error)
   {
      error.addProperty("path-source", filePath.absolutePath());
      return error;
   }

   std::copy(paths.begin(), paths.end(), std::back_inserter(*pPaths));

   return Success();
}

void safeReadPathsFromFile(const FilePath& filePath,
                           std::vector<std::string>* pPaths)
{
   Error error = readPathsFromFile(filePath, pPaths);
   if (error)
      LOG_ERROR(error);
}

void addToPathIfNecessary(const std::string& entry, std::string* pPath)
{
   if (!regex_search(*pPath, boost::regex("(^|:)" + entry + "/?($|:)")))
   {
      if (!pPath->empty())
         pPath->push_back(':');
      pPath->append(entry);
   }
}

} // anonymous namespace


Error initialize()
{
#ifdef __APPLE__
   if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      // do we need to add /opt/local/bin?
      std::string probePath = core::system::getenv("PATH");
      FilePath optLocalBinPath("/opt/local/bin");
      if (!regex_search(probePath, boost::regex("(^|:)/opt/local/bin/?($|:)"))
          && optLocalBinPath.exists())
      {
         // add opt/local/bin to path (prepend so we find macports texi2dvi
         // first if it is installed there)
         core::system::addToSystemPath(optLocalBinPath, true);
      }

      // read /etc/paths
      std::vector<std::string> paths;
      safeReadPathsFromFile(FilePath("/etc/paths"), &paths);

      // read /etc/paths.d/* (once again failure is not fatal as we
      // can fall back to the previous setting)
      FilePath pathsD("/etc/paths.d");
      if (pathsD.exists())
      {
         // enumerate the children
         std::vector<FilePath> pathsDChildren;
         Error error = pathsD.children(&pathsDChildren);
         if (error)
            LOG_ERROR(error);

         // collect their paths
         std::for_each(pathsDChildren.begin(),
                       pathsDChildren.end(),
                       boost::bind(safeReadPathsFromFile, _1, &paths));

      }

      // build the PATH
      std::string path = core::system::getenv("PATH");
      std::for_each(paths.begin(),
                    paths.end(),
                    boost::bind(addToPathIfNecessary, _1, &path));

      // do we need to add /usr/texbin (sometimes texlive doesn't get this
      // written into /etc/paths.d)
      FilePath texbinPath("/usr/texbin");
      if (texbinPath.exists())
         addToPathIfNecessary(texbinPath.absolutePath(), &path);

      // set the path
      core::system::setenv("PATH", path);
   }
#endif

   return Success();

}
   
   
} // namespace path
} // namespace modules
} // namesapce session

