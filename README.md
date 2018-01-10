![ƀ](/images/icon.png) Pan for Android
----------------------------------

[![download](/images/icon-google-play.png)] (https://play.google.com/store/apps/details?id=com.panwallet)

###monacoin done right

This is the monacoin port of the breadwallet Android app, which can be found [here](https://github.com/breadwallet/breadwallet-android).

#####a completely standalone monacoin wallet:

Unlike many other monacoin wallets, Pan is a real standalone monacoin
client. There is no server to get hacked or go down, so you can always access
your money. Using
[SPV](https://en.bitcoin.it/wiki/Thin_Client_Security#Header-Only_Clients)
mode, Pan connects directly to the monacoin network with the fast
performance you need on a mobile device.

#####the next step in wallet security:

Pan is designed to protect you from malware, browser security holes, *even physical theft*. 
With AES hardware encryption, app sandboxing, and verified boot, Pan represents a significant 
security advance over web and desktop wallets.

#####beautiful simplicity:

Simplicity is Pan's core design principle. A simple backup phrase is
all you need to restore your wallet on another device if yours is ever lost or broken.
Because Pan is [deterministic](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki),
your balance and transaction history can be recovered from just your backup phrase.

###features:

- ["simplified payment verification"](https://github.com/bitcoin/bips/blob/master/bip-0037.mediawiki) for fast mobile performance
- no server to get hacked or go down
- single backup phrase that works forever
- private keys never leave your device
- import [password protected](https://github.com/bitcoin/bips/blob/master/bip-0038.mediawiki) paper wallets
- ["payment protocol"](https://github.com/bitcoin/bips/blob/master/bip-0070.mediawiki) payee identity certification

##How to set up the development environment:
1. Download and install Java 7 or up
2. Download and Install the latest Android studio
3. Download and install the latest NDK https://developer.android.com/ndk/downloads/index.html or download it in android studio by "choosing the NDK" and press "download"
4. Go to https://github.com/edgarnet/panwallet-android and clone or download the project
5. Open the project with Android Studio and let the project sync
6. Go to SDK Manager and download all the SDK Platforms and SDK Tools
7. Initialize the submodules - <code>git submodule init</code>
8. Update the submodules - <code>git submodule update</code>
9. Build -> Rebuild Project
