/*
 * Process.cpp
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

#include <core/process/Process.hpp>

#include <iostream>

// Boost.Process uses boost::filesystem so use our standard boost::filesystem
// include sequence
#include <core/BoostFilesystem.hpp>


// NOTE: Boost.Process is a proposed boost library for process management
//
// We have an emdedded version of the library which was download on 7/18/2011
// from this location (the file stamps embedded in the archive indicate
// that the code within was last updated on 2/11/2001):
//
//   http://www.highscore.de/boost/gsoc2010/process.zip
//
// Documentation for the library can be found at:
//
//   http://www.highscore.de/boost/gsoc2010/
//
// The following thread includes additional discussion about the
// project and its implementation (this thread was in response to the
// original posting of the code from gsoc2010):
//
//   http://thread.gmane.org/gmane.comp.lib.boost.devel/207594/
//
#include <core/BoostThread.hpp>
#include <boost/process/all.hpp>

#include <boost/shared_ptr.hpp>

#include <core/Log.hpp>
#include <core/FilePath.hpp>

namespace core {
namespace process {

namespace {


void onExit(const boost::system::error_code &ec,
            int exitCode,
            boost::shared_ptr<Child> pChild)
{
   // log and return for errors
   if (ec)
   {
      LOG_ERROR(Error(ec, ERROR_LOCATION));
      return;
   }

   std::cout << "process " << pChild->id() << " exited with status " << exitCode << std::endl;

   // call child
   pChild->onExit(exitCode);
}

} // anonymous namespace

struct Child::Impl
{
   Impl() : pChild(NULL) {}
   boost::scoped_ptr<boost::process::child> pChild;
};

Child::Child()
   : pImpl_(new Impl())
{
}

Child::~Child()
{
   try
   {
   }
   catch(...)
   {
   }
}

Pid Child::id() const
{
   return pImpl_->pChild->get_id();
}

Error Child::terminate(bool force)
{
   try
   {
      pImpl_->pChild->terminate(force);
      return Success();
   }
   catch(boost::system::system_error& e)
   {
      return Error(e.code(), ERROR_LOCATION);
   }
}

Error Child::writeToStdin(const std::string& input)
{


   return Success();
}



struct Supervisor::Impl
{
   Impl() : ioService() {}
   boost::asio::io_service ioService;
};

Supervisor::Supervisor()
   : pImpl_(new Impl())
{
}

Supervisor::~Supervisor()
{
   try
   {
   }
   catch(...)
   {
   }
}

Error Supervisor::createChild(const std::string& exe,
                              std::vector<std::string> args,
                              boost::shared_ptr<Child>* ppChild)
{
   try
   {
      std::string path = boost::process::find_executable_in_path(exe);
      return createChild(FilePath(path), args, ppChild);
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION);
      error.addProperty("exe", exe);
      return error;
   }
}

Error Supervisor::createChild(const FilePath& exePath,
                              std::vector<std::string> args,
                              boost::shared_ptr<Child>* ppChild)
{
   try
   {

      // launch process
      using namespace boost::process;
      child c = create_child(exePath.absolutePath(), args);

      // create child object and alias it
      *ppChild = boost::shared_ptr<Child>(new Child());

      // transfer pid and handles to Child to be returned
      std::map<stream_id,handle> handles;

      (*ppChild)->pImpl_->pChild.reset(new child(c.get_id(), handles));

      // setup async wait on exit
      boost::process::status status(pImpl_->ioService);
      status.async_wait(c.get_id(), boost::bind(onExit, _1, _2, *ppChild));

      return Success();
   }
   catch(const boost::system::system_error& e)
   {
      return Error(e.code(), ERROR_LOCATION);
   }
}


Error Supervisor::processEvents()
{
   boost::system::error_code ec;
   pImpl_->ioService.poll(ec);
   if (ec)
      return Error(ec, ERROR_LOCATION);
   else
      return Success();
}




} // namespace process
} // namespace core
