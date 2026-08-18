package com.arh.terminal.di;

import com.arh.terminal.core.mcp.bridge.AccessibilityBridge;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class TerminalModule_ProvideAccessibilityBridgeFactory implements Factory<AccessibilityBridge> {
  @Override
  public AccessibilityBridge get() {
    return provideAccessibilityBridge();
  }

  public static TerminalModule_ProvideAccessibilityBridgeFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AccessibilityBridge provideAccessibilityBridge() {
    return Preconditions.checkNotNullFromProvides(TerminalModule.INSTANCE.provideAccessibilityBridge());
  }

  private static final class InstanceHolder {
    static final TerminalModule_ProvideAccessibilityBridgeFactory INSTANCE = new TerminalModule_ProvideAccessibilityBridgeFactory();
  }
}
