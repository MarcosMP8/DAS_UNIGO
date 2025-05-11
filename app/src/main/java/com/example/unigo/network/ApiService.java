package com.example.unigo.network;

import com.example.unigo.network.LoginResponse;
import com.example.unigo.network.RegisterResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiService {

    @FormUrlEncoded
    @POST("login_unigo.php")
    Call<LoginResponse> loginUser(
            @Field("nombre")   String nombre,
            @Field("password") String password
    );

    @FormUrlEncoded
    @POST("register_unigo.php")
    Call<RegisterResponse> registerUser(
            @Field("nombre")   String nombre,
            @Field("email")    String email,
            @Field("password") String password,
            @Field("telefono") String telefono
    );

    @FormUrlEncoded
    @POST("update_profile_unigo.php")
    Call<GenericResponse> updateProfile(
            @Field("id")       int    userId,
            @Field("nombre")   String nombre,
            @Field("email")    String email,
            @Field("telefono") String telefono
    );

}
