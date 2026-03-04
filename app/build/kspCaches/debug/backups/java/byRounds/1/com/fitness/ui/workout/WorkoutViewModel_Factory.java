package com.fitness.ui.workout;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
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

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public WorkoutViewModel_Factory(Provider<Context> contextProvider,
      Provider<ExerciseDao> daoProvider, Provider<SavedStateHandle> savedStateHandleProvider) {
    this.contextProvider = contextProvider;
    this.daoProvider = daoProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public WorkoutViewModel get() {
    return newInstance(contextProvider.get(), daoProvider.get(), savedStateHandleProvider.get());
  }

  public static WorkoutViewModel_Factory create(Provider<Context> contextProvider,
      Provider<ExerciseDao> daoProvider, Provider<SavedStateHandle> savedStateHandleProvider) {
    return new WorkoutViewModel_Factory(contextProvider, daoProvider, savedStateHandleProvider);
  }

  public static WorkoutViewModel newInstance(Context context, ExerciseDao dao,
      SavedStateHandle savedStateHandle) {
    return new WorkoutViewModel(context, dao, savedStateHandle);
  }
}
