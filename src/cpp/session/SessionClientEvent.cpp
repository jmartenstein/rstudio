/*
 * SessionClientEvent.cpp
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

#include <session/SessionClientEvent.hpp>

#include <boost/lexical_cast.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>

using namespace core ;

namespace session {

namespace client_events {
   
const int kBusy = 1;  
const int kConsolePrompt = 2;
const int kConsoleWriteOutput = 3;
const int kConsoleWriteError = 4;
const int kShowErrorMessage = 5;
const int kShowHelp = 6;
const int kBrowseUrl = 7;
const int kWorkspaceRefresh = 8;
const int kWorkspaceAssign = 9;
const int kWorkspaceRemove = 10;   
const int kShowEditor = 11;
const int kChooseFile = 13;
const int kAbendWarning = 14;
const int kQuit = 15;
const int kSuicide = 16;
const int kFileChanged = 17;
const int kWorkingDirChanged = 18;
const int kPlotsStateChanged = 19;
const int kViewData = 20;
const int kPackageStatusChanged = 21;
const int kInstalledPackagesChanged = 22;
const int kLocator = 23;
const int kConsoleResetHistory = 25;
const int kSessionSerialization = 26;
const int kHistoryEntriesAdded = 27;
const int kQuotaStatus = 29;
const int kOAuthApproval = 30;
const int kPublishPdf = 31;
const int kFileEdit = 32;
const int kShowContent = 33;
const int kShowData = 34;
const int kAsyncCompletion = 35;
const int kSaveActionChanged = 36;
const int kConsoleWritePrompt = 37;
const int kConsoleWriteInput = 38;
const int kShowWarningBar = 39;
const int kOpenProjectError = 40;
const int kVcsRefresh = 41;
const int kAskPass = 42;
const int kConsoleProcessOutput = 43;
const int kConsoleProcessExit = 44;

}   

void ClientEvent::init(int type, const json::Value& data)
{
   type_ = type;
   data_ = data;
   id_ = core::system::generateUuid();
}
   
void ClientEvent::asJsonObject(int id, json::Object* pObject) const
{
   json::Object& object = *pObject;
   object["id"] = id;
   object["type"] = typeName(); 
   object["data"] = data();
}
   
std::string ClientEvent::typeName() const 
{
   switch(type_)
   {
      case client_events::kBusy:
         return "busy";  
      case client_events::kConsolePrompt:
         return "console_prompt";
      case client_events::kConsoleWriteOutput:
         return "console_output";
      case client_events::kConsoleWriteError: 
         return "console_error";
      case client_events::kShowErrorMessage: 
         return "show_error_message";
      case client_events::kShowHelp: 
         return "show_help";
      case client_events::kBrowseUrl: 
         return "browse_url";
      case client_events::kWorkspaceRefresh:
         return "workspace_refresh";
      case client_events::kWorkspaceAssign: 
         return "workspace_assign";
      case client_events::kWorkspaceRemove: 
         return "workspace_remove";   
      case client_events::kShowEditor: 
         return "show_editor";
      case client_events::kChooseFile: 
         return "choose_file";
      case client_events::kAbendWarning:
         return "abend_warning";
      case client_events::kQuit:
         return "quit";
      case client_events::kSuicide: 
         return "suicide";
      case client_events::kFileChanged:
         return "file_changed";
      case client_events::kWorkingDirChanged: 
         return "working_dir_changed";
      case client_events::kPlotsStateChanged: 
         return "plots_state_changed";
      case client_events::kViewData: 
         return "view_data";
      case client_events::kPackageStatusChanged: 
         return "package_status_changed";
      case client_events::kInstalledPackagesChanged: 
         return "installed_packages_changed";
      case client_events::kLocator:
         return "locator";
      case client_events::kConsoleResetHistory:
         return "console_reset_history";
      case client_events::kSessionSerialization:
         return "session_serialization";
      case client_events::kHistoryEntriesAdded:
         return "history_entries_added";
      case client_events::kQuotaStatus:
         return "quota_status";
      case client_events::kOAuthApproval:
         return "oauth_approval";
      case client_events::kPublishPdf:
         return "publish_pdf";
      case client_events::kFileEdit:
         return "file_edit";
      case client_events::kShowContent:
         return "show_content";
      case client_events::kShowData:
         return "show_data";
      case client_events::kAsyncCompletion:
         return "async_completion";
      case client_events::kSaveActionChanged:
         return "save_action_changed";
      case client_events::kConsoleWritePrompt:
         return "console_write_prompt";
      case client_events::kConsoleWriteInput:
         return "console_write_input";
      case client_events::kShowWarningBar:
         return "show_warning_bar";
      case client_events::kOpenProjectError:
         return "open_project_error";
      case client_events::kVcsRefresh:
         return "vcs_refresh";
      case client_events::kAskPass:
         return "ask_pass";
      case client_events::kConsoleProcessOutput:
         return "console_process_output";
      case client_events::kConsoleProcessExit:
         return "console_process_exit";
      default:
         LOG_WARNING_MESSAGE("unexpected event type: " + 
                             boost::lexical_cast<std::string>(type_));
         return "";
   }
}

ClientEvent browseUrlEvent(const std::string& url, const std::string& window)
{
   json::Object browseURLInfo;
   browseURLInfo["url"] = url;
   browseURLInfo["window"] = window;
   return ClientEvent(client_events::kBrowseUrl, browseURLInfo);
}
    
   
ClientEvent showErrorMessageEvent(const std::string& title,
                                  const std::string& message)
{
   json::Object errorMessage ;
   errorMessage["title"] = title;
   errorMessage["message"] = message;
   return ClientEvent(client_events::kShowErrorMessage, errorMessage);
}


   
   
} // namespace session
