package com.example.appli20240829;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AfficherPanierActivity extends AppCompatActivity {
    private ListView listePanierView;
    private TextView panierVideText;
    private Button btnValiderPanier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_afficher_panier);

        Log.d("PanierDebug", "AfficherPanierActivity onCreate");

        listePanierView = findViewById(R.id.listePanier);
        panierVideText = findViewById(R.id.panierVideText);
        btnValiderPanier = findViewById(R.id.btnValiderPanier);

        afficherFilmsDansPanier();

        btnValiderPanier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validerPanierFinal();
            }
        });
    }

    private void afficherFilmsDansPanier() {
        List<String> filmsDansPanier = PanierManager.getInstance().getFilmsDansPanier();

        if (filmsDansPanier.isEmpty()) {
            listePanierView.setVisibility(View.GONE);
            panierVideText.setVisibility(View.VISIBLE);
            btnValiderPanier.setVisibility(View.GONE);
        } else {
            listePanierView.setVisibility(View.VISIBLE);
            panierVideText.setVisibility(View.GONE);
            btnValiderPanier.setVisibility(View.VISIBLE);

            Log.d("PanierDebug", "Nombre de films dans le panier : " + filmsDansPanier.size());
            Log.d("PanierDebug", "Contenu du panier : " + filmsDansPanier.toString());

            PanierAdapter adapter = new PanierAdapter(this, filmsDansPanier);
            listePanierView.setAdapter(adapter);
        }
    }


    private void validerPanierFinal() {
        Log.d("PanierDebug", "Validation du panier déclenchée");

        List<String> panier = PanierManager.getInstance().getFilmsDansPanier();
        Log.d("PanierDebug", "Panier avant validation : " + panier.toString());
        if (panier.isEmpty()) {
            Toast.makeText(this, "Le panier est vide.", Toast.LENGTH_SHORT).show();
            return;
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                for (String filmString : panier) {
                    int inventoryId = parseInventoryId(filmString);
                    if (inventoryId == -1) {
                        Log.e("PanierDebug", "Erreur de parsing pour : " + filmString);
                        continue;
                    }
                    // Remplacez customerId par l'ID de l'utilisateur connecté
                    int customerId = 1;
                    // Construire l'URL d'appel de location en utilisant la même méthode que LoginActivity
                    String urlStr = DonneesPartagees.getURLConnexion()
                            + "/toad/rental/rent?inventory_id=" + inventoryId
                            + "&customer_id=" + customerId;
                    Log.d("PanierDebug", "Appel de l'URL : " + urlStr);
                    try {
                        URL url = new URL(urlStr);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(5000);
                        int responseCode = conn.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            Log.d("PanierDebug", "DVD " + inventoryId + " loué avec succès !");
                            runOnUiThread(() -> Toast.makeText(AfficherPanierActivity.this, "DVD " + " loué avec succès !", Toast.LENGTH_SHORT).show());
                        } else if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
                            Log.e("PanierDebug", "DVD " + inventoryId + " déjà loué !");
                            runOnUiThread(() -> Toast.makeText(AfficherPanierActivity.this, "Le DVD " + " est déjà loué. Veuillez en choisir un autre.", Toast.LENGTH_LONG).show());
                        } else {
                            Log.e("PanierDebug", "Erreur " + responseCode + " pour DVD " + inventoryId);
                            runOnUiThread(() -> Toast.makeText(AfficherPanierActivity.this, "Erreur serveur pour le DVD " + inventoryId, Toast.LENGTH_SHORT).show());
                        }

                        conn.disconnect();
                    } catch (Exception e) {
                        Log.e("PanierDebug", "Erreur lors de l'appel pour DVD " + inventoryId, e);
                    }
                }
                // Une fois tous les appels effectués, vider le panier et rafraîchir l'affichage sur le thread UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PanierManager.getInstance().viderPanier();
                        afficherFilmsDansPanier();
                    }
                });
            }
        });
    }

    /**
     * Extrait l'inventoryId d'une chaîne formatée : "42 - Titre : ..."
     */
    private int parseInventoryId(String filmDetails) {
        try {
            // Chaque ligne séparée par un retour à la ligne
            String[] lignes = filmDetails.split("\n");
            for (String ligne : lignes) {
                if (ligne.startsWith("Inventory ID")) {
                    // Extrait juste le nombre
                    String[] parts = ligne.split(":");
                    return Integer.parseInt(parts[1].trim());
                }
            }
        } catch (Exception e) {
            Log.e("PanierDebug", "Impossible de parser l'inventoryId pour: " + filmDetails, e);
        }
        return -1; // Valeur d'erreur
    }

}
