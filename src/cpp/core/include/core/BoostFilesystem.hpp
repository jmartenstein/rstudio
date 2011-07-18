/*
 * BoostFilesystem.hpp
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

#ifndef CORE_BOOST_FILESYSTEM_HPP
#define CORE_BOOST_FILESYSTEM_HPP

#include <boost/version.hpp>
#ifdef __APPLE__
#define BOOST_FILESYSTEM_VERSION 2
#elif BOOST_VERSION >= 104400
#define BOOST_FILESYSTEM_VERSION 3
#endif
#define BOOST_FILESYSTEM_NARROW_ONLY
#define BOOST_FILESYSTEM2_NARROW_ONLY
#define BOOST_FILESYSTEM_NO_DEPRECATED
#include <boost/filesystem.hpp>

#endif // CORE_BOOST_FILESYSTEM_HPP

