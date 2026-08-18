package com.arh.terminal.ui.session;

import com.arh.terminal.core.mcp.server.McpServerEngine;
import com.arh.terminal.core.relay.client.RelayWebSocketClient;
import com.arh.terminal.data.profiles.ProfileRepository;
import com.arh.terminal.util.NetworkMonitor;
import com.pocketshell.core.tmux.TmuxClientFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class SessionViewModel_Factory implements Factory<SessionViewModel> {
  private final Provider<TmuxClientFactory> tmuxFactoryProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  private final Provider<NetworkMonitor> networkMonitorProvider;

  private final Provider<McpServerEngine> mcpServerEngineProvider;

  private final Provider<RelayWebSocketClient> relayClientProvider;

  private SessionViewModel_Factory(Provider<TmuxClientFactory> tmuxFactoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<NetworkMonitor> networkMonitorProvider,
      Provider<McpServerEngine> mcpServerEngineProvider,
      Provider<RelayWebSocketClient> relayClientProvider) {
    this.tmuxFactoryProvider = tmuxFactoryProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.networkMonitorProvider = networkMonitorProvider;
    this.mcpServerEngineProvider = mcpServerEngineProvider;
    this.relayClientProvider = relayClientProvider;
  }

  @Override
  public SessionViewModel get() {
    return newInstance(tmuxFactoryProvider.get(), profileRepositoryProvider.get(), networkMonitorProvider.get(), mcpServerEngineProvider.get(), relayClientProvider.get());
  }

  public static SessionViewModel_Factory create(Provider<TmuxClientFactory> tmuxFactoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<NetworkMonitor> networkMonitorProvider,
      Provider<McpServerEngine> mcpServerEngineProvider,
      Provider<RelayWebSocketClient> relayClientProvider) {
    return new SessionViewModel_Factory(tmuxFactoryProvider, profileRepositoryProvider, networkMonitorProvider, mcpServerEngineProvider, relayClientProvider);
  }

  public static SessionViewModel newInstance(TmuxClientFactory tmuxFactory,
      ProfileRepository profileRepository, NetworkMonitor networkMonitor,
      McpServerEngine mcpServerEngine, RelayWebSocketClient relayClient) {
    return new SessionViewModel(tmuxFactory, profileRepository, networkMonitor, mcpServerEngine, relayClient);
  }
}
