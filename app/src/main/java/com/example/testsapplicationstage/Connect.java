package com.example.testsapplicationstage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import com.example.testsapplicationstage.objects.Connection;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class Connect extends AppCompatActivity {

    Toast toast;
    EditText id;
    EditText pw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_connect);

        ActionBar ab = this.getSupportActionBar();
        if (ab != null)
            ab.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home)
            NavUtils.navigateUpFromSameTask(this);
        return super.onOptionsItemSelected(item);
    }

    public void connect(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        id = findViewById(R.id.username);
        pw = findViewById(R.id.password);
        String textId = id.getText().toString();
        String textPw = pw.getText().toString();
        if(textId.trim().isEmpty() || textPw.trim().isEmpty()) {
            makeToast("Veuillez remplir tous les champs");
        } else {
            new ConnectionTask(this).execute(textId, textPw);
        }
    }

    class ConnectionTask extends AsyncTask<String, Void, Void> {

        private final WeakReference<Connect> contextRef;

        ConnectionTask(Connect mainActivity) {
            contextRef = new WeakReference<>(mainActivity);
        }

        String token;

        @Override
        protected Void doInBackground(String... strings) {
            Context context = contextRef.get();
            try {
                URL url = new URL("https://dolibarr.siladel.com/api/index.php/login?login=" + strings[0] + "&password="+ strings[1]);
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                String result = CharStreams.toString(new InputStreamReader(https.getInputStream(), Charsets.UTF_8));
                Log.i("TOKEN", "doInBackground: " + result);
                Gson g = new Gson();
                Connection curr = g.fromJson(result, Connection.class);
                token = curr.getSuccess().getToken();
                Log.i("TOKEN", "doInBackground: " + curr.getSuccess().getCode());
                Log.i("TOKEN", "doInBackground: " + token);
                runOnUiThread(() -> makeToast("Connexion réussie"));
                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
            } catch (IOException e) {
                Log.e("Connection", e.getMessage(), e);
                runOnUiThread(() -> makeToast("La connexion a échoué, veuillez vérifier vos identifiants"));
                id.setText("");
                pw.setText("");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            Context context = contextRef.get();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("DOLAPIKEY", token);
            editor.commit();
        }
    }

    public void makeToast(String text) {
        if(toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        toast.show();
    }
}