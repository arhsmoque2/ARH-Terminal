package com.arh.terminal.di;

import com.arh.terminal.core.mcp.bridge.AccessibilityBridge;
import com.arh.terminal.core.mcp.tools.AndroidToolRegistry;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class TerminalModule_ProvideAndroidToolRegistryFactory implements Factory<AndroidToolRegistry> {
  private final Provider<AccessibilityBridge> bridgeProvider;

  private TerminalModule_ProvideAndroidToolRegistryFactory(
      Provider<AccessibilityBridge> bridgeProvider) {
    this.bridgeProvider = bridgeProvider;
  }

  @Override
  public AndroidToolRegistry get() {
    return provideAndroidToolRegistry(bridgeProvider.get());
  }

  public static TerminalModule_ProvideAndroidToolRegistryFactory create(
      Provider<AccessibilityBridge> bridgeProvider) {
    return new TerminalModule_ProvideAndroidToolRegistryFactory(bridgeProvider);
  }

  public static AndroidToolRegistry provideAndroidToolRegistry(AccessibilityBridge bridge) {
    return Preconditions.checkNotNullFromProvides(TerminalModule.INSTANCE.provideAndroidToolRegistry(bridge));
  }
}
