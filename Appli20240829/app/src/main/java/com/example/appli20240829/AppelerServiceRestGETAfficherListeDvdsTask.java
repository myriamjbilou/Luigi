package com.example.appli20240829;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class AppelerServiceRestGETAfficherListeDvdsTask extends AsyncTask<String, Void, ArrayList<String>> {

    private final AfficherListeDvdsActivity activity;

    public AppelerServiceRestGETAfficherListeDvdsTask(AfficherListeDvdsActivity activity) {
        this.activity = activity;
    }

    @Override
    protected ArrayList<String> doInBackground(String... urls) {
        ArrayList<String> listeDvds = new ArrayList<>();
        try {
            // Vérifiez si l'URL est bien reçue
            String urlString = urls[0];
            Log.d("mydebug", "URL reçue pour l'appel API : " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(5000); // Timeout de connexion
            urlConnection.setReadTimeout(5000);    // Timeout de lecture

            // Vérifiez le code de réponse HTTP
            int responseCode = urlConnection.getResponseCode();
            Log.d("mydebug", "Code de réponse HTTP : " + responseCode);

            // Si la réponse n'est pas OK (200), renvoyez une erreur
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("mydebug", "Erreur lors de l'appel API : Code de réponse " + responseCode);
                return null;
            }

            Log.d("mydebug", "Connexion établie, lecture des données...");

            // Lecture des données
            InputStreamReader reader = new InputStreamReader(urlConnection.getInputStream());
            int data = reader.read();
            StringBuilder stringBuilder = new StringBuilder();
            while (data != -1) {
                char current = (char) data;
                stringBuilder.append(current);
                data = reader.read();
            }
            String result = stringBuilder.toString();
            Log.d("mydebug", "Données reçues : " + result);

            // Parsing du JSON
            JSONArray jsonArray = new JSONArray(result);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject film = jsonArray.getJSONObject(i);
                int inventoryId = film.getInt("inventoryId");
                int filmId = film.getInt("filmId");
                String title = film.getString("title");

                String dvdInfo = "Inventory ID : " + inventoryId
                        + "\nFilm ID : " + filmId
                        + "\nTitre : " + title;
                listeDvds.add(dvdInfo);
            }

        } catch (Exception e) {
            Log.e("mydebug", "Erreur d'appel à l'API", e);
        }
        return listeDvds;
    }

    @Override
    protected void onPostExecute(ArrayList<String> listeDvds) {
        // Récupérer les vues nécessaires
        TextView texteChargement = activity.findViewById(R.id.texteChargement);
        ProgressBar barreDeProgression = activity.findViewById(R.id.barreDeProgression);
        ListView listeDvdsView = activity.findViewById(R.id.listeDvds);

        // Cacher la barre de progression et afficher la liste
        barreDeProgression.setVisibility(View.GONE);
        texteChargement.setVisibility(View.GONE);
        listeDvdsView.setVisibility(View.VISIBLE);

        // Afficher les films dans la liste
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, listeDvds);
        listeDvdsView.setAdapter(adapter);
    }
}
