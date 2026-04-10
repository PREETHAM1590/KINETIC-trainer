package com.kinetic.trainer.di;

import com.google.firebase.firestore.FirebaseFirestore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AppModule_Companion_ProvideFirestoreFactory implements Factory<FirebaseFirestore> {
  @Override
  public FirebaseFirestore get() {
    return provideFirestore();
  }

  public static AppModule_Companion_ProvideFirestoreFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FirebaseFirestore provideFirestore() {
    return Preconditions.checkNotNullFromProvides(AppModule.Companion.provideFirestore());
  }

  private static final class InstanceHolder {
    private static final AppModule_Companion_ProvideFirestoreFactory INSTANCE = new AppModule_Companion_ProvideFirestoreFactory();
  }
}
