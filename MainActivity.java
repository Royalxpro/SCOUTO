package com.kaoshik.scouto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Spinner carMakeSpinner;
    private Spinner carModelSpinner;
    private Button addCarButton;
    private ListView carListView;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    private ArrayList<Car> carsList;
    private CarListAdapter carListAdapter;
    private ArrayList<String> carMakes = new ArrayList<>();
    private ArrayList<String> carModel = new ArrayList<>();

    private String selectedCarMake;
    private String selectedCarModel;
    private String carImageURL;

    private final String CARQUERY_API_URL = "https://www.carqueryapi.com/api/0.3/";
    private final String CARQUERY_API_KEY = "YOUR_API_KEY";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        carMakeSpinner = findViewById(R.id.car_make_spinner);
        carModelSpinner = findViewById(R.id.car_model_spinner);
        addCarButton = findViewById(R.id.add_car_button);
        carListView = findViewById(R.id.cars_list_view);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        databaseReference = FirebaseDatabase.getInstance().getReference().child("users").child(firebaseUser.getUid()).child("cars");

        carsList = new ArrayList<>();
        carListAdapter = new CarListAdapter(this, carsList);
        carListView.setAdapter(carListAdapter);


        // Get Car Makes from API
        String url = "https://www.carqueryapi.com/api/0.3/?cmd=getMakes";
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray makesArray = response.getJSONArray("Makes");
                            for (int i = 0; i < makesArray.length(); i++) {
                                JSONObject makeObj = makesArray.getJSONObject(i);
                                String make = makeObj.getString("make_display");
                                carMakes.add(make);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }
        );

        queue.add(jsonObjectRequest);







        carMakeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedCarMake = adapterView.getItemAtPosition(i).toString();
                loadCarModels();


            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });




        carModelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedCarModel= adapterView.getItemAtPosition(i).toString();
               
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        addCarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addCar();
            }
        });



        carListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                deleteCar(i);
                return true;
            }
        });
    }

    private void loadCarModels() {
        String url = CARQUERY_API_URL + "?cmd=getModels&make=" + selectedCarMake + "&sold_in_us=1&key=" + CARQUERY_API_KEY;

        HttpHandler httpHandler = new HttpHandler();
        String response = httpHandler.makeServiceCall(url);

        if (response != null) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("Models");

                ArrayList<String> carModelList = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject modelObject = jsonArray.getJSONObject(i);
                    String modelName = modelObject.getString("model_name");
                    carModelList.add(modelName);
                }

                ArrayAdapter<String> carModelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, carModelList);
                carModelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                carModelSpinner.setAdapter(carModelAdapter);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }



    private void addCar() {
        if (selectedCarMake == null || selectedCarModel == null) {
            Toast.makeText(this, "Please select car make and model", Toast.LENGTH_SHORT).show();
            return;
        }

        if (carImageURL == null) {
            Toast.makeText(this, "Please upload car image", Toast.LENGTH_SHORT).show();
            return;
        }

        Car car = new Car(selectedCarMake, selectedCarModel, carImageURL);

        String carId = databaseReference.push().getKey();
        databaseReference.child(carId).setValue(car);

        carsList.add(car);
        carListAdapter.notifyDataSetChanged();

        Toast.makeText(this, "Car added successfully", Toast.LENGTH_SHORT).show();
    }

    private void deleteCar(int position) {
        Car car = carsList.get(position);
        String carId = car.getId();

        databaseReference.child(carId).removeValue();

        carsList.remove(position);
        carListAdapter.notifyDataSetChanged();

        Toast.makeText(this, "Car deleted successfully", Toast.LENGTH_SHORT).show();
    }

    public void uploadCarImage(View view) {
        // Code to upload car image to Firebase Storage and get the download URL
        // Once the upload is complete, set the carImageURL variable with the download URL
    }
}
