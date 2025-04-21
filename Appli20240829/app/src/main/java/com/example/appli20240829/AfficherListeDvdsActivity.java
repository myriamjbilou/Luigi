package com.example.appli20240829;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.Intent;
import android.widget.Button;

public class AfficherListeDvdsActivity extends AppCompatActivity {

    private ListView listeDvdsView;
    private ProgressBar barreDeProgression;
    private TextView texteChargement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_afficher_liste_dvds);

        // Initialisation des vues
        listeDvdsView = findViewById(R.id.listeDvds);
        barreDeProgression = findViewById(R.id.barreDeProgression);
        texteChargement = findViewById(R.id.texteChargement);

        // 🔹 Ajout du bouton "Voir Panier"
        Button btnVoirPanier = findViewById(R.id.btnVoirPanier);
        btnVoirPanier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("PanierDebug", "Bouton Voir Panier cliqué !");
                Intent intent = new Intent(AfficherListeDvdsActivity.this, AfficherPanierActivity.class);
                startActivity(intent);
            }
        });

        // Afficher la barre de progression et masquer la liste
        barreDeProgression.setVisibility(View.VISIBLE);
        texteChargement.setVisibility(View.VISIBLE);
        listeDvdsView.setVisibility(View.GONE);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                final ArrayList<String> listeDvds = appelerApi(
                        DonneesPartagees.getURLConnexion() + "/toad/inventory/available/details"
                );

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AfficherListeDvdsActivity.this, "Données reçues et prêtes à afficher", Toast.LENGTH_SHORT).show();
                        afficherListe(listeDvds);
                    }
                });
            }
        });
    }


    private ArrayList<String> appelerApi(String url) {
        ArrayList<String> listeDvds = new ArrayList<>();
        try {
            // Connexion à l'API
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // Vérifie le code de réponse
            int responseCode = connection.getResponseCode();
            Log.d("API_DEBUG", "Code de réponse : " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Lire la réponse de l'API
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d("API_DEBUG", "Réponse brute JSON : " + response.toString());

                // Traitement du JSON
                JSONArray jsonArray = new JSONArray(response.toString());
                Log.d("API_DEBUG", "Nombre d'exemplaires reçus : " + jsonArray.length());

                // 🔁 Map pour stocker un seul exemplaire dispo par filmId
                Map<Integer, JSONObject> filmParId = new HashMap<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject film = jsonArray.getJSONObject(i);
                    int filmId = film.getInt("filmId");

                    // DEBUG : log les doublons
                    Log.d("DEBUG_FILM", "filmId = " + filmId + ", inventoryId = " + film.getInt("inventoryId") + ", title = " + film.getString("title"));

                    // Si le filmId n’est pas encore stocké, on ajoute le premier exemplaire dispo
                    if (!filmParId.containsKey(filmId)) {
                        filmParId.put(filmId, film);
                    }
                }

                // 🧾 Construction de la liste finale à afficher
                for (JSONObject film : filmParId.values()) {
                    int inventoryId = film.getInt("inventoryId");
                    int filmId = film.getInt("filmId");
                    String title = film.getString("title");

                    String dvdInfo = "Inventory ID : " + inventoryId
                            + "\nFilm ID : " + filmId
                            + "\nTitre : " + title;

                    listeDvds.add(dvdInfo);
                }

            } else {
                Log.e("API_DEBUG", "Erreur HTTP : " + responseCode);
            }

        } catch (Exception e) {
            Log.e("API_DEBUG", "Erreur lors de l'appel API", e);
        }

        return listeDvds;
    }


    private void afficherListe(ArrayList<String> listeDvds) {
        // Vérifie si la liste est vide et log le nombre de films
        Log.d("AfficherListeDvdsActivity", "Nombre de films récupérés : " + listeDvds.size());

        // Cacher la barre de progression et afficher la liste
        barreDeProgression.setVisibility(View.GONE);
        texteChargement.setVisibility(View.GONE);
        listeDvdsView.setVisibility(View.VISIBLE);

        // Afficher les films dans la liste
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listeDvds);
        listeDvdsView.setAdapter(adapter);

        // Click court : Afficher les détails du film
        listeDvdsView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(AfficherListeDvdsActivity.this, AfficherDetailDvdsActivity.class);
            intent.putExtra("DVD_DETAILS", listeDvds.get(position));
            startActivity(intent);
        });

        // Click long : Ajouter au panier
        listeDvdsView.setOnItemLongClickListener((parent, view, position, id) -> {
            String filmDetails = listeDvds.get(position);
            PanierManager.getInstance().ajouterAuPanier(filmDetails);

            // Afficher une confirmation
            Toast.makeText(AfficherListeDvdsActivity.this,
                    "Film ajouté au panier",
                    Toast.LENGTH_SHORT).show();

            return true; // Consomme l'événement
        });

        // Configuration du bouton Voir Panier
        Button btnVoirPanier = findViewById(R.id.btnVoirPanier);
        btnVoirPanier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AfficherListeDvdsActivity.this, AfficherPanierActivity.class);
                startActivity(intent);
            }
        });
    }
}
