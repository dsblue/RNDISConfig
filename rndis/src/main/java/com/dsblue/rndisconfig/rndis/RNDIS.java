package com.dsblue.rndisconfig.rndis;

import android.support.v7.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.io.*;

public class RNDIS extends AppCompatActivity {

    private static final String INTERFACE = "rndis0";

    private File usbTetherStart;
    private File usbTetherStop;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_rndi, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
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
        setContentView(R.layout.activity_rndis);

        usbTetherStart = loadScript("usb_tether_start.sh");
        usbTetherStop = loadScript("usb_tether_stop.sh");

        InitializeApp();
    }

    private File loadScript(String filename) {
        File f = new File(getCacheDir() + "/" + filename);
        try {
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return f;
    }

    void execCommandLine(String command)
    {
        Runtime runtime = Runtime.getRuntime();
        Process proc = null;
        OutputStreamWriter osw = null;

        try
        {
            proc = runtime.exec("su");
            osw = new OutputStreamWriter(proc.getOutputStream());
            osw.write(command);
            osw.flush();
            osw.close();
        }
        catch (IOException ex)
        {
            Log.e("execCommandLine()", "Command resulted in an IO Exception: " + command);
            return;
        }
        finally
        {
            if (osw != null)
            {
                try
                {
                    osw.close();
                }
                catch (IOException e){}
            }
        }

        try
        {
            proc.waitFor();
        }
        catch (InterruptedException e){}

        if (proc.exitValue() != 0)
        {
            Log.e("execCommandLine()", "Command returned error: " + command + "\n  Exit code: " + proc.exitValue());
        }
    }

    public static String fromStream(InputStream in) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
            out.append(newLine);
        }
        return out.toString();
    }

    private String getUsbMode () {
        Runtime rt = Runtime.getRuntime();
        Process proc;
        String mode = null;
        try {
            proc = rt.exec("getprop sys.usb.config");
            mode = fromStream(proc.getInputStream());

            mode = mode.replaceAll("\\r|\\n", "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mode;
    }

    private void InitializeApp()
    {
        final SharedPreferences myPrefs = this.getSharedPreferences("Prefs", MODE_PRIVATE);

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
        String ipString = myPrefs.getString("ip", null);
        String maskString = myPrefs.getString("mask", null);

        if (ipString != null) {
            ip.setText(ipString);
        }

        if (maskString != null) {
            mask.setText(maskString);
        }

        String mode = getUsbMode();
        if (mode.toLowerCase().contains("rndis")) {
            // In RNDIS mode
            Log.i("InitializeApp()", "In RNDIS mode");
            enable.setChecked(true);
        } else {
            Log.i("InitializeApp()", "Not in RNDIS mode");
            enable.setChecked(false);
        }

        enable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Save current entries for next run
                SharedPreferences.Editor prefsEditor = myPrefs.edit();
                prefsEditor.putString("ip", ip.getText().toString());
                prefsEditor.putString("mask", mask.getText().toString());
                prefsEditor.apply();

                // true if the switch is in the On position
                if (isChecked) {
                    prefsEditor.putString("orig_mode", getUsbMode());
                    prefsEditor.apply();
                    execCommandLine("sh " +
                            usbTetherStart.getAbsolutePath() +
                            " " + ip.getText() + "/" + mask.getText() +
                            " " + INTERFACE);
                } else {
                    String originalMode = myPrefs.getString("orig_mode", "adb");
                    execCommandLine("sh " +
                            usbTetherStop.getAbsolutePath() +
                            " " + originalMode +
                            " " + INTERFACE);
                }
            }
        });

        buttonDHCP.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("buttonDhcp", "Attempting to probe for DHCP");
                execCommandLine("netcfg " + INTERFACE + " dhcp");
            }
        });
    }
}
