package com.fitness.ui.profile;

import com.fitness.data.local.ExerciseDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
    "KotlinInternalInJava"
})
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<ExerciseDao> daoProvider;

  public ProfileViewModel_Factory(Provider<ExerciseDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(daoProvider.get());
  }

  public static ProfileViewModel_Factory create(Provider<ExerciseDao> daoProvider) {
    return new ProfileViewModel_Factory(daoProvider);
  }

  public static ProfileViewModel newInstance(ExerciseDao dao) {
    return new ProfileViewModel(dao);
  }
}
