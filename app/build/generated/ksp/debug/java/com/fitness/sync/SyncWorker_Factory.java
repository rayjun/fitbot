package com.fitness.sync;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.fitness.data.local.ExerciseDao;
import com.fitness.data.local.PlanDao;
import dagger.internal.DaggerGenerated;
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
public final class SyncWorker_Factory {
  private final Provider<ExerciseDao> exerciseDaoProvider;

  private final Provider<PlanDao> planDaoProvider;

  public SyncWorker_Factory(Provider<ExerciseDao> exerciseDaoProvider,
      Provider<PlanDao> planDaoProvider) {
    this.exerciseDaoProvider = exerciseDaoProvider;
    this.planDaoProvider = planDaoProvider;
  }

  public SyncWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams, exerciseDaoProvider.get(), planDaoProvider.get());
  }

  public static SyncWorker_Factory create(Provider<ExerciseDao> exerciseDaoProvider,
      Provider<PlanDao> planDaoProvider) {
    return new SyncWorker_Factory(exerciseDaoProvider, planDaoProvider);
  }

  public static SyncWorker newInstance(Context appContext, WorkerParameters workerParams,
      ExerciseDao exerciseDao, PlanDao planDao) {
    return new SyncWorker(appContext, workerParams, exerciseDao, planDao);
  }
}
