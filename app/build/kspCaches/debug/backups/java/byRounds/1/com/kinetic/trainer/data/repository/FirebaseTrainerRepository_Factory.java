package com.kinetic.trainer.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.kinetic.trainer.data.SessionManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class FirebaseTrainerRepository_Factory implements Factory<FirebaseTrainerRepository> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  public FirebaseTrainerRepository_Factory(Provider<FirebaseFirestore> firestoreProvider,
      Provider<SessionManager> sessionManagerProvider) {
    this.firestoreProvider = firestoreProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public FirebaseTrainerRepository get() {
    return newInstance(firestoreProvider.get(), sessionManagerProvider.get());
  }

  public static FirebaseTrainerRepository_Factory create(
      Provider<FirebaseFirestore> firestoreProvider,
      Provider<SessionManager> sessionManagerProvider) {
    return new FirebaseTrainerRepository_Factory(firestoreProvider, sessionManagerProvider);
  }

  public static FirebaseTrainerRepository newInstance(FirebaseFirestore firestore,
      SessionManager sessionManager) {
    return new FirebaseTrainerRepository(firestore, sessionManager);
  }
}
