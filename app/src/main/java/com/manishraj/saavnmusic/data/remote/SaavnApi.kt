package com.manishraj.saavnmusic.data.remote
import com.manishraj.saavnmusic.data.remote.dto.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit contract for sumitkolhe/jiosaavn-api (public instance https://saavn.dev/api). No auth headers - the API is unauthenticated. */
interface SaavnApi {
    @GET("search")
    suspend fun searchAll(
        @Query("query") q: String,
    ): ApiResponse<GlobalSearchDto>

    @GET("search/songs")
    suspend fun searchSongs(
        @Query("query") q: String,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<SearchResultDto<SongDto>>

    @GET("search/albums")
    suspend fun searchAlbums(
        @Query("query") q: String,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<SearchResultDto<AlbumDto>>

    @GET("search/artists")
    suspend fun searchArtists(
        @Query("query") q: String,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<SearchResultDto<ArtistDetailDto>>

    @GET("search/playlists")
    suspend fun searchPlaylists(
        @Query("query") q: String,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<SearchResultDto<PlaylistDto>>

    @GET("songs/{id}")
    suspend fun songById(
        @Path("id") id: String,
        @Query("lyrics") lyrics: Boolean = false,
    ): ApiResponse<List<SongDto>>

    @GET("songs/{id}/suggestions")
    suspend fun suggestions(
        @Path("id") id: String,
        @Query("limit") limit: Int = 10,
    ): ApiResponse<List<SongDto>>

    @GET("albums")
    suspend fun albumById(
        @Query("id") id: String,
    ): ApiResponse<AlbumDto>

    @GET("playlists")
    suspend fun playlistById(
        @Query("id") id: String,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 100,
    ): ApiResponse<PlaylistDto>

    @GET("artists/{id}")
    suspend fun artistById(
        @Path("id") id: String,
    ): ApiResponse<ArtistDetailDto>

    @GET("artists/{id}/songs")
    suspend fun artistSongs(
        @Path("id") id: String,
        @Query("page") page: Int = 0,
    ): ApiResponse<SearchResultDto<SongDto>>

    @GET("artists/{id}/albums")
    suspend fun artistAlbums(
        @Path("id") id: String,
        @Query("page") page: Int = 0,
    ): ApiResponse<SearchResultDto<AlbumDto>>
}
