/*
 * DesktopOptions.cpp
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

#include "DesktopOptions.hpp"

#include <QtGui>

#include <core/Error.hpp>
#include <core/Random.hpp>
#include <core/system/System.hpp>

using namespace core;

namespace desktop {

#ifdef _WIN32
// Defined in DesktopRVersion.cpp
QString binDirToHomeDir(QString binDir);
#endif

QString scratchPath;

Options& options()
{
   static Options singleton;
   return singleton;
}

void Options::restoreMainWindowBounds(QMainWindow* win)
{
   settings_.beginGroup(QString::fromAscii("mainwindow"));

   // If we restore to a smaller monitor than when we saved, let the system
   // decide where to position the window.
   QSize desktopSize = settings_.value(QString::fromAscii("desktopSize"), QSize(0, 0)).toSize();
   QSize curDesktopSize = QApplication::desktop()->size();
   if (desktopSize.width() != 0 && desktopSize.height() != 0
       && desktopSize.width() <= curDesktopSize.width()
       && desktopSize.height() <= curDesktopSize.height())
   {
      win->move(settings_.value(QString::fromAscii("pos"), QPoint(0, 0)).toPoint());
   }

   QSize size = settings_.value(QString::fromAscii("size"), QSize(1024, 768)).toSize()
          .expandedTo(QSize(200, 200))
          .boundedTo(QApplication::desktop()->availableGeometry(win->pos()).size());
   win->resize(size);

   if (settings_.value(QString::fromAscii("maximized"), false).toBool())
      win->setWindowState(Qt::WindowMaximized);

   settings_.endGroup();
}

void Options::saveMainWindowBounds(QMainWindow* win)
{
   settings_.setValue(QString::fromAscii("mainwindow/maximized"), win->isMaximized());
   if (!win->isMaximized())
   {
     settings_.setValue(QString::fromAscii("mainwindow/size"), win->size());
     settings_.setValue(QString::fromAscii("mainwindow/pos"), win->pos());
     settings_.setValue(QString::fromAscii("mainwindow/desktopSize"), QApplication::desktop()->size());
   }
}

QString Options::portNumber() const
{
   // lookup / generate on demand
   if (portNumber_.length() == 0)
   {
      // Use a random-ish port number to avoid collisions between different
      // instances of rdesktop-launched rsessions
      int base = std::abs(core::random::uniformRandomInteger<int>());
      portNumber_ = QString::number((base % 40000) + 8080);
   }

   return portNumber_;
}

QString Options::newPortNumber()
{
   portNumber_.clear();
   return portNumber();
}

namespace {
QString findFirstMatchingFont(const QStringList& fonts, QString defaultFont)
{
   QStringList families = QFontDatabase().families();
   for (int i = 0; i < fonts.size(); i++)
      if (families.contains(fonts.at(i)))
         return fonts.at(i);
   return defaultFont;
}
} // anonymous namespace

QString Options::proportionalFont() const
{
   QStringList fontList;
#if defined(_WIN32)
   fontList <<
           QString::fromAscii("Segoe UI") << QString::fromAscii("Verdana") <<  // Windows
           QString::fromAscii("Lucida Sans") << QString::fromAscii("DejaVu Sans") <<  // Linux
           QString::fromAscii("Lucida Grande") <<          // Mac
           QString::fromAscii("Helvetica");
#elif defined(__APPLE__)
   fontList <<
           QString::fromAscii("Lucida Grande") <<          // Mac
           QString::fromAscii("Lucida Sans") << QString::fromAscii("DejaVu Sans") <<  // Linux
           QString::fromAscii("Segoe UI") << QString::fromAscii("Verdana") <<  // Windows
           QString::fromAscii("Helvetica");
#else
   fontList <<
           QString::fromAscii("Lucida Sans") << QString::fromAscii("DejaVu Sans") <<  // Linux
           QString::fromAscii("Lucida Grande") <<          // Mac
           QString::fromAscii("Segoe UI") << QString::fromAscii("Verdana") <<  // Windows
           QString::fromAscii("Helvetica");
#endif
   return QString::fromAscii("\"") +
         findFirstMatchingFont(fontList, QString::fromAscii("sans-serif")) +
         QString::fromAscii("\"");
}

QString Options::fixedWidthFont() const
{
   // NB: Windows has "Lucida Console" and "Consolas" reversed vs.
   // the list in WebThemeFontLoader (in the GWT codebase). This is
   // because Consolas is more attractive but has a spacing issue
   // in Win/Desktop mode (U+200B shows up as a space, and we use it
   // in the History pane).
   QStringList fontList;
   fontList <<
#ifdef Q_WS_X11
           QString::fromAscii("Ubuntu Mono") <<
#endif
           QString::fromAscii("Monospace") << QString::fromAscii("Droid Sans Mono") << QString::fromAscii("DejaVu Sans Mono") << // Linux
           QString::fromAscii("Monaco") <<                      // Mac
           QString::fromAscii("Lucida Console") << QString::fromAscii("Consolas")   // Windows;
           ;
   return QString::fromAscii("\"") +
         findFirstMatchingFont(fontList, QString::fromAscii("monospace")) +
         QString::fromAscii("\"");
}


#ifdef _WIN32
QString Options::rBinDir() const
{
   // HACK: If RBinDir doesn't appear at all, that means the user has never
   // specified a preference for R64 vs. 32-bit R. In this situation we should
   // accept either. We'll distinguish between this case (where preferR64
   // should be ignored) and the other case by using null for this case and
   // empty string for the other.
   if (!settings_.contains(QString::fromAscii("RBinDir")))
      return QString::null;

   QString value = settings_.value(QString::fromAscii("RBinDir")).toString();
   return value.isNull() ? QString() : value;
}

void Options::setRBinDir(QString path)
{
   settings_.setValue(QString::fromAscii("RBinDir"), path);
}

bool Options::preferR64() const
{
   if (!core::system::isWin64())
      return false;

   if (!settings_.contains(QString::fromAscii("PreferR64")))
      return true;
   return settings_.value(QString::fromAscii("PreferR64")).toBool();
}

void Options::setPreferR64(bool preferR64)
{
   settings_.setValue(QString::fromAscii("PreferR64"), preferR64);
}
#endif


FilePath Options::supportingFilePath() const
{
   if (supportingFilePath_.empty())
   {
      // default to install path
      core::system::installPath("..",
                                QApplication::argc(),
                                QApplication::argv(),
                                &supportingFilePath_);

      // adapt for OSX resource bundles
#ifdef __APPLE__
         if (supportingFilePath_.complete("Info.plist").exists())
            supportingFilePath_ = supportingFilePath_.complete("Resources");
#endif
   }
   return supportingFilePath_;
}


QStringList Options::ignoredUpdateVersions() const
{
   return settings_.value(QString::fromAscii("ignoredUpdateVersions"), QStringList()).toStringList();
}

void Options::setIgnoredUpdateVersions(const QStringList& ignoredVersions)
{
   settings_.setValue(QString::fromAscii("ignoredUpdateVersions"), ignoredVersions);
}

core::FilePath Options::scratchTempDir(core::FilePath defaultPath)
{
   core::FilePath dir(scratchPath.toUtf8().constData());

   if (!dir.empty() && dir.exists())
   {
      dir = dir.childPath("tmp");
      core::Error error = dir.ensureDirectory();
      if (!error)
         return dir;
   }
   return defaultPath;
}

void Options::cleanUpScratchTempDir()
{
   core::FilePath temp = scratchTempDir(core::FilePath());
   if (!temp.empty())
      temp.removeIfExists();
}

bool Options::webkitDevTools()
{
   return settings_.value(QString::fromAscii("webkitDevTools"), false).toBool();
}

} // namespace desktop
