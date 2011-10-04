/*
 * DesktopMainWindow.cpp
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

#include "DesktopMainWindow.hpp"

#include <QtGui>
#include <QtWebKit>

#include <boost/bind.hpp>

#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#include "DesktopGwtCallback.hpp"
#include "DesktopMenuCallback.hpp"
#include "DesktopWebView.hpp"
#include "DesktopOptions.hpp"
#include "DesktopSlotBinders.hpp"
#include "DesktopUtils.hpp"

using namespace core;

namespace desktop {

MainWindow::MainWindow(QUrl url) :
      BrowserWindow(false, false, url, NULL),
      menuCallback_(this),
      gwtCallback_(this),
      updateChecker_(this),
      pCurrentSessionProcess_(NULL)
{
   quitConfirmed_ = false;
   pToolbar_->setVisible(false);

   // Dummy menu bar to deal with the fact that
   // the real menu bar isn't ready until well
   // after startup.
   QMenuBar* pMainMenuStub = new QMenuBar(this);
   pMainMenuStub->addMenu(QString::fromUtf8("File"));
   pMainMenuStub->addMenu(QString::fromUtf8("Edit"));
   pMainMenuStub->addMenu(QString::fromUtf8("View"));
   pMainMenuStub->addMenu(QString::fromUtf8("Project"));
   pMainMenuStub->addMenu(QString::fromUtf8("Workspace"));
   pMainMenuStub->addMenu(QString::fromUtf8("Plots"));
   pMainMenuStub->addMenu(QString::fromUtf8("Tools"));
   pMainMenuStub->addMenu(QString::fromUtf8("Help"));
   setMenuBar(pMainMenuStub);

   connect(&menuCallback_, SIGNAL(menuBarCompleted(QMenuBar*)),
           this, SLOT(setMenuBar(QMenuBar*)));
   connect(&menuCallback_, SIGNAL(commandInvoked(QString)),
           this, SLOT(invokeCommand(QString)));
   connect(&menuCallback_, SIGNAL(manageCommand(QString,QAction*)),
           this, SLOT(manageCommand(QString,QAction*)));

   connect(&gwtCallback_, SIGNAL(workbenchInitialized()),
           this, SIGNAL(firstWorkbenchInitialized()));
   connect(&gwtCallback_, SIGNAL(workbenchInitialized()),
           this, SLOT(onWorkbenchInitialized()));

   setWindowIcon(QIcon(QString::fromAscii(":/icons/RStudio.ico")));

   setWindowTitle(QString::fromAscii("RStudio"));

#ifdef Q_OS_MAC
   QMenuBar* pDefaultMenu = new QMenuBar();
   pDefaultMenu->addMenu(new WindowMenu());
#endif

   //setContentsMargins(10000, 0, -10000, 0);
   setStyleSheet(QString::fromAscii("QMainWindow { background: #e1e2e5; }"));
}

void MainWindow::onWorkbenchInitialized()
{
   //QTimer::singleShot(300, this, SLOT(resetMargins()));

   // reset state (in case this occurred in response to a manual reload
   // or reload for a new project context)
   quitConfirmed_ = false;

   // see if there is a project dir to display in the titlebar
   // if there are unsaved changes then resolve them before exiting
   QVariant vProjectDir = webView()->page()->mainFrame()->evaluateJavaScript(
         QString::fromAscii("window.desktopHooks.getActiveProjectDir()"));
   QString projectDir = vProjectDir.toString();
   if (projectDir.length() > 0)
      setWindowTitle(projectDir + QString::fromAscii(" - RStudio"));
   else
      setWindowTitle(QString::fromAscii("RStudio"));

#ifdef Q_WS_MACX
   webView()->page()->mainFrame()->evaluateJavaScript(
         QString::fromAscii("document.body.className = document.body.className + ' avoid-move-cursor'"));
#endif

   // check for updates
   updateChecker_.performCheck(false);
}

void MainWindow::resetMargins()
{
   setContentsMargins(0, 0, 0, 0);
}

void MainWindow::loadUrl(const QUrl& url)
{
   webView()->setBaseUrl(url);
   webView()->load(url);
}

void MainWindow::quit()
{
   quitConfirmed_ = true;
   close();
}

void MainWindow::onJavaScriptWindowObjectCleared()
{
   webView()->page()->mainFrame()->addToJavaScriptWindowObject(
         QString::fromAscii("desktop"),
         &gwtCallback_,
         QScriptEngine::QtOwnership);
   webView()->page()->mainFrame()->addToJavaScriptWindowObject(
         QString::fromAscii("desktopMenuCallback"),
         &menuCallback_,
         QScriptEngine::QtOwnership);
}

void MainWindow::invokeCommand(QString commandId)
{
   webView()->page()->mainFrame()->evaluateJavaScript(
         QString::fromAscii("window.desktopHooks.invokeCommand('") + commandId + QString::fromAscii("');"));
}

void MainWindow::manageCommand(QString cmdId, QAction* action)
{
   QWebFrame* pMainFrame = webView()->page()->mainFrame();
   action->setVisible(pMainFrame->evaluateJavaScript(
         QString::fromAscii("window.desktopHooks.isCommandVisible('") + cmdId + QString::fromAscii("')")).toBool());
   action->setEnabled(pMainFrame->evaluateJavaScript(
         QString::fromAscii("window.desktopHooks.isCommandEnabled('") + cmdId + QString::fromAscii("')")).toBool());
   action->setText(pMainFrame->evaluateJavaScript(
         QString::fromAscii("window.desktopHooks.getCommandLabel('") + cmdId + QString::fromAscii("')")).toString());
}

void MainWindow::closeEvent(QCloseEvent* pEvent)
{
   QWebFrame* pFrame = webView()->page()->mainFrame();
   if (!pFrame)
   {
       pEvent->accept();
       return;
   }

   QVariant hasQuitR = pFrame->evaluateJavaScript(QString::fromAscii("!!window.desktopHooks"));

   if (quitConfirmed_
       || !hasQuitR.toBool()
       || pCurrentSessionProcess_ == NULL
       || pCurrentSessionProcess_->state() != QProcess::Running)
   {
      pEvent->accept();
   }
   else
   {
      pFrame->evaluateJavaScript(QString::fromAscii("window.desktopHooks.quitR()"));
      pEvent->ignore();
   }
}

void MainWindow::setMenuBar(QMenuBar *pMenubar)
{
   delete menuBar();
   this->QMainWindow::setMenuBar(pMenubar);
}

void MainWindow::openFileInRStudio(QString path)
{
   QFileInfo fileInfo(path);
   if (!fileInfo.isAbsolute() || !fileInfo.exists() || !fileInfo.isFile())
      return;

   path = path.replace(QString::fromAscii("\\"), QString::fromAscii("\\\\"))
          .replace(QString::fromAscii("\""), QString::fromAscii("\\\""))
          .replace(QString::fromAscii("\n"), QString::fromAscii("\\n"));

   webView()->page()->mainFrame()->evaluateJavaScript(
         QString::fromAscii("window.desktopHooks.openFile(\"") + path + QString::fromAscii("\")"));
}

void MainWindow::checkForUpdates()
{
   updateChecker_.performCheck(true);
}


// private interface for SessionLauncher

void MainWindow::setSessionProcess(QProcess* pSessionProcess)
{
   pCurrentSessionProcess_ = pSessionProcess;
}

// allow SessionLauncher to collect switch requests from GwtCallback
bool MainWindow::collectPendingSwitchToProjectRequest()
{
   return gwtCallback_.collectPendingSwitchToProjectRequest();
}

} // namespace desktop
