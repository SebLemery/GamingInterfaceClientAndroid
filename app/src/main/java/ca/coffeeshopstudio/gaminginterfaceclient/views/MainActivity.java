package ca.coffeeshopstudio.gaminginterfaceclient.views;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import ca.coffeeshopstudio.gaminginterfaceclient.App;
import ca.coffeeshopstudio.gaminginterfaceclient.R;
import ca.coffeeshopstudio.gaminginterfaceclient.models.screen.IScreen;
import ca.coffeeshopstudio.gaminginterfaceclient.models.screen.IScreenRepository;
import ca.coffeeshopstudio.gaminginterfaceclient.models.screen.ScreenRepository;
import ca.coffeeshopstudio.gaminginterfaceclient.network.CommandService;
import ca.coffeeshopstudio.gaminginterfaceclient.network.RestClientInstance;
import ca.coffeeshopstudio.gaminginterfaceclient.utils.CryptoHelper;
import ca.coffeeshopstudio.gaminginterfaceclient.views.launch.SplashIntroActivity;
import ca.coffeeshopstudio.gaminginterfaceclient.views.screenmanager.ScreenManagerActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 Copyright [2019] [Terence Doerksen]

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    public static final String INTENT_SCREEN_INDEX = "screen_index";
    private static final String PREFS_CHOSEN_ID = "chosen_id";
    public static final int REQUEST_CODE_INTRO = 65;
    private SparseArray<String> screenList;
    private Spinner spinner;
    private static final String PREF_KEY_FIRST_START = "prefSplash";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (((App) getApplication()).isNightModeEnabled())
            setTheme(R.style.ActivityTheme_Primary_Base_Dark);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        boolean firstStart = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(PREF_KEY_FIRST_START, true);

        if (firstStart) {
            Intent intent = new Intent(this, SplashIntroActivity.class);
            startActivityForResult(intent, REQUEST_CODE_INTRO);
        }

        toolbar.setTitle(R.string.app_name);

        buildControls();

        loadSettings();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_INTRO) {
            if (resultCode == RESULT_OK) {
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putBoolean(PREF_KEY_FIRST_START, false)
                        .apply();
            } else {
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putBoolean(PREF_KEY_FIRST_START, true)
                        .apply();
            }
            new ScreenRepository(this).init();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.menu_about:
                MainActivity.this.startActivity(new Intent(MainActivity.this, AboutActivity.class));
                return true;
            case R.id.menu_help:
                String url = "https://github.com/Terence-D/GamingInterfaceClientAndroid/wiki";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return true;
            case R.id.menu_toggle_theme:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor prefsEditor = prefs.edit();
                prefsEditor.putBoolean("NIGHT_MODE", !prefs.getBoolean("NIGHT_MODE", true));
                prefsEditor.apply();
                recreate();
                break;
            case R.id.menu_show_intro:
                Intent intent = new Intent(this, SplashIntroActivity.class);
                startActivityForResult(intent, REQUEST_CODE_INTRO);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void buildControls() {
        findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startApp();
            }
        });
        findViewById(R.id.btnScreenManager).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.startActivity(new Intent(MainActivity.this, ScreenManagerActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        final ScreenRepository screenRepository = new ScreenRepository(getApplicationContext());
        screenRepository.loadScreens(new IScreenRepository.LoadCallback() {
            @Override
            public void onLoaded(List<IScreen> screens) {
                screenRepository.getScreenList(new IScreenRepository.LoadScreenListCallback() {
                    @Override
                    public void onLoaded(SparseArray<String> screenList) {
                        buildScreenSpinner(screenList);
                    }
                });
            }
        });
    }

    private void buildScreenSpinner(SparseArray<String> screenList) {
        this.screenList = screenList;

        spinner = findViewById(R.id.spnScreens);

        String[] spinnerArray = new String[screenList.size()];
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(ScreenRepository.PREFS_NAME, MODE_PRIVATE);
        int chosenId = prefs.getInt(PREFS_CHOSEN_ID, 0);
        int chosenIndex = 0;

        for (int i = 0; i < screenList.size(); i++) {
            spinnerArray[i] = screenList.valueAt(i);
            if (screenList.keyAt(i) == chosenId)
                chosenIndex = i;
        }

        ArrayAdapter<CharSequence> dataAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_dropdown_item, spinnerArray);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(dataAdapter);
        spinner.setSelection(chosenIndex);
        spinner.setOnItemSelectedListener(this);
    }

    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        if (str.isEmpty()) {
            return false;
        }
        int i = 0;
        int length = str.length();

        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private void checkServerVersion(String address, String port, final Intent myIntent) {
        String url = "http://" + address + ":" + port + "/";

        CommandService routeMap = RestClientInstance.getRetrofitInstance(url).create(CommandService.class);
        Call<String> version = routeMap.getVersion();
        version.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    if (response.body().equals("1.3.0.0")) {
                        int screenIndex = screenList.keyAt(spinner.getSelectedItemPosition());
                        myIntent.putExtra(MainActivity.INTENT_SCREEN_INDEX, screenIndex);
                        MainActivity.this.startActivity(myIntent);
                        return;
                    }
                    displayUpgradeWarning();
                } else {
                    Toast.makeText(getApplicationContext(), response.message(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Something went wrong...Please try later! " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayUpgradeWarning() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(R.string.activity_main_server_upgrade_title);
        alertDialog.setMessage(getString(R.string.activity_main_server_upgrade_text));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void startApp() {
        TextView txtPassword = findViewById(R.id.txtPassword);
        TextView txtPort = findViewById(R.id.txtPort);
        TextView txtAddress = findViewById(R.id.txtAddress);
        String password = txtPassword.getText().toString();
        String port = txtPort.getText().toString();
        String address = txtAddress.getText().toString();

        if (password.length() < 6) {
            Toast.makeText(this, R.string.password_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isInteger(port)) {
            Toast.makeText(this, R.string.port_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        if (address.length() < 7) {
            Toast.makeText(this, R.string.address_invalid, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            password = CryptoHelper.encrypt(txtPassword.getText().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (password.isEmpty()) {
            Toast.makeText(this, R.string.password_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        port = port.replaceFirst("\\s++$", "");
        address = address.replaceFirst("\\s++$", "");

        Intent myIntent = new Intent(MainActivity.this, GameActivity.class);
        myIntent.putExtra("address", address);
        myIntent.putExtra("port", port);
        myIntent.putExtra("password", password);

        saveSettings(password, port, address);

        checkServerVersion(address, port, myIntent);
    }

    private void saveSettings(String password, String port, String address) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("gics", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();

        prefsEditor.putString("address", address);
        prefsEditor.putString("port", port);
        prefsEditor.putString("password", password);

        prefsEditor.apply();
    }

    private void loadSettings() {
        TextView txtPassword = findViewById(R.id.txtPassword);
        TextView txtPort = findViewById(R.id.txtPort);
        TextView txtAddress = findViewById(R.id.txtAddress);

        SharedPreferences prefs = getApplicationContext().getSharedPreferences("gics", MODE_PRIVATE);

        String password = prefs.getString("password", "");
        try {
            password = CryptoHelper.decrypt(password);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (password == null) {
            Log.d("GIC", "start: Password Decryption Failure");
        }

        String address = prefs.getString("address", "");
        String port = prefs.getString("port", "8091");
        txtPassword.setText(password);
        txtPort.setText(port);
        txtAddress.setText(address);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(ScreenRepository.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        try {
            prefsEditor.putInt(PREFS_CHOSEN_ID, screenList.keyAt(screenList.indexOfKey(i)));
            prefsEditor.apply();
        } catch (Exception e) {

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }
}
