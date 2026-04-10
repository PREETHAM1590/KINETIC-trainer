package com.kinetic.trainer.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class FakeTrainerRepository_Factory implements Factory<FakeTrainerRepository> {
  @Override
  public FakeTrainerRepository get() {
    return newInstance();
  }

  public static FakeTrainerRepository_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FakeTrainerRepository newInstance() {
    return new FakeTrainerRepository();
  }

  private static final class InstanceHolder {
    private static final FakeTrainerRepository_Factory INSTANCE = new FakeTrainerRepository_Factory();
  }
}
