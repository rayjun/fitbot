package com.fitness;

import androidx.hilt.work.HiltWorkerFactory;
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
public final class FitBotApp_MembersInjector implements MembersInjector<FitBotApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public FitBotApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<FitBotApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new FitBotApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(FitBotApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.fitness.FitBotApp.workerFactory")
  public static void injectWorkerFactory(FitBotApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
