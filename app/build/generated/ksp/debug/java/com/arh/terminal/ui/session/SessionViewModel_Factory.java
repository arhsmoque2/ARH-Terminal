package com.arh.terminal.ui.session;

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

  private SessionViewModel_Factory(Provider<TmuxClientFactory> tmuxFactoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<NetworkMonitor> networkMonitorProvider) {
    this.tmuxFactoryProvider = tmuxFactoryProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.networkMonitorProvider = networkMonitorProvider;
  }

  @Override
  public SessionViewModel get() {
    return newInstance(tmuxFactoryProvider.get(), profileRepositoryProvider.get(), networkMonitorProvider.get());
  }

  public static SessionViewModel_Factory create(Provider<TmuxClientFactory> tmuxFactoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<NetworkMonitor> networkMonitorProvider) {
    return new SessionViewModel_Factory(tmuxFactoryProvider, profileRepositoryProvider, networkMonitorProvider);
  }

  public static SessionViewModel newInstance(TmuxClientFactory tmuxFactory,
      ProfileRepository profileRepository, NetworkMonitor networkMonitor) {
    return new SessionViewModel(tmuxFactory, profileRepository, networkMonitor);
  }
}
