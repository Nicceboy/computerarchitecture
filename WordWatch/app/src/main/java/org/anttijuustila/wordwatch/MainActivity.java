package org.anttijuustila.wordwatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.anttijuustila.keywordclient.KeywordAPI;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Runnable {

    private Button addButton = null;
    private Button connectButton = null;
    private Button disconnectButton = null;
    private KeywordWatcher watcher = null;
    private EditText alertText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        watcher = new KeywordWatcher(this, this);
        try {
            watcher.initialize();
        } catch (KeywordAPI.KeywordAPIException e) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.cannot_intialize_watcher),
                    Toast.LENGTH_LONG).show();
        }

        ArrayAdapter adapter = new ArrayAdapter<String>(this, R.layout.list_row_layout, watcher.keywords());
        ListView list = (ListView) findViewById(R.id.listWords);
        list.setAdapter(adapter);
        list.requestFocus();

        alertText = (EditText)findViewById(R.id.textAlerts);

        addButton = (Button) findViewById(R.id.buttonAdd);
        addButton.setOnClickListener(this);
        connectButton = (Button) findViewById(R.id.buttonConnect);
        connectButton.setOnClickListener(this);
        disconnectButton = (Button) findViewById(R.id.buttonDisconnect);
        disconnectButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            connectToServer();
        } catch (KeywordAPI.KeywordAPIException e) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.cannot_connect_watcher),
                    Toast.LENGTH_LONG).show();
        }
        updateButtonState();
    }

    @Override
    protected void onStop() {
        super.onStop();

        watcher.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == addButton.getId()) {
            EditText wordEditor = (EditText)findViewById(R.id.editorWord);
            String word = wordEditor.getText().toString();
            if (word.length() > 0) {
                try {
                    watcher.watchKeyword(word);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                wordEditor.setText("");
                ListView list = (ListView) findViewById(R.id.listWords);
                ((ArrayAdapter<String>)list.getAdapter()).notifyDataSetChanged();

            }
        } else if (view.getId() == connectButton.getId()) {
            try {
                connectToServer();
                alertText.setText(R.string.status_connected);
            } catch (KeywordAPI.KeywordAPIException e) {
                e.printStackTrace();
                alertText.setText(e.getMessage());
            }
        } else if (view.getId() == disconnectButton.getId()) {
            watcher.disconnect();
            alertText.setText(R.string.status_disconnected);
        }
        updateButtonState();
    }

    private void updateButtonState() {
        if (watcher.isConnected()) {
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
        } else {
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
        }
    }

    // TODO: add method watchKeywords and get the keywords from the listbox if any, when connected
    // and start watching them immediately
    private void connectToServer() throws KeywordAPI.KeywordAPIException {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String serverAddr = sharedPref.getString("server_addr_text", getString(R.string.pref_default_server_address));
        String serverPath = sharedPref.getString("server_path_text", getString(R.string.pref_default_server_path));
        watcher.connect(serverAddr, serverPath);
    }

    @Override
    public void run() {
        try {
            String alert = "";
            while (watcher.hasAlerts()) {
                alert += watcher.getLatestAlert() + "\n";
            }
            alertText.getText().append(alert);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        updateButtonState();
    }

}
