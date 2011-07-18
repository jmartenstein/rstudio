#
# CMakeLists.txt
#
# Copyright (C) 2009-11 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

project (SUPERVISOR)

# include files
file(GLOB_RECURSE SUPERVISOR_HEADER_FILES "*.h*")

# source files
set(SUPERVISOR_SOURCE_FILES
   SupervisorMain.cpp
)

# set include directories
include_directories(
   ${Boost_INCLUDE_DIRS}
   ${CORE_SOURCE_DIR}/include
)

# define executable
add_executable(rsupervisor ${SUPERVISOR_SOURCE_FILES} ${SUPERVISOR_HEADER_FILES})

# set link dependencies
target_link_libraries(rsupervisor
   rstudio-core
)



