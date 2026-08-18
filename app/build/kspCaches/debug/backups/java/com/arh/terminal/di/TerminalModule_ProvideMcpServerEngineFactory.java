package com.arh.terminal.di;

import com.arh.terminal.core.mcp.server.McpServerEngine;
import com.arh.terminal.core.mcp.tools.AndroidToolRegistry;
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
public final class TerminalModule_ProvideMcpServerEngineFactory implements Factory<McpServerEngine> {
  private final Provider<AndroidToolRegistry> registryProvider;

  private final Provider<CoroutineScope> scopeProvider;

  private TerminalModule_ProvideMcpServerEngineFactory(
      Provider<AndroidToolRegistry> registryProvider, Provider<CoroutineScope> scopeProvider) {
    this.registryProvider = registryProvider;
    this.scopeProvider = scopeProvider;
  }

  @Override
  public McpServerEngine get() {
    return provideMcpServerEngine(registryProvider.get(), scopeProvider.get());
  }

  public static TerminalModule_ProvideMcpServerEngineFactory create(
      Provider<AndroidToolRegistry> registryProvider, Provider<CoroutineScope> scopeProvider) {
    return new TerminalModule_ProvideMcpServerEngineFactory(registryProvider, scopeProvider);
  }

  public static McpServerEngine provideMcpServerEngine(AndroidToolRegistry registry,
      CoroutineScope scope) {
    return Preconditions.checkNotNullFromProvides(TerminalModule.INSTANCE.provideMcpServerEngine(registry, scope));
  }
}
