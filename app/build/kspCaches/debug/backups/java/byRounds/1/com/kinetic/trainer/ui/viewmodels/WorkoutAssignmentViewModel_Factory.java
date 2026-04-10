package com.kinetic.trainer.ui.viewmodels;

import androidx.lifecycle.SavedStateHandle;
import com.kinetic.trainer.data.repository.TrainerRepository;
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
    "KotlinInternalInJava",
    "cast"
})
public final class WorkoutAssignmentViewModel_Factory implements Factory<WorkoutAssignmentViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<TrainerRepository> trainerRepositoryProvider;

  public WorkoutAssignmentViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<TrainerRepository> trainerRepositoryProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.trainerRepositoryProvider = trainerRepositoryProvider;
  }

  @Override
  public WorkoutAssignmentViewModel get() {
    return newInstance(savedStateHandleProvider.get(), trainerRepositoryProvider.get());
  }

  public static WorkoutAssignmentViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<TrainerRepository> trainerRepositoryProvider) {
    return new WorkoutAssignmentViewModel_Factory(savedStateHandleProvider, trainerRepositoryProvider);
  }

  public static WorkoutAssignmentViewModel newInstance(SavedStateHandle savedStateHandle,
      TrainerRepository trainerRepository) {
    return new WorkoutAssignmentViewModel(savedStateHandle, trainerRepository);
  }
}
