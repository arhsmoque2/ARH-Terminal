package com.arh.terminal.di

import com.arh.terminal.core.mcp.bridge.AccessibilityBridge
import com.arh.terminal.core.mcp.bridge.DefaultAccessibilityBridge
import com.arh.terminal.core.mcp.server.McpServerEngine
import com.arh.terminal.core.mcp.tools.AndroidToolRegistry
import com.arh.terminal.core.relay.client.RelayWebSocketClient
import com.pocketshell.core.tmux.TmuxClientFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object TerminalModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideTmuxClientFactory(
        @ApplicationScope scope: CoroutineScope
    ): TmuxClientFactory = TmuxClientFactory(scope)

    @Provides
    @Singleton
    fun provideAccessibilityBridge(): AccessibilityBridge = DefaultAccessibilityBridge()

    @Provides
    @Singleton
    fun provideAndroidToolRegistry(bridge: AccessibilityBridge): AndroidToolRegistry = AndroidToolRegistry(bridge)

    @Provides
    @Singleton
    fun provideMcpServerEngine(
        registry: AndroidToolRegistry,
        @ApplicationScope scope: CoroutineScope
    ): McpServerEngine = McpServerEngine(registry, scope)

    @Provides
    @Singleton
    fun provideRelayWebSocketClient(
        @ApplicationScope scope: CoroutineScope
    ): RelayWebSocketClient = RelayWebSocketClient(scope)
}
