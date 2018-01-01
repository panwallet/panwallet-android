package com.panwallet.presenter.activities.settings;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.panwallet.R;
import com.panwallet.presenter.activities.util.ActivityUTILS;
import com.panwallet.presenter.activities.util.BRActivity;
import com.panwallet.tools.manager.BRSharedPrefs;
import com.panwallet.tools.manager.FontManager;
import com.panwallet.tools.security.AuthManager;
import com.panwallet.tools.security.BRKeyStore;
import com.panwallet.tools.util.BRCurrency;
import com.panwallet.tools.util.BRExchange;
import com.panwallet.wallet.BRWalletManager;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


import static com.panwallet.tools.util.BRConstants.ONE_BITCOIN;


public class SpendLimitActivity extends BRActivity {
    private static final String TAG = SpendLimitActivity.class.getName();
    public static boolean appVisible = false;
    private static SpendLimitActivity app;
    private static final long[] MONACOIN = { 10000000L, 100000000L, 1000000000L, 10000000000L };
    private ListView listView;
    private LimitAdaptor adapter;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    public static SpendLimitActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spend_limit);

        /*ImageButton faq = findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSupportFragment(app, BRConstants.fingerprintSpendingLimit);
            }
        });*/

        listView = findViewById(R.id.limit_list);
        listView.setFooterDividersEnabled(true);
        adapter = new LimitAdaptor(this);
        List<Long> items = new ArrayList<>();
        items.add(getAmountByStep(0).longValue());
        items.add(getAmountByStep(1).longValue());
        items.add(getAmountByStep(2).longValue());
        items.add(getAmountByStep(3).longValue());
        items.add(getAmountByStep(4).longValue());

        adapter.addAll(items);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.e(TAG, "onItemClick: " + position);
                long limit = adapter.getItem(position);
                BRKeyStore.putSpendLimit(limit, app);

                AuthManager.getInstance().setTotalLimit(app, BRWalletManager.getInstance().getTotalSent()
                        + BRKeyStore.getSpendLimit(app));
                adapter.notifyDataSetChanged();
            }

        });
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

    }

    //satoshis
    private BigDecimal getAmountByStep(int step) {
        BigDecimal result;
        switch (step) {
            case 0:
                result = new BigDecimal(0);// 0 always require
                break;
            case 1:
                result = new BigDecimal(MONACOIN[0]);//   0.1 MONA
                break;
            case 2:
                result = new BigDecimal(MONACOIN[1]);//   1 MONA
                break;
            case 3:
                result = new BigDecimal(MONACOIN[2]);//   10 MONA
                break;
            case 4:
                result = new BigDecimal(MONACOIN[3]);//   100 MONA
                break;

            default:
                result = new BigDecimal(MONACOIN[1]);//   1 MONA Default
                break;
        }
        return result;
    }

    private int getStepFromLimit(long limit) {
        switch ((int) limit) {

            case 0:
                return 0;
            case 1:
                return 1;
            case 10:
                return 2;
            case 100:
                return 3;
            case 1000:
                return 4;
            default:
                return 2; //1 MONA Default
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
        ActivityUTILS.init(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    public class LimitAdaptor extends ArrayAdapter<Long> {

        private final Context mContext;
        private final int layoutResourceId;
        private TextView textViewItem;

        public LimitAdaptor(Context mContext) {

            super(mContext, R.layout.currency_list_item);

            this.layoutResourceId = R.layout.currency_list_item;
            this.mContext = mContext;
        }

        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            final long limit = BRKeyStore.getSpendLimit(app);
            if (convertView == null) {
                // inflate the layout
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                convertView = inflater.inflate(layoutResourceId, parent, false);
            }
            // get the TextView and then set the text (item name) and tag (item ID) values
            textViewItem = convertView.findViewById(R.id.currency_item_text);
            FontManager.overrideFonts(textViewItem);

            BigDecimal item = getAmountByStep(position);
            BigDecimal curRate = BRExchange.getAmountFromSatoshis(app, BRSharedPrefs.getIso(app), item);
            String curAmount = BRCurrency.getFormattedCurrencyString(app, BRSharedPrefs.getIso(app), curRate);

            item = item.divide(new BigDecimal(MONACOIN[1]));
            String btcAmount = BRCurrency.getFormattedCurrencyString(app, "MONA", item);

            String text = String.format(item.signum() == 0 ? app.getString(R.string.TouchIdSpendingLimit) : "%s (%s)", curAmount, btcAmount);
            textViewItem.setText(text);
            ImageView checkMark = convertView.findViewById(R.id.currency_checkmark);

            if (position == getStepFromLimit(limit / MONACOIN[0])) {
                checkMark.setVisibility(View.VISIBLE);
            } else {
                checkMark.setVisibility(View.GONE);
            }
            return convertView;

        }

        @Override
        public int getCount() {
            return super.getCount();
        }

        @Override
        public int getItemViewType(int position) {
            return IGNORE_ITEM_VIEW_TYPE;
        }

    }

}
