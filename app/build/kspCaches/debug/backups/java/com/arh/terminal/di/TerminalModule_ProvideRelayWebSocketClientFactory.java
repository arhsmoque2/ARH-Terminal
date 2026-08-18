package com.arh.terminal.di;

import com.arh.terminal.core.relay.client.RelayWebSocketClient;
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
public final class TerminalModule_ProvideRelayWebSocketClientFactory implements Factory<RelayWebSocketClient> {
  private final Provider<CoroutineScope> scopeProvider;

  private TerminalModule_ProvideRelayWebSocketClientFactory(
      Provider<CoroutineScope> scopeProvider) {
    this.scopeProvider = scopeProvider;
  }

  @Override
  public RelayWebSocketClient get() {
    return provideRelayWebSocketClient(scopeProvider.get());
  }

  public static TerminalModule_ProvideRelayWebSocketClientFactory create(
      Provider<CoroutineScope> scopeProvider) {
    return new TerminalModule_ProvideRelayWebSocketClientFactory(scopeProvider);
  }

  public static RelayWebSocketClient provideRelayWebSocketClient(CoroutineScope scope) {
    return Preconditions.checkNotNullFromProvides(TerminalModule.INSTANCE.provideRelayWebSocketClient(scope));
  }
}
