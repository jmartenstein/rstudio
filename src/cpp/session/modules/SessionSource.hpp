/*
 * SessionSource.hpp
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

#ifndef SESSION_SOURCE_HPP
#define SESSION_SOURCE_HPP

#include <vector>

#include <boost/shared_ptr.hpp>

#include <core/json/Json.hpp>

namespace core {
   class Error;
   namespace r_util {
      class RSourceIndex;
   }
}
 
namespace session {
namespace modules { 
namespace source {
   
core::Error clientInitDocuments(core::json::Array* pJsonDocs) ;

std::vector<boost::shared_ptr<core::r_util::RSourceIndex> > rIndexes();

core::Error initialize();
                       
} // namespace source
} // namespace modules
} // namesapce session

#endif // SESSION_SOURCE_HPP
