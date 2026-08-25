package com.arh.terminal.data.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubClient @Inject constructor(
    private val authManager: GitHubAuthManager
) {
    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000
    }

    suspend fun listUserRepositories(): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val token = authManager.getStoredToken() ?: return@withContext Result.failure(Exception("Not authenticated with GitHub"))
            val url = URL("https://api.github.com/user/repos?sort=updated&per_page=50")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "ARH-Terminal")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }

            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(text)
                val repos = mutableListOf<GitHubRepo>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val fullName = obj.getString("full_name")
                    repos.add(
                        GitHubRepo(
                            id = obj.getLong("id"),
                            name = obj.getString("name"),
                            fullName = fullName,
                            description = if (obj.isNull("description")) null else obj.getString("description"),
                            isPrivate = obj.getBoolean("private"),
                            defaultBranch = obj.optString("default_branch", "main"),
                            htmlUrl = obj.getString("html_url"),
                            cloneUrl = obj.getString("clone_url"),
                            sshUrl = obj.optString("ssh_url", "git@github.com:$fullName.git"),
                            starsCount = obj.optInt("stargazers_count", 0),
                            openIssuesCount = obj.optInt("open_issues_count", 0)
                        )
                    )
                }
                Result.success(repos)
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP ${conn.responseCode}"
                Result.failure(Exception("Failed to fetch repositories: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun listBranches(owner: String, repo: String): Result<List<GitHubBranch>> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val token = authManager.getStoredToken() ?: return@withContext Result.failure(Exception("Not authenticated"))
            val url = URL("https://api.github.com/repos/$owner/$repo/branches")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "ARH-Terminal")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }

            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(text)
                val branches = mutableListOf<GitHubBranch>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    branches.add(
                        GitHubBranch(
                            name = obj.getString("name"),
                            commitSha = obj.getJSONObject("commit").getString("sha")
                        )
                    )
                }
                Result.success(branches)
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP ${conn.responseCode}"
                Result.failure(Exception("Failed to fetch branches: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun listIssues(owner: String, repo: String): Result<List<GitHubIssue>> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val token = authManager.getStoredToken() ?: return@withContext Result.failure(Exception("Not authenticated"))
            val url = URL("https://api.github.com/repos/$owner/$repo/issues?state=open&per_page=30")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "ARH-Terminal")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }

            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(text)
                val issues = mutableListOf<GitHubIssue>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (!obj.has("pull_request")) {
                        issues.add(
                            GitHubIssue(
                                number = obj.getInt("number"),
                                title = obj.getString("title"),
                                body = if (obj.isNull("body")) null else obj.getString("body"),
                                state = obj.getString("state"),
                                htmlUrl = obj.getString("html_url"),
                                author = obj.getJSONObject("user").getString("login")
                            )
                        )
                    }
                }
                Result.success(issues)
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP ${conn.responseCode}"
                Result.failure(Exception("Failed to fetch issues: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun getUserProfile(): Result<GitHubUser> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val token = authManager.getStoredToken() ?: return@withContext Result.failure(Exception("Not authenticated"))
            val url = URL("https://api.github.com/user")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "ARH-Terminal")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }

            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(text)
                Result.success(
                    GitHubUser(
                        login = obj.getString("login"),
                        id = obj.getLong("id"),
                        avatarUrl = if (obj.isNull("avatar_url")) null else obj.getString("avatar_url"),
                        name = if (obj.isNull("name")) null else obj.getString("name"),
                        email = if (obj.isNull("email")) null else obj.getString("email")
                    )
                )
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP ${conn.responseCode}"
                Result.failure(Exception("Failed to fetch user profile: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }
}
