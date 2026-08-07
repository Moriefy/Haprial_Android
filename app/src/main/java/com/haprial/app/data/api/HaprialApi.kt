package com.haprial.app.data.api

import com.haprial.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface HaprialApi {
    @POST("/api/admin/auth")
    suspend fun login(@Body body: Map<String, String>): Response<LoginResponse>

    @GET("/api/admin/verify")
    suspend fun verify(): Response<GenericResponse>

    @GET("/api/admin/articles")
    suspend fun getArticles(@Query("status") status: String = "all"): Response<ArticleListResponse>

    @GET("/api/admin/articles/{id}")
    suspend fun getArticle(@Path("id") id: Int): Response<ArticleDetailResponse>

    @POST("/api/admin/articles")
    suspend fun createArticle(@Body article: ArticleCreateRequest): Response<ArticleCreateResponse>

    @PUT("/api/admin/articles/{id}")
    suspend fun updateArticle(@Path("id") id: Int, @Body article: ArticleCreateRequest): Response<GenericResponse>

    @DELETE("/api/admin/articles/{id}")
    suspend fun deleteArticle(@Path("id") id: Int): Response<GenericResponse>

    @POST("/api/admin/articles/{id}/publish")
    suspend fun togglePublish(@Path("id") id: Int): Response<PublishResponse>

    @GET("/api/admin/comments")
    suspend fun getComments(
        @Query("page_slug") pageSlug: String? = null,
        @Query("limit") limit: Int = 200
    ): Response<CommentListResponse>

    @POST("/api/admin/comments")
    suspend fun postComment(@Body comment: CommentPostRequest): Response<GenericResponse>

    @DELETE("/api/admin/comments/{id}")
    suspend fun deleteComment(@Path("id") id: Int): Response<GenericResponse>

    @POST("/api/admin/comments/{id}/pin")
    suspend fun pinComment(@Path("id") id: Int): Response<PinResponse>

    @POST("/api/admin/comments/{id}/like")
    suspend fun likeComment(@Path("id") id: Int): Response<GenericResponse>

    @GET("/api/admin/images/list")
    suspend fun getImages(@Query("folder") folder: String? = null): Response<ImageListResponse>

    @POST("/api/admin/images/upload")
    suspend fun uploadImage(@Body body: Map<String, String>): Response<ImageUploadResponse>

    @POST("/api/admin/images/copy")
    suspend fun copyImage(@Body body: Map<String, String>): Response<GenericResponse>

    @DELETE("/api/admin/images/{path}")
    suspend fun deleteImage(@Path("path") path: String): Response<GenericResponse>

    @GET("/api/admin/friends")
    suspend fun getFriends(): Response<FriendListResponse>

    @POST("/api/admin/friends")
    suspend fun createFriend(@Body body: Map<String, String>): Response<GenericResponse>

    @PUT("/api/admin/friends/{id}")
    suspend fun updateFriend(@Path("id") id: Int, @Body body: Map<String, String>): Response<GenericResponse>

    @DELETE("/api/admin/friends/{id}")
    suspend fun deleteFriend(@Path("id") id: Int): Response<GenericResponse>

    @GET("/api/admin/stats")
    suspend fun getStats(): Response<StatsResponse>

    @GET("/api/admin/trash")
    suspend fun getTrash(): Response<TrashListResponse>

    @POST("/api/admin/trash/{id}/restore")
    suspend fun restoreTrash(@Path("id") id: Int): Response<GenericResponse>

    @DELETE("/api/admin/trash/{id}")
    suspend fun deleteTrash(@Path("id") id: Int): Response<GenericResponse>

    @POST("/api/admin/trash/empty")
    suspend fun emptyTrash(): Response<GenericResponse>
}
