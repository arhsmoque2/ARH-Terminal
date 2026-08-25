package com.arh.terminal.data.github

import androidx.compose.runtime.Immutable

@Immutable
data class GitHubDeviceAuth(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Int,
    val interval: Int
)

@Immutable
data class GitHubRepo(
    val id: Long,
    val name: String,
    val fullName: String,
    val description: String?,
    val isPrivate: Boolean,
    val defaultBranch: String,
    val htmlUrl: String,
    val cloneUrl: String,
    val sshUrl: String,
    val starsCount: Int = 0,
    val openIssuesCount: Int = 0
)

@Immutable
data class GitHubBranch(
    val name: String,
    val commitSha: String
)

@Immutable
data class GitHubIssue(
    val number: Int,
    val title: String,
    val body: String?,
    val state: String,
    val htmlUrl: String,
    val author: String
)

@Immutable
data class GitHubUser(
    val login: String,
    val id: Long,
    val avatarUrl: String?,
    val name: String?,
    val email: String?
)
