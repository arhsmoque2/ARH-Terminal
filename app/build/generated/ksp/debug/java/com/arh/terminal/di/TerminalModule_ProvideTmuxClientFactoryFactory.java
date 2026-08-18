package com.arh.terminal.di;

import com.pocketshell.core.tmux.TmuxClientFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.arh.terminal.di.ApplicationScope")
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
public final class TerminalModule_ProvideTmuxClientFactoryFactory implements Factory<TmuxClientFactory> {
  private final Provider<CoroutineScope> scopeProvider;

  private TerminalModule_ProvideTmuxClientFactoryFactory(Provider<CoroutineScope> scopeProvider) {
    this.scopeProvider = scopeProvider;
  }

  @Override
  public TmuxClientFactory get() {
    return provideTmuxClientFactory(scopeProvider.get());
  }

  public static TerminalModule_ProvideTmuxClientFactoryFactory create(
      Provider<CoroutineScope> scopeProvider) {
    return new TerminalModule_ProvideTmuxClientFactoryFactory(scopeProvider);
  }

  public static TmuxClientFactory provideTmuxClientFactory(CoroutineScope scope) {
    return Preconditions.checkNotNullFromProvides(TerminalModule.INSTANCE.provideTmuxClientFactory(scope));
  }
}
