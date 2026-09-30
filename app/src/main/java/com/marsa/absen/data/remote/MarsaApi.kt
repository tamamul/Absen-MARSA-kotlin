package com.marsa.absen.data.remote

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.Part

interface MarsaApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: JsonObject): Response<JsonElement>

    @GET("api/auth/profile")
    suspend fun profile(): Response<JsonElement>

    @POST("api/auth/logout")
    suspend fun logout(): Response<JsonElement>

    @GET("api/pegawai/profil")
    suspend fun pegawaiProfil(): Response<JsonElement>

    @GET("api/absen/hari-ini")
    suspend fun absenHariIni(): Response<JsonElement>

    @POST("api/absen/cek-lokasi")
    suspend fun cekLokasi(@Body body: JsonObject): Response<JsonElement>

    @GET("api/absen/riwayat")
    suspend fun riwayat(@Query("limit") limit: Int): Response<JsonElement>

    @GET("api/absen/galeri")
    suspend fun galeri(@Query("tanggal") tanggal: String): Response<JsonElement>

    @GET("api/pengumuman")
    suspend fun pengumuman(
        @Query("filter") filter: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<JsonElement>

    @POST("api/pengumuman")
    suspend fun buatPengumuman(@Body body: JsonObject): Response<JsonElement>

    @DELETE("api/pengumuman/{id}")
    suspend fun hapusPengumuman(@Path("id") id: Int): Response<JsonElement>

    @GET("api/pengumuman/{id}/komentar")
    suspend fun komentar(@Path("id") id: Int): Response<JsonElement>

    @POST("api/pengumuman/{id}/komentar")
    suspend fun kirimKomentar(@Path("id") id: Int, @Body body: JsonObject): Response<JsonElement>

    @Multipart
    @POST("api/absen/masuk")
    suspend fun absenMasuk(
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part foto: MultipartBody.Part
    ): Response<JsonElement>

    @Multipart
    @POST("api/absen/keluar")
    suspend fun absenKeluar(
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part foto: MultipartBody.Part
    ): Response<JsonElement>    
}
