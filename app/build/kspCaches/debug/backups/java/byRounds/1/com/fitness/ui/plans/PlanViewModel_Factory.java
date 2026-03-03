package com.fitness.ui.plans;

import com.fitness.data.local.PlanDao;
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
public final class PlanViewModel_Factory implements Factory<PlanViewModel> {
  private final Provider<PlanDao> daoProvider;

  public PlanViewModel_Factory(Provider<PlanDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public PlanViewModel get() {
    return newInstance(daoProvider.get());
  }

  public static PlanViewModel_Factory create(Provider<PlanDao> daoProvider) {
    return new PlanViewModel_Factory(daoProvider);
  }

  public static PlanViewModel newInstance(PlanDao dao) {
    return new PlanViewModel(dao);
  }
}
