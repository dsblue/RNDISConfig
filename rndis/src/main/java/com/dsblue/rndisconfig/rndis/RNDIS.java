package com.dsblue.rndisconfig.rndis;

import android.support.v7.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

        // Load defaults from previous run of the App
        String ipString = myPrefs.getString("ip", null);
        String maskString = myPrefs.getString("mask", null);

        if (ipString != null) {
            ip.setText(ipString);
        }

        if (maskString != null) {
            mask.setText(maskString);
        }

        Switch enable = (Switch) findViewById(R.id.rndis_switch);
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
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                // Save current entries for next run
                SharedPreferences.Editor prefsEditor = myPrefs.edit();
                prefsEditor.putString("ip", ip.getText().toString());
                prefsEditor.putString("mask", mask.getText().toString());
                prefsEditor.apply();

                // true if the switch is in the On position
                if (isChecked) {
                    execCommandLine("sh " +
                            usbTetherStart.getAbsolutePath() +
                            " " + ip.getText() + "/" + mask.getText() +
                            INTERFACE);
                } else {
                    execCommandLine("sh " + usbTetherStop.getAbsolutePath() + " adb " + INTERFACE);
                }
            }
        });
    }
}
