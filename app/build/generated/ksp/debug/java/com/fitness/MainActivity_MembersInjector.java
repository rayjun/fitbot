package com.fitness;

import com.fitness.sync.AuthManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
    "KotlinInternalInJava"
})
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<AuthManager> authManagerProvider;

  public MainActivity_MembersInjector(Provider<AuthManager> authManagerProvider) {
    this.authManagerProvider = authManagerProvider;
  }

  public static MembersInjector<MainActivity> create(Provider<AuthManager> authManagerProvider) {
    return new MainActivity_MembersInjector(authManagerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectAuthManager(instance, authManagerProvider.get());
  }

  @InjectedFieldSignature("com.fitness.MainActivity.authManager")
  public static void injectAuthManager(MainActivity instance, AuthManager authManager) {
    instance.authManager = authManager;
  }
}
