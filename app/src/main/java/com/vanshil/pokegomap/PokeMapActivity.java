package com.vanshil.pokegomap;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vanshil.pokegomap.network.NianticManager;

import java.util.List;

import butterknife.ButterKnife;

public class PokeMapActivity extends BaseActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener{
    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    private static final int REQ_SIGN_IN_REQUIRED = 55664;
    private String mEmail, mAccountType; // Received from newChooseAccountIntent(); passed to getToken()

    private GoogleMap map = null;
    private SupportMapFragment mapFragment;

    private Circle range;
    private boolean firstLocation;
    private LatLng myLatLng = null;
    Marker myLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poke_map);
        ButterKnife.bind(this);
        firstLocation = true;
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        locationListener = new LocationManager.Listener() {
            @Override
            public void onLocationChanged(Location location) {
                myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                Log.d(TAG, "location ready");
                initWhenReady();

            }
        };
        runSignIn();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setOnMarkerClickListener(this);
        Log.d(TAG, "map ready");
        initWhenReady();
    }
    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    private void initWhenReady(){
        if(map != null && myLatLng != null){
            if(firstLocation){
                myLocation = map.addMarker(new MarkerOptions().position(myLatLng));
                range = map.addCircle(new CircleOptions()
                        .center(myLatLng)
                        .radius(2000)
                        .strokeColor(getResources().getColor(R.color.colorAccent))
                        .fillColor(getResources().getColor(R.color.yelllow_alpha)));
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 12));
                firstLocation = false;
            }
        }
    }




    private void runSignIn() {
        nianticManager = NianticManager.getInstance();
        nianticManager.login(getString(R.string.username), getString(R.string.password));
    }

    private void loadPokemon(){

    }

    private void renderMarkers(List<SimplePokemon> pokemonList){
        for(SimplePokemon pokemon: pokemonList){

            int resourceID = getResources().getIdentifier("" + pokemon.getID(), "drawable", getPackageName());

            map.addMarker(new MarkerOptions()
                            .position(pokemon.getLatLng())
                            .icon(BitmapDescriptorFactory.fromResource(resourceID))
                            .snippet(pokemon.getName()));
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

}
