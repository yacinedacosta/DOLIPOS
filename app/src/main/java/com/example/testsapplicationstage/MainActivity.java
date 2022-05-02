package com.example.testsapplicationstage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.testsapplicationstage.objects.Product;
import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    RecyclerView lv;
    RecyclerView.Adapter adapter;
    RecyclerView.LayoutManager layoutManager;

    Menu menuAccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main_activity);
        lv = findViewById(R.id.lv);
        File file = new File(getFilesDir(), "data.json");
        if (file.exists())
            showList();
        else
            downloadAndShowList();
        SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(() -> {
            refresh();
            pullToRefresh.setRefreshing(false);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        menuAccess = menu;
        menu.findItem(R.id.connection).setVisible(sharedPref.getString("DOLAPIKEY", null) == null);
        menu.findItem(R.id.disconnection).setVisible((sharedPref.getString("DOLAPIKEY", null) != null));
        menu.findItem(R.id.transparentWhenNoStock).setChecked(sharedPref.getBoolean("TransparentNoStock", false));
        return super.onPrepareOptionsMenu(menu);
    }

    void refresh() {
        if (isNetworkAvailable())
            downloadAndShowList();
        else
            Toast.makeText(this, "Pas de connexion Internet disponible.", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        switch (item.getItemId()) {
            case R.id.connection:
                startActivity(new Intent(this, Connect.class));
                invalidateOptionsMenu();
                menuAccess.findItem(R.id.connection).setVisible(false);
                menuAccess.findItem(R.id.disconnection).setVisible(true);
                break;
            case R.id.disconnection:
                editor.remove("DOLAPIKEY");
                invalidateOptionsMenu();
                menuAccess.findItem(R.id.connection).setVisible(true);
                menuAccess.findItem(R.id.disconnection).setVisible(false);
                editor.apply();
            case R.id.refresh:
                refresh();
                break;
            case R.id.transparentWhenNoStock:
                item.setChecked(!item.isChecked());
                editor.putBoolean("TransparentNoStock", item.isChecked());
                editor.apply();
                showList();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void showList() {
        Gson g = new Gson();
        try {
            InputStreamReader file = new InputStreamReader(this.openFileInput("data.json"));
            StringBuilder sb = new StringBuilder();
            char[] dataBuffer = new char[1024];
            int bytesRead;
            while ((bytesRead = file.read(dataBuffer, 0, 1024)) != -1) {
                sb.append(dataBuffer, 0, bytesRead);
            }
            lv.setHasFixedSize(true);

            layoutManager = new LinearLayoutManager(this);
            lv.setLayoutManager(layoutManager);
            List<Product> al = Arrays.asList(g.fromJson(sb.toString(), Product[].class));
            List<Product> alStock = new ArrayList<>();
            for (Product curr:
                 al) {
                if (curr.getStock_reel() != 0)
                    alStock.add(curr);
            }
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            adapter = new ProductsCustomAdapter(sharedPref.getBoolean("TransparentNoStock", false) ? al : alStock);
            lv.setAdapter(adapter);
        } catch (FileNotFoundException e) {
            //Unreachable
        } catch (IOException e) {
            Log.e("AsyncWrite", e.getMessage(), e);
        }
    }

    private void downloadAndShowList() {
        new DownloadJsonFileTask(this).execute();
        showList();
    }

    static class DownloadJsonFileTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<MainActivity> contextRef;

        DownloadJsonFileTask(MainActivity mainActivity) {
            contextRef = new WeakReference<>(mainActivity);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity context = contextRef.get();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String apiKey = sharedPref.getString("DOLAPIKEY", null);
            if (apiKey == null) {
                context.runOnUiThread(() -> new AlertDialog.Builder(context)
                        .setTitle("Aucune connexion")
                        .setMessage("Aucun utilisateur connecté.\nVeuillez vous connecter via la page de configuration")
                        .setNeutralButton("OK", null)
                        .setPositiveButton("Y Aller", (dialogInterface, i) -> {
                            Intent intent = new Intent(context, Connect.class);
                            context.startActivity(intent);
                        })
                        .show()
                );
                return null;
            } else {
                try {
                    URL url = new URL("https://dolibarr.siladel.com/api/index.php/products");
                    HttpURLConnection http = (HttpURLConnection) url.openConnection();
                    http.setRequestProperty("Accept", "application/json");
                    http.setRequestProperty("DOLAPIKEY", apiKey);
                    if (isCancelled())
                        return null;
                    Log.i("AsyncDL", http.getResponseCode() + " " + http.getResponseMessage());
                    BufferedInputStream bis = new BufferedInputStream(http.getInputStream());
                    FileOutputStream fos = context.openFileOutput("data.json", Context.MODE_PRIVATE);
                    fos.write(32);
                    fos.close();
                    fos = context.openFileOutput("data.json", Context.MODE_APPEND);
                    byte[] dataBuffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = bis.read(dataBuffer, 0, 1024)) != -1) {
                        if (isCancelled())
                            return null;
                        fos.write(dataBuffer, 0, bytesRead);
                    }
                    http.disconnect();
                    fos.close();
                    bis.close();
                    return null;
                } catch (IOException e) {
                    Log.e("AsyncDL", e.getMessage(), e);
                    return null;
                }
            }
        }
    }
}

