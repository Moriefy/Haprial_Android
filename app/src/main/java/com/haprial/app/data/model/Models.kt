package com.haprial.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

private val gson by lazy { Gson() }

data class LoginResponse(val ok: Boolean, val token: String? = null, val message: String? = null, val error: String? = null)
data class GenericResponse(val ok: Boolean, val error: String? = null, val github: GithubResult? = null)
data class GithubResult(val ok: Boolean, val error: String? = null)
data class ArticleListResponse(val articles: List<Article>)
data class ArticleDetailResponse(val article: Article)
data class ArticleCreateResponse(val ok: Boolean, val id: Int, val slug: String, val github: GithubResult? = null)
data class PublishResponse(val ok: Boolean, val status: String, val github: GithubResult? = null)
data class CommentListResponse(val comments: List<Comment>, val total: Int, val pages: List<String>)
data class PinResponse(val ok: Boolean, val pinned: Int)
data class ImageListResponse(val images: List<ImageItem>, val folders: List<String>)
data class ImageUploadResponse(val ok: Boolean, val url: String, val path: String)
data class FriendListResponse(val friends: List<Friend>)
data class StatsResponse(val articles: Int, val published: Int, val drafts: Int, val comments: Int, val friends: Int, val trash: Int)

data class Article(
    val id: Int, val slug: String, val title: String, val date: String,
    val tags: String, val category: String, val excerpt: String,
    val content: String? = null, val status: String, val pinned: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    fun tagList(): List<String> = try {
        if (tags.startsWith("[")) gson.fromJson(tags, Array<String>::class.java).toList()
        else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    } catch (e: Exception) { emptyList() }
}

data class Comment(
    val id: Int, @SerializedName("parent_id") val parentId: Int, val depth: Int,
    val nickname: String, val email: String? = null, val website: String? = null,
    @SerializedName("avatar_hash") val avatarHash: String? = null,
    @SerializedName("content_html") val contentHtml: String,
    val liked: Int = 0, @SerializedName("is_admin") val isAdmin: Int = 0,
    val pinned: Int = 0, @SerializedName("created_at") val createdAt: String
)

data class ImageItem(val name: String, val url: String, val size: Long, val sha: String)
data class Friend(val id: Int, val name: String, val url: String, val avatar: String? = null, val desc: String? = null, val sort: Int = 0)

data class ArticleCreateRequest(
    val title: String, val date: String, val tags: List<String>,
    val category: String, val excerpt: String, val content: String,
    val status: String, val pinned: Boolean = false
)

data class CommentPostRequest(
    val page: String, @SerializedName("parent_id") val parentId: Int = 0,
    val depth: Int = 0, val nickname: String, val email: String = "",
    val website: String = "", val content: String
)

data class TrashItem(
    val id: Int, val title: String, val slug: String, val type: String = "article",
    @SerializedName("deleted_at") val deletedAt: String
)
data class TrashListResponse(@SerializedName("trash") val items: List<TrashItem>)
