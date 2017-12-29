package com.panwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.panwallet.PanApp;
import com.panwallet.presenter.entities.CurrencyEntity;
import com.panwallet.tools.sqlite.CurrencyDataSource;
import com.panwallet.tools.threads.BRExecutor;
import com.panwallet.tools.util.BRConstants;
import com.panwallet.tools.util.Utils;
import com.panwallet.wallet.BRWalletManager;
import com.platform.APIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static com.panwallet.presenter.fragments.FragmentSend.isEconomyFee;
import okhttp3.Request;
import okhttp3.Response;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class BRApiManager {
    private static final String TAG = BRApiManager.class.getName();

    private static BRApiManager instance;
    private Timer timer;

    private TimerTask timerTask;

    private Handler handler;

    private BRApiManager() {
        handler = new Handler();
    }

    private static String[] codes = { "BTC", "USD", "AUD", "BRL", "CAD", "CHF", "CLP", "CNY", "CZK",
            "DKK", "EUR", "GBP", "HKD", "HUF", "IDR", "ILS", "INR", "JPY", "KRW",
            "MXN", "MYR", "NOK", "NZD", "PHP", "PKR", "PLN", "RUB", "SEK", "SGD",
            "THB", "TRY", "TWD", "ZAR" };

    private static String[] names = { "Bitcoin", "US Dollar", "Australian Dollar", "Brazilian Real",
            "Canadian Dollar", "Swiss Franc", "Chilean Peso", "Chinese Yuan", "Czech Koruna",
            "Danish Krone", "Eurozone Euro", "Pound Sterling", "Hong Kong Dollar", "Hungarian Forint",
            "Indonesian Rupiah", "Israeli Shekel", "Indian Rupee", "Japanese Yen", "South Korean Won",
            "Mexican Peso", "Malaysian Ringgit", "Norwegian Krone", "New Zealand Dollar",
            "Philippine Peso", "Pakistani Rupee", "Polish Zloty", "Russian Ruble", "Swedish Krona",
            "Singapore Dollar", "Thai Baht", "Turkish Lira", "New Taiwan Dollar", "South African Rand" };

    public static BRApiManager getInstance() {

        if (instance == null) {
            instance = new BRApiManager();
        }
        return instance;
    }

    private Set<CurrencyEntity> getCurrencies(Activity context) {
        Set<CurrencyEntity> set = new LinkedHashSet<>();
        try {
            JSONArray arr = fetchRates(context);
            updateFeePerKb(context);
            if (arr != null) {
                int length = arr.length();
                for (int i = 1; i < length; i++) {
                    CurrencyEntity tmp = new CurrencyEntity();
                    try {
                        JSONObject tmpObj = (JSONObject) arr.get(i);
                        tmp.name = tmpObj.getString("name");
                        tmp.code = tmpObj.getString("code");
                        tmp.rate = (float) tmpObj.getDouble("rate");
                        String selectedISO = BRSharedPrefs.getIso(context);
//                        Log.e(TAG,"selectedISO: " + selectedISO);
                        if (tmp.code.equalsIgnoreCase(selectedISO)) {
//                            Log.e(TAG, "theIso : " + theIso);
//                                Log.e(TAG, "Putting the shit in the shared preffs");
                            BRSharedPrefs.putIso(context, tmp.code);
                            BRSharedPrefs.putCurrencyListPosition(context, i - 1);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    set.add(tmp);
                }
            } else {
                Log.e(TAG, "getCurrencies: failed to get currencies, response string: " + arr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        List tempList = new ArrayList<>(set);
        Collections.reverse(tempList);
        return new LinkedHashSet<>(set);
    }


    private void initializeTimerTask(final Context context) {
        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                if (!PanApp.isAppInBackground(context)) {
                                    Log.e(TAG, "doInBackground: Stopping timer, no activity on.");
                                    BRApiManager.getInstance().stopTimerTask();
                                }
                                Set<CurrencyEntity> tmp = getCurrencies((Activity) context);
                                if (tmp.size() > 0) {
                                    CurrencyDataSource.getInstance(context).putCurrencies(tmp);
                                }
                            }
                        });
                    }
                });
            }
        };
    }

    public void startTimer(Context context) {
        //set a new Timer
        if (timer != null) return;
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask(context);

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 0, 60000); //
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    public static JSONArray fetchRates(Activity activity) {
        StringBuilder ruse = new StringBuilder();
        ruse.append("{\"data\":[{\"code\":\"MONA\",\"name\":\"Monacoin\",\"rate\":1},");

        for (int i = 0; codes.length == names.length && i < codes.length; i++) {
            ruse.append("{\"code\":\"").append(codes[i]).append("\",\"name\":\"")
                    .append(names[i]).append("\",\"rate\":");
            String myURL = "https://api.coinmarketcap.com/v1/ticker/monacoin/?convert=" + codes[i];
            String tempJsonString = urlGET(activity, myURL);
            if (tempJsonString == null) return null;
            try {
                JSONArray tempJsonArray = new JSONArray(tempJsonString);
                JSONObject tempJsonObject = tempJsonArray.getJSONObject(0);

                String from_code = "price_" + codes[i].toLowerCase();
                String rate = tempJsonObject.getString(from_code);

                ruse.append(Double.parseDouble(rate));
                if (i + 1 == codes.length)
                    ruse.append("}]}");
                else
                    ruse.append("},");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        String jsonString = ruse.toString();

        JSONArray jsonArray = null;
        if (jsonString == null) return null;
        try {
            JSONObject obj = new JSONObject(jsonString);

            jsonArray = obj.getJSONArray("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    // There is no backup where we're headed
    /*
    public static JSONArray backupFetchRates(Activity activity) {
        String jsonString = urlGET(activity, "https://bitpay.com/rates");

        JSONArray jsonArray = null;
        if (jsonString == null) return null;
        try {
            JSONObject obj = new JSONObject(jsonString);

            jsonArray = obj.getJSONArray("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }
    */

    public static String fetchAddress(String username, Activity activity) {
        String myUrl, address;
        if (username.charAt(0) == '@')
            myUrl = "https://api.monappy.jp/v1/users/get_nickname?twitter_id=" + username.substring(1, username.length());
        else
            myUrl = "https://api.monappy.jp/v1/users/get_address?nickname=" + username;

        String jsonString = urlGET(activity, myUrl);
        if (jsonString == null) return "";
        try {
            jsonString = "[" + jsonString + "]";
            JSONArray jsonArray = new JSONArray(jsonString);
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            String status = jsonObject.getString("status");
            if (status.equals("error")) {
                Log.e(TAG, jsonObject.getString("error"));
                return "";
            }
            address = jsonObject.getString("address");
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }

        return address;
    }

    // TODO: Develop Monacoin FeePerKb API
    public static void updateFeePerKb(Activity activity) {
        long fee = 400000;
        long economyFee = 200000;
        if (fee != 0 && fee < BRConstants.MAX_FEE_PER_KB) {
            BRSharedPrefs.putFeePerKb(activity, fee);
            BRWalletManager.getInstance().setFeePerKb(fee, isEconomyFee);
        }
        if (economyFee != 0 && economyFee < BRConstants.MAX_FEE_PER_KB) {
            BRSharedPrefs.putEconomyFeePerKb(activity, economyFee);
        }
    }

    private static String urlGET(Context app, String myURL) {
//        System.out.println("Requested URL_EA:" + myURL);
        Request request = new Request.Builder()
                .url(myURL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(app, "android/HttpURLConnection"))
                .get().build();
        String response = null;
        Response resp = APIClient.getInstance(app).sendRequest(request, false, 0);

        try {
            if (resp == null) {
                Log.e(TAG, "urlGET: " + myURL + ", resp is null");
                return null;
            }
            response = resp.body().string();
            String strDate = resp.header("date");
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            Date date = formatter.parse(strDate);
            long timeStamp = date.getTime();
            BRSharedPrefs.putSecureTime(app, timeStamp);
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        } finally {
            if (resp != null) resp.close();

        }
        return response;
    }

}
