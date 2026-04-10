package com.kinetic.trainer.ui.viewmodels;

import com.kinetic.trainer.data.SessionManager;
import com.kinetic.trainer.data.repository.TrainerRepository;
import com.kinetic.trainer.domain.TrainerInsightEngine;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<TrainerRepository> trainerRepositoryProvider;

  private final Provider<TrainerInsightEngine> insightEngineProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  public HomeViewModel_Factory(Provider<TrainerRepository> trainerRepositoryProvider,
      Provider<TrainerInsightEngine> insightEngineProvider,
      Provider<SessionManager> sessionManagerProvider) {
    this.trainerRepositoryProvider = trainerRepositoryProvider;
    this.insightEngineProvider = insightEngineProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(trainerRepositoryProvider.get(), insightEngineProvider.get(), sessionManagerProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<TrainerRepository> trainerRepositoryProvider,
      Provider<TrainerInsightEngine> insightEngineProvider,
      Provider<SessionManager> sessionManagerProvider) {
    return new HomeViewModel_Factory(trainerRepositoryProvider, insightEngineProvider, sessionManagerProvider);
  }

  public static HomeViewModel newInstance(TrainerRepository trainerRepository,
      TrainerInsightEngine insightEngine, SessionManager sessionManager) {
    return new HomeViewModel(trainerRepository, insightEngine, sessionManager);
  }
}
