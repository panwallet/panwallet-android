package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.NetworkOnMainThreadException;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.SetPinActivity;
import com.breadwallet.presenter.activities.PaperKeyActivity;
import com.breadwallet.presenter.activities.PaperKeyProveActivity;
import com.breadwallet.presenter.activities.intro.WriteDownActivity;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.threads.PaymentProtocolPostPaymentTask;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.platform.APIClient;
import com.platform.entities.TxMetaData;
import com.platform.tools.BRBitId;
import com.platform.tools.KVStoreManager;

import java.io.IOException;
import java.util.Arrays;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 4/14/16.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class PostAuth {
    public static final String TAG = PostAuth.class.getName();

    private String phraseForKeyStore;
    public PaymentItem paymentItem;
    private PaymentRequestWrapper paymentRequest;

    private static PostAuth instance;

    private PostAuth() {
    }

    public static PostAuth getInstance() {
        if (instance == null) {
            instance = new PostAuth();
        }
        return instance;
    }

    public void onCreateWalletAuth(Activity app, boolean authAsked) {
//        Log.e(TAG, "onCreateWalletAuth: " + authAsked + ", " + app.getClass().getName());
        long start = System.currentTimeMillis();
        boolean success = BRWalletManager.getInstance().generateRandomSeed(app);
        if (success) {
            Intent intent = new Intent(app, WriteDownActivity.class);
            app.startActivity(intent);
            app.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

        } else {
            Log.e(TAG, "onCreateWalletAuth: Failed to generateSeed");

        }
    }

    public void onPhraseCheckAuth(Activity app, boolean authAsked) {
        String cleanPhrase;
        try {
            cleanPhrase = new String(BRKeyStore.getPhrase(app, BRConstants.SHOW_PHRASE_REQUEST_CODE));
        } catch (UserNotAuthenticatedException e) {
            return;
        }
        Intent intent = new Intent(app, PaperKeyActivity.class);
        intent.putExtra("phrase", cleanPhrase);
        app.startActivity(intent);
        app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
    }

    public void onPhraseProveAuth(Activity app, boolean authAsked) {
        String cleanPhrase;
        try {
            cleanPhrase = new String(BRKeyStore.getPhrase(app, BRConstants.PROVE_PHRASE_REQUEST));
        } catch (UserNotAuthenticatedException e) {
            return;
        }
        Intent intent = new Intent(app, PaperKeyProveActivity.class);
        intent.putExtra("phrase", cleanPhrase);
        app.startActivity(intent);
        app.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    public void onBitIDAuth(Activity app, boolean authenticated) {
        BRBitId.completeBitID(app, authenticated);
    }

    public void onRecoverWalletAuth(Activity app, boolean authAsked) {
        if (phraseForKeyStore == null) {
            Log.e(TAG, "onRecoverWalletAuth: phraseForKeyStore is null!");
            BRReportsManager.reportBug(new NullPointerException("onRecoverWalletAuth: phraseForKeyStore is null"));
            return;
        }
        byte[] bytePhrase = new byte[0];

        try {
            boolean success = false;
            try {
                success = BRKeyStore.putPhrase(phraseForKeyStore.getBytes(),
                        app, BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE);
            } catch (UserNotAuthenticatedException e) {
                Log.e(TAG, "onRecoverWalletAuth: not authenticated");
                return;
            }

            if (!success) {
                if (authAsked) {
                    Log.e(TAG, "onRecoverWalletAuth,!success && authAsked");
                }
            } else {
                if (phraseForKeyStore.length() != 0) {
                    BRSharedPrefs.putPhraseWroteDown(app, true);
                    bytePhrase = TypesConverter.getNullTerminatedPhrase(phraseForKeyStore.getBytes());
                    byte[] seed = BRWalletManager.getSeedFromPhrase(bytePhrase);
                    byte[] authKey = BRWalletManager.getAuthPrivKeyForAPI(seed);
                    BRKeyStore.putAuthKey(authKey, app);
                    byte[] pubKey = BRWalletManager.getInstance().getMasterPubKey(bytePhrase);
                    BRKeyStore.putMasterPublicKey(pubKey, app);
                    app.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                    Intent intent = new Intent(app, SetPinActivity.class);
                    intent.putExtra("noPin", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    app.startActivity(intent);
                    if (!app.isDestroyed()) app.finish();
                    phraseForKeyStore = null;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            BRReportsManager.reportBug(e);
        } finally {
            Arrays.fill(bytePhrase, (byte) 0);
        }

    }

    public void onPublishTxAuth(final Context app, boolean authAsked) {
        if (ActivityUTILS.isMainThread()) throw new NetworkOnMainThreadException();

        final BRWalletManager walletManager = BRWalletManager.getInstance();
        byte[] rawSeed;
        try {
            rawSeed = BRKeyStore.getPhrase(app, BRConstants.PAY_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            return;
        }
        if (rawSeed.length < 10) return;
        final byte[] seed = TypesConverter.getNullTerminatedPhrase(rawSeed);
        try {
            if (seed.length != 0) {
                if (paymentItem != null && paymentItem.serializedTx != null) {
                    byte[] txHash = walletManager.publishSerializedTransaction(paymentItem.serializedTx, seed);
                    Log.e(TAG, "onPublishTxAuth: txhash:" + Arrays.toString(txHash));
                    if (Utils.isNullOrEmpty(txHash)) {
                        Log.e(TAG, "onPublishTxAuth: publishSerializedTransaction returned FALSE");
                        //todo fix this
//                        BRWalletManager.getInstance().offerToChangeTheAmount(app, new PaymentItem(paymentRequest.addresses, paymentItem.serializedTx, paymentRequest.amount, null, paymentRequest.isPaymentRequest));
                    } else {
                        TxMetaData txMetaData = new TxMetaData();
                        txMetaData.comment = paymentItem.comment;
                        KVStoreManager.getInstance().putTxMetaData(app, txMetaData, txHash);
                    }
                    paymentItem = null;
                } else {
                    throw new NullPointerException("payment item is null");
                }
            } else {
                Log.e(TAG, "onPublishTxAuth: seed length is 0!");
                return;
            }
        } finally {
            Arrays.fill(seed, (byte) 0);
        }

    }

    public void onPaymentProtocolRequest(Activity app, boolean authAsked) {

        byte[] rawSeed;
        try {
            rawSeed = BRKeyStore.getPhrase(app, BRConstants.PAYMENT_PROTOCOL_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            return;
        }
        if (rawSeed == null || rawSeed.length < 10 || paymentRequest.serializedTx == null) {
            Log.d(TAG, "onPaymentProtocolRequest() returned: rawSeed is malformed: " + Arrays.toString(rawSeed));
            return;
        }
        if (rawSeed.length < 10) return;

        final byte[] seed = TypesConverter.getNullTerminatedPhrase(rawSeed);

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                byte[] txHash = BRWalletManager.getInstance().publishSerializedTransaction(paymentRequest.serializedTx, seed);
                if (Utils.isNullOrEmpty(txHash)) throw new NullPointerException("txHash is null!");
                PaymentProtocolPostPaymentTask.sent = true;
                Arrays.fill(seed, (byte) 0);
                paymentRequest = null;
            }
        });

    }

    public void setPhraseForKeyStore(String phraseForKeyStore) {
        this.phraseForKeyStore = phraseForKeyStore;
    }


    public void setPaymentItem(PaymentItem item) {
        this.paymentItem = item;
    }

    public void setTmpPaymentRequest(PaymentRequestWrapper paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

    public void onCanaryCheck(final Activity app, boolean authAsked) {
        String canary = null;
        try {
            canary = BRKeyStore.getCanary(app, BRConstants.CANARY_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            return;
        }
        if (canary == null || !canary.equalsIgnoreCase(BRConstants.CANARY_STRING)) {
            byte[] phrase;
            try {
                phrase = BRKeyStore.getPhrase(app, BRConstants.CANARY_REQUEST_CODE);
            } catch (UserNotAuthenticatedException e) {
                return;
            }

            String strPhrase = new String((phrase == null) ? new byte[0] : phrase);
            if (strPhrase.isEmpty()) {
                BRWalletManager m = BRWalletManager.getInstance();
                m.wipeKeyStore(app);
                m.wipeWalletButKeystore(app);
            } else {
                Log.e(TAG, "onCanaryCheck: Canary wasn't there, but the phrase persists, adding canary to keystore.");
                try {
                    BRKeyStore.putCanary(BRConstants.CANARY_STRING, app, 0);
                } catch (UserNotAuthenticatedException e) {
                    return;
                }
            }
        }
        BRWalletManager.getInstance().startTheWalletIfExists(app);
    }

}
