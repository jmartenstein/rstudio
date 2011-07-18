/*
 * Supervisor.cpp
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

#include <iostream>

#include <boost/lexical_cast.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>
#include <core/process/Process.hpp>


using namespace core ;

int exitFailure(const Error& error, const ErrorLocation& location)
{
   core::log::logError(error, location);
   return EXIT_FAILURE;
}

class PrintExitStatus : public process::Child
{
public:

   virtual void onExit(int exitCode)
   {

      std::cout << "process " << id() << " exited with status " << exitCode << std::endl;
   }


};

int main(int argc, char * const argv[]) 
{
   try
   { 
      // initialize log
      initializeSystemLog("rsupervisor", core::system::kLogLevelWarning);


      process::Supervisor supervisor;

      // fire off a bunch of processess
      for (int i = 0; i<10; i++)
      {
         std::string arg = boost::lexical_cast<std::string>(i);
         std::vector<std::string> args;
         args.push_back(arg);

         boost::shared_ptr<process::Child> pChild;
         Error error = supervisor.createChild("sleep", args, &pChild);
         if (error)
            LOG_ERROR(error);

         std::cout << "created pid " << pChild->id() << std::endl;

      }

      while (true)
         supervisor.processEvents();



      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}

