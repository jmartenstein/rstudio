/*
 * SessionProjectContext.cpp
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

#include <session/projects/SessionProjects.hpp>

#include <map>

#include <boost/format.hpp>

#include <core/FileSerializer.hpp>
#include <core/r_util/RProjectFile.hpp>

#include <core/system/FileMonitor.hpp>

#include <r/RExec.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace projects {

namespace {

bool canWriteToProjectDir(const FilePath& projectDirPath)
{
   FilePath testFile = projectDirPath.complete(core::system::generateUuid());
   Error error = core::writeStringToFile(testFile, "test");
   if (error)
   {
      return false;
   }
   else
   {
      error = testFile.removeIfExists();
      if (error)
         LOG_ERROR(error);

      return true;
   }
}

}  // anonymous namespace


Error computeScratchPath(const FilePath& projectFile, FilePath* pScratchPath)
{
   // ensure project user dir
   FilePath projectUserDir = projectFile.parent().complete(".Rproj.user");
   if (!projectUserDir.exists())
   {
      // create
      Error error = projectUserDir.ensureDirectory();
      if (error)
         return error;

      // mark hidden if we are on win32
#ifdef _WIN32
      error = core::system::makeFileHidden(projectUserDir);
      if (error)
         return error;
#endif
   }

   // create user subdirectory if we have a username
   std::string username = core::system::username();
   if (!username.empty())
   {
      projectUserDir = projectUserDir.complete(username);
      Error error = projectUserDir.ensureDirectory();
      if (error)
         return error;
   }

   // now add context id to form scratch path
   FilePath scratchPath = projectUserDir.complete(userSettings().contextId());
   Error error = scratchPath.ensureDirectory();
   if (error)
      return error;

   // return the path
   *pScratchPath = scratchPath;
   return Success();
}

// NOTE: this function is called very early in the process lifetime (from
// session::projects::startup) so can only have limited dependencies.
// specifically, it can rely on userSettings() and persistentState() being
// available, but cannot definitely NOT rely on calling into R. For
// initialization related tasks that need to run after R is available use
// the implementation of the initialize method (below)
Error ProjectContext::startup(const FilePath& projectFile,
                              std::string* pUserErrMsg)
{
   // test for project file existence
   if (!projectFile.exists())
   {
      *pUserErrMsg = "the project file does not exist";
      return pathNotFoundError(projectFile.absolutePath(), ERROR_LOCATION);
   }

   // test for writeabilty of parent
   if (!canWriteToProjectDir(projectFile.parent()))
   {
      *pUserErrMsg = "the project directory is not writeable";
      return systemError(boost::system::errc::permission_denied,
                         ERROR_LOCATION);
   }

   // calculate project scratch path
   FilePath scratchPath;
   Error error = computeScratchPath(projectFile, &scratchPath);
   if (error)
   {
      *pUserErrMsg = "unable to initialize project - " + error.summary();
      return error;
   }

   // read project file config
   bool providedDefaults;
   r_util::RProjectConfig config;
   error = r_util::readProjectFile(projectFile,
                                   defaultConfig(),
                                   &config,
                                   &providedDefaults,
                                   pUserErrMsg);
   if (error)
      return error;

   // if we provided defaults then re-write the project file
   // with the defaults
   if (providedDefaults)
   {
      error = r_util::writeProjectFile(projectFile, config);
      if (error)
         LOG_ERROR(error);
   }

   // initialize members
   file_ = projectFile;
   directory_ = file_.parent();
   scratchPath_ = scratchPath;
   config_ = config;

   // assume true so that the initial files pane listing doesn't register
   // a duplicate monitor. if it turns out to be false then this can be
   // repaired by a single refresh of the files pane
   hasFileMonitor_ = config_.enableCodeIndexing;

   // return success
   return Success();

}

Error ProjectContext::initialize()
{
   if (hasProject())
   {
      // compute the default encoding
      Error error = r::exec::RFunction(
                        ".rs.validateAndNormalizeEncoding",
                        config().encoding).call(&defaultEncoding_);
      if (error)
         return error;

      // if the default encoding is empty then change to UTF-8 and
      // and enque a warning
      if (defaultEncoding_.empty())
      {
         // fallback
         defaultEncoding_ = "UTF-8";

         // enque a warning
         json::Object msgJson;
         msgJson["severe"] = false;
         boost::format fmt(
           "Project text encoding '%1%' not available (using UTF-8). "
           "You can specify an alternate text encoding via Project Options.");
         msgJson["message"] = boost::str(fmt % config().encoding);
         ClientEvent event(client_events::kShowWarningBar, msgJson);
         module_context::enqueClientEvent(event);
      }

      // subscribe to deferred init (for initializing our file monitor)
      if (config().enableCodeIndexing)
      {
         module_context::events().onDeferredInit.connect(
                          boost::bind(&ProjectContext::onDeferredInit, this));
      }
   }

   return Success();
}


void ProjectContext::onDeferredInit()
{
   // kickoff file monitoring for this directory
   using namespace boost;
   core::system::file_monitor::Callbacks cb;
   cb.onRegistered = bind(&ProjectContext::fileMonitorRegistered,
                          this, _1, _2);
   cb.onRegistrationError = bind(&ProjectContext::fileMonitorTermination,
                                 this, _1);
   cb.onMonitoringError = bind(&ProjectContext::fileMonitorTermination,
                               this, _1);
   cb.onFilesChanged = bind(&ProjectContext::fileMonitorFilesChanged,
                            this, _1);
   cb.onUnregistered = bind(&ProjectContext::fileMonitorTermination,
                            this, Success());
   core::system::file_monitor::registerMonitor(
                                         directory(),
                                         true,
                                         module_context::fileListingFilter,
                                         cb);
}

void ProjectContext::fileMonitorRegistered(
                              core::system::file_monitor::Handle handle,
                              const tree<core::FileInfo>& files)
{
   // update state
   hasFileMonitor_ = true;

   // notify subscribers
   onMonitoringEnabled_(files);
}

void ProjectContext::fileMonitorFilesChanged(
                   const std::vector<core::system::FileChangeEvent>& events)
{
   // notify client (gwt)
   module_context::enqueFileChangedEvents(directory(), events);

   // notify subscribers
   onFilesChanged_(events);
}

void ProjectContext::fileMonitorTermination(const Error& error)
{
   // always log error
   if (error)
      LOG_ERROR(error);

   // if we have a file monitor then unwind it
   if (hasFileMonitor_)
   {
      // do this only once
      hasFileMonitor_ = false;

      // notify end-user if this was an error condition
      if (error)
      {
         // base error message
         boost::system::error_code ec = error.code();
         std::string dir = module_context::createAliasedPath(directory());
         boost::format fmt(
          "\nWarning message:\n"
          "File monitoring failed for project at \"%1%\"\n"
          "Error %2% (%3%)");
         std::string msg = boost::str(fmt % dir % ec.value() % ec.message());

         // enumeration of affected features
         if (!monitorSubscribers_.empty())
            msg.append("\nFeatures disabled:");
         for(std::size_t i=0; i<monitorSubscribers_.size(); ++i)
         {
            if (i > 0)
               msg.append(",");
            msg.append(" ");
            msg.append(monitorSubscribers_[i]);
         }

         // write to console
         module_context::consoleWriteError(msg);
      }

      // notify subscribers
      onMonitoringDisabled_();
   }
}

bool ProjectContext::isMonitoringDirectory(const FilePath& dir) const
{
   return hasProject() && hasFileMonitor() && dir.isWithin(directory());
}

void ProjectContext::subscribeToFileMonitor(const std::string& featureName,
                                            const FileMonitorCallbacks& cb)
{
   if (!featureName.empty())
      monitorSubscribers_.push_back(featureName);

   if (cb.onMonitoringEnabled)
      onMonitoringEnabled_.connect(cb.onMonitoringEnabled);
   if (cb.onFilesChanged)
      onFilesChanged_.connect(cb.onFilesChanged);
   if (cb.onMonitoringDisabled)
      onMonitoringDisabled_.connect(cb.onMonitoringDisabled);
}

std::string ProjectContext::defaultEncoding() const
{
   return defaultEncoding_;
}

json::Object ProjectContext::uiPrefs() const
{
   json::Object uiPrefs;
   uiPrefs["use_spaces_for_tab"] = config_.useSpacesForTab;
   uiPrefs["num_spaces_for_tab"] = config_.numSpacesForTab;
   uiPrefs["default_encoding"] = defaultEncoding();
   return uiPrefs;
}


r_util::RProjectConfig ProjectContext::defaultConfig()
{
   // setup defaults for project file
   r_util::RProjectConfig defaultConfig;
   defaultConfig.useSpacesForTab = userSettings().useSpacesForTab();
   defaultConfig.numSpacesForTab = userSettings().numSpacesForTab();
   if (!userSettings().defaultEncoding().empty())
      defaultConfig.encoding = userSettings().defaultEncoding();
   else
      defaultConfig.encoding = "UTF-8";
   return defaultConfig;
}

} // namespace projects
} // namesapce session

