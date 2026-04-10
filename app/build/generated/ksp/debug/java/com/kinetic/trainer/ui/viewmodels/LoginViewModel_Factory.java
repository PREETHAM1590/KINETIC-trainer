package com.kinetic.trainer.ui.viewmodels;

import com.kinetic.trainer.data.SessionManager;
import com.kinetic.trainer.data.repository.AuthRepository;
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
public final class LoginViewModel_Factory implements Factory<LoginViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  public LoginViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<SessionManager> sessionManagerProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public LoginViewModel get() {
    return newInstance(authRepositoryProvider.get(), sessionManagerProvider.get());
  }

  public static LoginViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<SessionManager> sessionManagerProvider) {
    return new LoginViewModel_Factory(authRepositoryProvider, sessionManagerProvider);
  }

  public static LoginViewModel newInstance(AuthRepository authRepository,
      SessionManager sessionManager) {
    return new LoginViewModel(authRepository, sessionManager);
  }
}
