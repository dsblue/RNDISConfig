package com.dsblue.rndisconfig.rndis;

import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

public class RNDIS extends AppCompatActivity {

    private boolean mIsBound;

    private static Context applicationContext;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_rndi, menu);

        // Initialize the menu based on preference settings
        final SharedPreferences myPrefs = this.getSharedPreferences(getString(R.string.prefrence_file_name), MODE_PRIVATE);
        MenuItem item = menu.findItem(R.id.action_rndis_on_boot);
        item.setChecked(myPrefs.getBoolean(getString(R.string.pref_on_startup), false));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_rndis_on_boot) {
            final SharedPreferences myPrefs = this.getSharedPreferences(getString(R.string.prefrence_file_name), MODE_PRIVATE);
            final SharedPreferences.Editor prefsEditor = myPrefs.edit();

            if (!item.isChecked()) {
                prefsEditor.putBoolean(getString(R.string.pref_on_startup), true).apply();
            } else {
                prefsEditor.putBoolean(getString(R.string.pref_on_startup), false).apply();
            }

            // Toggle the check box
            item.setChecked(!item.isChecked());

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applicationContext = getApplicationContext();

        setContentView(R.layout.activity_rndis);

        doBindService();

        InitializeApp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    static public Context getAppContext() {
        return RNDIS.applicationContext;
    }

    private void InitializeApp()
    {
        final SharedPreferences myPrefs = this.getSharedPreferences(getString(R.string.prefrence_file_name), MODE_PRIVATE);

        final EditText ip = (EditText) findViewById(R.id.editIPAddress);
        final EditText mask = (EditText) findViewById(R.id.editIPMask);
        final TextView ipText = (TextView) findViewById(R.id.textIPAddress);
        final TextView maskText = (TextView) findViewById(R.id.textIPMask);
        final Button buttonDHCP = (Button) findViewById(R.id.buttonDhcp);
        final Spinner hosts = (Spinner) findViewById(R.id.spinnerHosts);
        final Switch enable = (Switch) findViewById(R.id.rndis_switch);

        // Populate Common Host types
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.host_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        hosts.setAdapter(adapter);

        hosts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = parent.getItemAtPosition(position).toString();

                if (selection.equals("TrellisWare CUB")) {
                    buttonDHCP.setVisibility(View.GONE);
                    ip.setVisibility(View.VISIBLE);
                    ipText.setVisibility(View.VISIBLE);
                    mask.setVisibility(View.VISIBLE);
                    maskText.setVisibility(View.VISIBLE);
                } else if (selection.equals("Windows 7 ICS (Static IP)")) {
                    buttonDHCP.setVisibility(View.GONE);
                    ip.setVisibility(View.VISIBLE);
                    ipText.setVisibility(View.VISIBLE);
                    mask.setVisibility(View.VISIBLE);
                    maskText.setVisibility(View.VISIBLE);
                } else if (selection.equals("Windows 7 ICS (DHCP)")) {
                    buttonDHCP.setVisibility(View.VISIBLE);
                    ip.setVisibility(View.GONE);
                    ipText.setVisibility(View.GONE);
                    mask.setVisibility(View.GONE);
                    maskText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Load defaults from previous run of the App
        String ipString = myPrefs.getString(getString(R.string.pref_ip), null);
        String maskString = myPrefs.getString(getString(R.string.pref_ip_mask), null);

        if (ipString != null) {
            ip.setText(ipString);
        }

        if (maskString != null) {
            mask.setText(maskString);
        }

        if (mIsBound) {
            boolean isRNDIS = mBoundService.isInRNDISMode();
            if (isRNDIS) {
                // In RNDIS mode
                Log.i("InitializeApp()", "In RNDIS mode");
                enable.setChecked(true);
            } else {
                Log.i("InitializeApp()", "Not in RNDIS mode");
                enable.setChecked(false);
            }
        }

        enable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Save current entries for next run
                SharedPreferences.Editor prefsEditor = myPrefs.edit();
                prefsEditor.putString(getString(R.string.pref_ip), ip.getText().toString());
                prefsEditor.putString(getString(R.string.pref_ip_mask), mask.getText().toString());
                prefsEditor.apply();

                // true if the switch is in the On position
                if (mIsBound) {
                    if (isChecked) {
                        mBoundService.enterRNDISMode(ip.getText().toString(), mask.getText().toString());
                    } else {
                        mBoundService.exitRNDISMode();
                    }
                }
            }
        });

        buttonDHCP.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("buttonDhcp", "Attempting to probe for DHCP");
                mBoundService.probeDHCP();
            }
        });
    }

    private WatchForDevices mBoundService;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((WatchForDevices.LocalBinder)service).getService();
            mIsBound = true;

            final Switch enable = (Switch) findViewById(R.id.rndis_switch);
            boolean isRNDIS = mBoundService.isInRNDISMode();
            if (isRNDIS) {
                // In RNDIS mode
                Log.i("InitializeApp()", "In RNDIS mode");
                enable.setChecked(true);
            } else {
                Log.i("InitializeApp()", "Not in RNDIS mode");
                enable.setChecked(false);
            }

            // Tell the user about this for our demo.
            Toast.makeText(RNDIS.this, R.string.local_service_connected, Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mIsBound = false;
            mBoundService = null;
            Toast.makeText(RNDIS.this, R.string.local_service_disconnected, Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(RNDIS.this, WatchForDevices.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
        }
    }
}
