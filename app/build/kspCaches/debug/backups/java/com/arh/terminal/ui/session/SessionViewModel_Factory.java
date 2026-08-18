package com.arh.terminal.ui.session;

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

  private SessionViewModel_Factory(Provider<TmuxClientFactory> tmuxFactoryProvider) {
    this.tmuxFactoryProvider = tmuxFactoryProvider;
  }

  @Override
  public SessionViewModel get() {
    return newInstance(tmuxFactoryProvider.get());
  }

  public static SessionViewModel_Factory create(Provider<TmuxClientFactory> tmuxFactoryProvider) {
    return new SessionViewModel_Factory(tmuxFactoryProvider);
  }

  public static SessionViewModel newInstance(TmuxClientFactory tmuxFactory) {
    return new SessionViewModel(tmuxFactory);
  }
}
