package com.vanshil.pokegomap.network;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by vanshilshah on 20/07/16.
 */
public class NianticManager {
    private static final String TAG = "NianticManager";
    private static final String BASE_URL = "https://sso.pokemon.com/sso/";
    private static final String LOGIN_URL = "https://sso.pokemon.com/sso/login?service=https://sso.pokemon.com/sso/oauth2.0/callbackAuthorize";
    private static final String LOGIN_OAUTH = "https://sso.pokemon.com/sso/oauth2.0/accessToken";
    private static final String PTC_CLIENT_SECRET = "w8ScCUXJQc6kXKw8FiOhd8Fixzht18Dq3PEVkUCP5ZPxtgyWsbTvWHFLm2wNY0JR";
    public static final String CLIENT_ID = "mobile-app_pokemon-go";
    public static final String REDIRECT_URI = "https://www.nianticlabs.com/pokemongo/error";

    private static NianticManager instance;

    private List<Listener> listeners;

    NianticService nianticService;
    final OkHttpClient client;
    public static NianticManager getInstance(){
        if(instance == null){
            instance = new NianticManager();
        }
        return instance;
    }

    private NianticManager(){
        listeners = new ArrayList<>();

        client = new OkHttpClient.Builder()
            .hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            })
            .addInterceptor(new LoggingInterceptor())
            .followRedirects(false)
            .followSslRedirects(false)
            .build();

        nianticService = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(NianticService.class);
    }
    public void login(final String username, final String password){

    }

    private void retrofitLogin(final String username, final String password){
        Callback<NianticService.InitialResponse> initialCallback = new Callback<NianticService.InitialResponse>() {
            @Override
            public void onResponse(Call<NianticService.InitialResponse> call, Response<NianticService.InitialResponse> response) {
                retrofitLoginStep2(new NianticService.LoginRequest(response.body(), username, password));
            }

            @Override
            public void onFailure(Call<NianticService.InitialResponse> call, Throwable t) {

            }
        };
        Call<NianticService.InitialResponse> call = nianticService.login();
        call.enqueue(initialCallback);
    }

    private void retrofitLoginStep2(NianticService.LoginRequest loginRequest){
        Callback<NianticService.LoginResponse> loginCallback = new Callback<NianticService.LoginResponse>() {
            @Override
            public void onResponse(Call<NianticService.LoginResponse> call, Response<NianticService.LoginResponse> response) {
                //TODO: make the next call to finish getting the token.
            }

            @Override
            public void onFailure(Call<NianticService.LoginResponse> call, Throwable t) {

            }
        };
        Call<NianticService.LoginResponse> call = nianticService.completeLogin(loginRequest.lt, loginRequest.execution, loginRequest._eventId, loginRequest.username, loginRequest.password);
        call.enqueue(loginCallback);
    }

    private void traditionalLogin(final String username, final String password){
        final OkHttpClient client = new OkHttpClient.Builder()
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String s, SSLSession sslSession) {
                        return true;
                    }
                })
                .addInterceptor(new LoggingInterceptor())
                .followRedirects(false)
                .followSslRedirects(false)
                .build();

        Request initialRequest = new Request.Builder()
                .addHeader("User-Agent", "Niantic App")
                .url(LOGIN_URL)
                .build();

        client.newCall(initialRequest).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "fuck :(", e);
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONObject data = new JSONObject(body);
                    Log.d(TAG, data.toString());

                    RequestBody formBody = new FormBody.Builder()
                            .add("lt", data.getString("lt"))
                            .add("execution", data.getString("execution"))
                            .add("_eventId", "submit")
                            .add("username", username)
                            .add("password", password)
                            .build();

                    Request interceptRedirect = new Request.Builder()
                            .addHeader("User-Agent", "Niantic App")
                            .url(LOGIN_URL)
                            .post(formBody)
                            .build();

                    client.newCall(interceptRedirect).enqueue(new okhttp3.Callback() {
                        @Override
                        public void onFailure(okhttp3.Call call, IOException e) {
                            Log.e(TAG, "fuck :(", e);
                        }

                        @Override
                        public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                            Log.d(TAG, String.valueOf(response.code())); // should be a 302 (redirect)
                            Log.d(TAG, response.headers().toString()); // should contain a "Location" header

                            String ticket = response.header("Location").split("ticket=")[1];

                            RequestBody loginForm = new FormBody.Builder()
                                    .add("client_id", CLIENT_ID)
                                    .add("redirect_uri", REDIRECT_URI)
                                    .add("client_secret", PTC_CLIENT_SECRET)
                                    .add("grant_type", "refresh_token")
                                    .add("code", ticket)
                                    .build();

                            Request loginRequest = new Request.Builder()
                                    .addHeader("User-Agent", "Niantic App")
                                    .url(LOGIN_OAUTH)
                                    .post(loginForm)
                                    .build();

                            client.newCall(loginRequest).enqueue(new okhttp3.Callback() {
                                @Override
                                public void onFailure(okhttp3.Call call, IOException e) {

                                }

                                @Override
                                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                                    String rawToken = response.body().string();
                                    String cleanToken = rawToken.replaceAll("&expires.*", "").replaceAll(".*access_token=", "");

                                    Log.d(TAG, cleanToken); // success!

                                    //token = cleanToken;
                                }
                            });
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }



    public interface Listener{

    }

}
