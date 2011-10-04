/*
 * DesktopInputDialog.cpp
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

#include "DesktopInputDialog.hpp"
#include "ui_DesktopInputDialog.h"

#include <QPushButton>

InputDialog::InputDialog(QWidget *parent) :
    QDialog(parent),
    ui(new Ui::InputDialog()),
    pOK_(NULL)
{
   ui->setupUi(this);
   setWindowFlags(Qt::Dialog | Qt::MSWindowsFixedSizeDialogHint);

   pOK_ = new QPushButton(QString::fromAscii("OK"));
   ui->buttonBox->addButton(pOK_, QDialogButtonBox::AcceptRole);

   QPushButton* pCancel = new QPushButton(QString::fromAscii("Cancel"));
   ui->buttonBox->addButton(pCancel, QDialogButtonBox::RejectRole);
}

InputDialog::~InputDialog()
{
   delete ui;
}

QString InputDialog::caption()
{
   return ui->label->text();
}

void InputDialog::setCaption(QString caption)
{
   ui->label->setText(caption);
}

QString InputDialog::textValue()
{
   return ui->lineEdit->text();
}

void InputDialog::setTextValue(QString value)
{
   ui->lineEdit->setText(value);
}

void InputDialog::setSelection(int offset, int length)
{
   offset = std::min(offset, textValue().size());
   length = std::min(length,
                     textValue().size() - offset);

   ui->lineEdit->setSelection(offset, length);
}

void InputDialog::setOkButtonLabel(QString label)
{
   pOK_->setText(label);
}

void InputDialog::setEchoMode(QLineEdit::EchoMode mode)
{
   ui->lineEdit->setEchoMode(mode);
}

void InputDialog::setNumbersOnly(bool numbersOnly)
{
   if (numbersOnly)
      ui->lineEdit->setInputMask(QString::fromAscii("D99999999"));
   else
      ui->lineEdit->setInputMask(QString());
}
