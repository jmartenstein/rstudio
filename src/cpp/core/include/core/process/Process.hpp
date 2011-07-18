/*
 * Process.hpp
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

#ifndef CORE_PROCESS_PROCESS_HPP
#define CORE_PROCESS_PROCESS_HPP

#include <string>


#include <boost/utility.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/function.hpp>

#include <core/Error.hpp>

// for Pid type
#if defined(_WIN32)
#include <windef.h>
#else  // UNIX
#endif

namespace boost {
   namespace process {
      class child;
   }
}

namespace core {

class FilePath;

namespace process {

#if defined(_WIN32)
typedef DWORD Pid;
#else  // UNIX
typedef pid_t Pid;
#endif

class Child
{
private:
   friend class Supervisor;
   Child();

public:
   virtual ~Child();

public:
   Pid id() const;

   Error terminate(bool force = false);

   Error writeToStdin(const std::string& input);

public:

   virtual void onStdout(const std::string& output)
   {
   }

   virtual void onStderr(const std::string& err)
   {
   }

   virtual void onExit(int exitCode)
   {
   }


private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};

class Supervisor;

class Supervisor : boost::noncopyable
{
public:
   Supervisor();
   virtual ~Supervisor();

   Error createChild(const std::string& exeName,
                     std::vector<std::string> args,
                     boost::shared_ptr<Child>* ppChild);

   Error createChild(const FilePath& exePath,
                     std::vector<std::string> args,
                     boost::shared_ptr<Child>* ppChild);

   Error processEvents();


private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};


} // namespace process
} // namespace core

#endif // CORE_PROCESS_PROCESS_HPP
