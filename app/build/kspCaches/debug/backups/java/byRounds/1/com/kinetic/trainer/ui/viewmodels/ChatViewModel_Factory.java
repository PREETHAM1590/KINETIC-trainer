package com.kinetic.trainer.ui.viewmodels;

import androidx.lifecycle.SavedStateHandle;
import com.kinetic.trainer.data.SessionManager;
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<TrainerRepository> trainerRepositoryProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  public ChatViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<TrainerRepository> trainerRepositoryProvider,
      Provider<SessionManager> sessionManagerProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.trainerRepositoryProvider = trainerRepositoryProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(savedStateHandleProvider.get(), trainerRepositoryProvider.get(), sessionManagerProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<TrainerRepository> trainerRepositoryProvider,
      Provider<SessionManager> sessionManagerProvider) {
    return new ChatViewModel_Factory(savedStateHandleProvider, trainerRepositoryProvider, sessionManagerProvider);
  }

  public static ChatViewModel newInstance(SavedStateHandle savedStateHandle,
      TrainerRepository trainerRepository, SessionManager sessionManager) {
    return new ChatViewModel(savedStateHandle, trainerRepository, sessionManager);
  }
}
