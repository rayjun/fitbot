package com.fitness.ui.workout;

import android.content.Context;
import com.fitness.data.local.ExerciseDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class WorkoutViewModel_Factory implements Factory<WorkoutViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<ExerciseDao> daoProvider;

  public WorkoutViewModel_Factory(Provider<Context> contextProvider,
      Provider<ExerciseDao> daoProvider) {
    this.contextProvider = contextProvider;
    this.daoProvider = daoProvider;
  }

  @Override
  public WorkoutViewModel get() {
    return newInstance(contextProvider.get(), daoProvider.get());
  }

  public static WorkoutViewModel_Factory create(Provider<Context> contextProvider,
      Provider<ExerciseDao> daoProvider) {
    return new WorkoutViewModel_Factory(contextProvider, daoProvider);
  }

  public static WorkoutViewModel newInstance(Context context, ExerciseDao dao) {
    return new WorkoutViewModel(context, dao);
  }
}
