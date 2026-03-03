package com.fitness.di;

import com.fitness.data.local.AppDatabase;
import com.fitness.data.local.PlanDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvidePlanDaoFactory implements Factory<PlanDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_ProvidePlanDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public PlanDao get() {
    return providePlanDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvidePlanDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_ProvidePlanDaoFactory(databaseProvider);
  }

  public static PlanDao providePlanDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePlanDao(database));
  }
}
