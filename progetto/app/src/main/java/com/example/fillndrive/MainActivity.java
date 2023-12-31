package com.example.fillndrive;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.fillndrive.databinding.ActivityMainBinding;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private DBHelper db;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private static SharedPreferences preferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        db = DBHelper.getInstance(this);

        int currentDay = DateUtility.getCurrentDay(this);
        int lastDay = DateUtility.getLastDay(this);

        // Verifica se la data attuale è diversa dall'ultima data registrata aggiorna i dati del db
        if(lastDay != currentDay){

            try {
                // Esegue l'operazione di aggiornamento dei dati
                db.updateData();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        //prova a prelevare le preferenze in cache
        preferences = getSharedPreferences("fuelAndService", Context.MODE_PRIVATE);

        //se le preferenze sono impostate allora carica la MapsActivity altrimenti prosegue
        if(preferences.getBoolean("cache", false)){
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        }

        //inflate del layout scelta carburante e tipo di servizio
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        SharedPreferences.Editor editor = preferences.edit();

        setChecked();
        
        // Listener per la selezione del carburante
        binding.fuelRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int fuelId) {

                RadioButton selectedButton = findViewById(fuelId);
                String buttonText = selectedButton.getText().toString();

                //Salva o aggiorna la preferenza sul carburante
                switch (buttonText){
                    case "Diesel":
                        editor.putString("fuel", "Gasolio%"); //Blue Diesel?s
                        break;
                    case "GPL":
                        editor.putString("fuel", "GPL%");
                        break;
                    case "Metano":
                        editor.putString("fuel", "Metano%");
                        break;

                    default:
                        editor.putString("fuel", "Benzina%");
                        break;
                }
                editor.putBoolean("cache", true);
                editor.apply();
            }
        });
        // Listener per lo switch dell'opzione Self_service
        binding.serviceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                //Salva o aggiorna la preferenza sul tipo di servizio
                if(isChecked){
                    //lo switch è checked (di default) -> isSelf = true
                    editor.putInt("self", 1);
                }
                else{
                    editor.putInt("self", 0);
                }
                editor.putBoolean("cache", true);
                editor.apply();
            }
        });

        binding.cercaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setChecked() {
        String fuel = preferences.getString("fuel", "Benzina%");
        fuel = fuel.replace("%", "");

        int flag = getPreferences().getInt("self", 1);

        CompoundButton cp = findViewById(R.id.serviceSwitch);

        // Array di RadioButton
        RadioButton[] radioButtons = new RadioButton[4];
        radioButtons[0] = findViewById(R.id.radioButton1);
        radioButtons[1] = findViewById(R.id.radioButton2);
        radioButtons[2] = findViewById(R.id.radioButton3);
        radioButtons[3] = findViewById(R.id.radioButton4);

        // Nomi dei carburanti corrispondenti ai RadioButton
        String[] fuelNames = {"Benzina", "Gasolio", "GPL", "Metano"};

        cp.setChecked(flag != 0);

        // Ciclo per cercare il carburante corrispondente e impostare il RadioButton
        for (int i = 0; i < radioButtons.length; i++) {
            if (fuel.equals(fuelNames[i])) {
                radioButtons[i].setChecked(true);
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {

            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void EmailButton(View view){
        String mailto = "mailto:supportfillndrive@gmail.com";
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse(mailto));

        try {
            startActivity(emailIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(MainActivity.this, "Error to open email app", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public static SharedPreferences getPreferences() {
        return preferences;
    }
}
