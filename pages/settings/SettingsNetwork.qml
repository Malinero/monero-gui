// Copyright (c) 2014-2022, The Monero Project
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without modification, are
// permitted provided that the following conditions are met:
// 
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 
// 2. Redistributions in binary form must reproduce the above copyright notice, this list
//    of conditions and the following disclaimer in the documentation and/or other
//    materials provided with the distribution.
// 
// 3. Neither the name of the copyright holder nor the names of its contributors may be
//    used to endorse or promote products derived from this software without specific
//    prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
// THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
// THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import QtQuick 2.9
import QtQuick.Layouts 1.1
import QtQuick.Controls 2.0
import QtQuick.Dialogs 1.2

import "../../js/Wizard.js" as Wizard
import "../../js/Utils.js" as Utils
import "../../version.js" as Version
import "../../components" as MoneroComponents


Rectangle {
    color: "transparent"
    Layout.fillWidth: true
    property alias infoHeight: networkLayout.height

    ColumnLayout {
        id: networkLayout
        Layout.fillWidth: true
        spacing: 20;

        anchors.left: parent.left
        anchors.top: parent.top
        anchors.right: parent.right

        MoneroComponents.WarningBox {
            Layout.topMargin: 10
            Layout.leftMargin: 10
            Layout.rightMargin: 10
            Layout.bottomMargin: 10
            text: qsTr("Your wallet communicates via the internet with nodes in the Monero network. This communication can be analyzed to identify you. Use the options below to choose how you like to protect your privacy. Please Check your local laws and internet policies before protecting your connection using Tor or I2P") + translationManager.emptyString
        }

        ColumnLayout {
            Layout.fillWidth: true
            Layout.leftMargin: 10;
            spacing: 20

            MoneroComponents.RadioButton {
                id: clearnetButton
                text: qsTr("No protection") + translationManager.emptyString;
                checked: persistentSettings.networkMode == 0
                onClicked: {
                    persistentSettings.networkMode = 0
                    proxyButton.checked = false;
                    i2pButton.checked = false;
                    torButton.checked = false;
                    persistentSettings.proxyEnabled = false;
                }
            }
            MoneroComponents.RadioButton {
                id: proxyButton
                text: "Manual socks5 proxy"
                checked: persistentSettings.networkMode == 1
                onClicked: {
                    persistentSettings.networkMode = 1;
                    clearnetButton.checked = false;
                    i2pButton.checked = false;
                    torButton.checked = false;
                    persistentSettings.proxyEnabled = true;
                }
            }

            MoneroComponents.RemoteNodeEdit {
                id: proxyEdit
                enabled: proxyButton.enabled
                Layout.leftMargin: 36
                Layout.topMargin: 6
                Layout.minimumWidth: 100
                placeholderFontSize: 15
                visible: proxyButton.checked

                daemonAddrLabelText: qsTr("IP address") + translationManager.emptyString
                daemonPortLabelText: qsTr("Port") + translationManager.emptyString

                initialAddress: socksProxyFlagSet ? socksProxyFlag : persistentSettings.proxyAddress
                onEditingFinished: {
                    persistentSettings.proxyAddress = proxyEdit.getAddress();
                }
            }
        }

        ColumnLayout {
            id: i2pLayout
            Layout.fillWidth: true
            Layout.leftMargin: 10;
            spacing: 10

            MoneroComponents.RadioButton {
                id: i2pButton
                text: qsTr("Embedded I2P") + translationManager.emptyString;
                checked: persistentSettings.networkMode == 2
                onClicked: {
                    persistentSettings.networkMode = 2
                    clearnetButton.checked = false;
                    proxyButton.checked = false;
                    torButton.checked = false;
                    persistentSettings.proxyEnabled = true;
                    persistentSettings.proxyAddress = "127.0.0.1:9051"
                }
            }

            RowLayout {
                spacing: 20;
                visible: i2pButton.checked

                MoneroComponents.StandardButton {
                    small: true
                    text: qsTr("Start I2P service") + translationManager.emptyString
                    onClicked: {
                        i2pManager.start()
                        appWindow.showStatusMessage(qsTr("I2P Service started"), 3);
                    }
                }

                MoneroComponents.StandardButton {
                    small: true
                    text: qsTr("Stop I2P Service") + translationManager.emptyString
                    onClicked: {
                        i2pManager.stop()
                        appWindow.showStatusMessage(qsTr("I2P Service stopped"), 3);
                    }
                }
            }
        }

        ColumnLayout {
            id: torLayout
            Layout.fillWidth: true
            Layout.leftMargin: 10;
            spacing: 10

            MoneroComponents.RadioButton {
                id: torButton
                text: qsTr("Embedded Tor") + translationManager.emptyString;
                checked: persistentSettings.networkMode == 3
                onClicked: {
                    persistentSettings.networkMode = 3
                    clearnetButton.checked = false;
                    proxyButton.checked = false;
                    i2pButton.checked = false;
                    persistentSettings.proxyEnabled = true;
                    persistentSettings.proxyAddress = "127.0.0.1:9050"
                }
            }

            RowLayout {
                spacing: 20;
                visible: torButton.checked

                MoneroComponents.StandardButton {
                    small: true
                    text: qsTr("Start Tor service") + translationManager.emptyString
                    onClicked: {
                        torManager.start()
                        appWindow.showStatusMessage(qsTr("Tor Service started"), 3);
                    }
                }

                MoneroComponents.StandardButton {
                    small: true
                    text: qsTr("Stop Tor Service") + translationManager.emptyString
                    onClicked: {
                        torManager.stop()
                        appWindow.showStatusMessage(qsTr("Tor Service stopped"), 3);
                    }
                }
            }
        }
    }
}
